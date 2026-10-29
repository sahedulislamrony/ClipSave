package com.sahedul.clipsave.core

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.sahedul.clipsave.BuildConfig
import com.sahedul.clipsave.ClipSaveApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    /** Couldn't reach GitHub; the app stays usable. */
    data class CheckFailed(val message: String) : UpdateState

    /** Everything below means a newer release exists, and the update screen is shown until it's installed. */
    sealed interface Pending : UpdateState { val release: Release }
    data class Available(override val release: Release, val error: String? = null) : Pending
    data class Downloading(override val release: Release, val progress: Float) : Pending
    data class Installing(override val release: Release) : Pending
}

data class Release(val version: String, val asset: GithubRelease.Asset)

/**
 * Updates the app from GitHub Releases. Each release ships four APKs (three ABI splits plus a
 * universal one); we download the same kind that's installed, so a phone on the 60 MB arm64 build
 * never pulls the 200 MB universal one. Android only accepts it if it's signed with the same key.
 */
class AppUpdater(private val app: Application, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state
    private val busy = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val dir = File(app.cacheDir, "update")

    /** Which of the release's APKs this install came from. */
    val installedVariant: String by lazy {
        runCatching {
            ZipFile(app.applicationInfo.sourceDir).use { zip ->
                AppRelease.variantFor(AppRelease.abisIn(zip.entries().asSequence().map { it.name }))
            }
        }.getOrDefault(AppRelease.UNIVERSAL)
    }

    /** Looks for a newer release. Ignored while a check, download or install is already underway. */
    fun check() {
        if (_state.value is UpdateState.Downloading || _state.value is UpdateState.Installing) return
        if (!busy.tryLock()) return
        val before = _state.value
        if (before !is UpdateState.Pending) _state.value = UpdateState.Checking
        scope.launch(Dispatchers.IO) {
            try {
                val latest = fetchLatest()
                val asset = AppRelease.assetFor(latest, installedVariant)
                _state.value = when {
                    !AppRelease.isNewer(latest.version, BuildConfig.VERSION_NAME) -> UpdateState.UpToDate
                    asset == null -> UpdateState.CheckFailed("No $installedVariant APK in ${latest.tag}")
                    // Keep any error message from a cancelled install of the same version.
                    before is UpdateState.Available && before.release.version == latest.version -> before
                    else -> UpdateState.Available(Release(latest.version, asset))
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Update check failed", e)
                // A pending update stays pending: being offline must not let the user skip it.
                if (before !is UpdateState.Pending) _state.value = UpdateState.CheckFailed(e.message ?: "Network error")
            } finally {
                busy.unlock()
            }
        }
    }

    /** Downloads the pending release's APK (reusing a finished download) and hands it to the system installer. */
    fun install() {
        val pending = _state.value as? UpdateState.Available ?: return
        if (!busy.tryLock()) return
        val release = pending.release
        _state.value = UpdateState.Downloading(release, 0f)
        scope.launch(Dispatchers.IO) {
            try {
                val apk = download(release)
                _state.value = UpdateState.Installing(release)
                commit(apk)
            } catch (e: Throwable) {
                Log.w(TAG, "Update failed", e)
                _state.value = UpdateState.Available(release, e.message ?: "Download failed")
            } finally {
                busy.unlock()
            }
        }
    }

    /** Called by [InstallResultReceiver] when the installer gives up or the user cancels. */
    internal fun onInstallFailed(message: String) {
        val s = _state.value as? UpdateState.Pending ?: return
        _state.value = UpdateState.Available(s.release, message)
    }

    private fun fetchLatest(): GithubRelease {
        val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "ClipSave/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode != 200) error("GitHub returned ${conn.responseCode}")
            return json.decodeFromString(GithubRelease.serializer(), conn.inputStream.bufferedReader().readText())
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun download(release: Release): File = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val asset = release.asset
        val out = File(dir, asset.name)
        if (out.length() == asset.size) return@withContext out
        // Drop APKs from older releases or interrupted downloads.
        dir.listFiles()?.forEach { it.delete() }

        val part = File(dir, "${asset.name}.part")
        val conn = (URL(asset.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ClipSave/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode != 200) error("Download failed (${conn.responseCode})")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: asset.size
            conn.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPct = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        val pct = (done * 100 / total).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            _state.value = UpdateState.Downloading(release, (done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        if (part.length() != asset.size) {
            part.delete()
            error("Download incomplete")
        }
        part.renameTo(out)
        out
    }

    private fun commit(apk: File) {
        val installer = app.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.packageName)
            setSize(apk.length())
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            // The installer fills in the result extras, so the PendingIntent must be mutable.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val callback = PendingIntent.getBroadcast(app, id, Intent(app, InstallResultReceiver::class.java), flags)
            session.commit(callback.intentSender)
        }
    }

    companion object {
        private const val TAG = "AppUpdater"
        private const val LATEST_URL = "https://api.github.com/repos/sahedulislamrony/ClipSave/releases/latest"
    }
}

/** Receives the system installer's verdict: shows its confirmation screen, or reports a failure. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val updater = (context.applicationContext as ClipSaveApp).container.updater
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm == null) {
                    updater.onInstallFailed("Installer didn't respond")
                } else {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            // On success Android replaces and restarts the app, so there's nothing to do.
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED -> updater.onInstallFailed("Update cancelled")
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w("AppUpdater", "Install failed: $status $msg")
                updater.onInstallFailed(
                    if (status == PackageInstaller.STATUS_FAILURE_CONFLICT) "This build can't be updated in place (different signature)"
                    else msg ?: "Install failed",
                )
            }
        }
    }
}
