package com.sahed.clipsave.ui

import android.content.ActivityNotFoundException
import com.sahed.clipsave.download.MediaStoreSaver
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahed.clipsave.AppContainer
import com.sahed.clipsave.BuildConfig
import com.sahed.clipsave.core.CookieStore
import com.sahed.clipsave.core.EngineState
import com.sahed.clipsave.core.Prefs
import com.sahed.clipsave.ui.theme.ClipType
import com.sahed.clipsave.ui.theme.GlassButton
import com.sahed.clipsave.ui.theme.LocalClipColors
import com.sahed.clipsave.ui.theme.Radius
import com.sahed.clipsave.ui.theme.panel
import kotlinx.coroutines.launch

private val QUALITY_CHOICES = listOf(
    "ask" to "Ask",
    "best" to "Best",
    "1080" to "1080p",
    "720" to "720p",
    "480" to "480p",
    "mp3" to "MP3",
)

private val THEME_LABELS = mapOf(Prefs.THEME_LIGHT to "Light", Prefs.THEME_DARK to "Dark", Prefs.THEME_SYSTEM to "System")

private enum class UpdateUi { Idle, Checking, UpToDate, Failed }

@Composable
fun SettingsScreen(container: AppContainer, insets: PaddingValues, onOpenPage: (String) -> Unit) {
    val c = LocalClipColors.current
    val context = LocalContext.current
    val prefs = container.prefs
    var quality by remember { mutableStateOf(prefs.defaultQuality) }
    var wifiOnly by remember { mutableStateOf(prefs.wifiOnly) }
    var accountsTick by remember { mutableIntStateOf(0) }
    var update by remember { mutableStateOf(UpdateUi.Idle) }
    var qualityMenu by remember { mutableStateOf(false) }
    var folderMenu by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf(prefs.downloadTree) }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val rw = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, rw)
            releaseFolder(context, prefs.downloadTree)
            prefs.downloadTree = uri.toString()
            folder = prefs.downloadTree
        }
    }
    val theme by prefs.themeFlow.collectAsStateWithLifecycle()
    val engine by container.engine.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Re-read login state after returning from LoginActivity.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) accountsTick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = insets.calculateTopPadding() + 32.dp, bottom = insets.calculateBottomPadding())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Settings", style = ClipType.Title, color = c.ink)

        Section("System") {
            SettingRow(
                icon = Icons.Outlined.Palette, title = "Theme",
                onClick = { onOpenPage(MainActivity.PAGE_THEME) },
            ) { Value(THEME_LABELS[theme] ?: "Light") }
            Divider()
            SettingRow(
                icon = Icons.Outlined.Notifications, title = "Notifications",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                },
            ) { Value(null) }
        }

        Section("Downloads") {
            SettingRow(icon = Icons.Outlined.Bolt, title = "Quality", onClick = { qualityMenu = true }) {
                Box {
                    Value(QUALITY_CHOICES.firstOrNull { it.first == quality }?.second ?: "Ask")
                    DropdownMenu(expanded = qualityMenu, onDismissRequest = { qualityMenu = false }, shape = RoundedCornerShape(Radius.Md)) {
                        QUALITY_CHOICES.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = if (key == quality) c.primary else c.ink) },
                                onClick = { quality = key; prefs.defaultQuality = key; qualityMenu = false },
                            )
                        }
                    }
                }
            }
            Divider()
            SettingRow(icon = Icons.Outlined.FolderOpen, title = "Save to", onClick = { folderMenu = true }) {
                Box {
                    Value(MediaStoreSaver.folderLabel(folder), maxWidth = 150.dp)
                    DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }, shape = RoundedCornerShape(Radius.Md)) {
                        DropdownMenuItem(text = { Text("Choose folder…") }, onClick = { folderMenu = false; pickFolder.launch(null) })
                        if (folder != null) {
                            DropdownMenuItem(
                                text = { Text("Use ${MediaStoreSaver.DEFAULT_FOLDER}") },
                                onClick = {
                                    folderMenu = false
                                    releaseFolder(context, prefs.downloadTree)
                                    prefs.downloadTree = null
                                    folder = null
                                },
                            )
                        }
                    }
                }
            }
            Divider()
            SettingRow(icon = Icons.Outlined.Wifi, title = "Wi-Fi only", onClick = { wifiOnly = !wifiOnly; prefs.wifiOnly = wifiOnly }) {
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = { wifiOnly = it; prefs.wifiOnly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = c.onPrimary,
                        checkedTrackColor = c.primary,
                        checkedBorderColor = c.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = c.inkFaint,
                        uncheckedBorderColor = c.inkFaint,
                    ),
                )
            }
        }

        // Reading accountsTick makes this recompose after sign-in/out.
        @Suppress("UNUSED_EXPRESSION") accountsTick
        Section("Accounts") {
            CookieStore.Site.entries.forEachIndexed { i, site ->
                if (i > 0) Divider()
                val signedIn = container.cookies.isLoggedIn(site)
                SettingRow(
                    icon = if (i == 0) Icons.Outlined.CameraAlt else Icons.Outlined.Groups,
                    title = site.key.replaceFirstChar { it.uppercase() },
                ) {
                    if (signedIn) {
                        GlassButton("Sign out", { container.cookies.clear(site); accountsTick++ })
                    } else {
                        GlassButton("Sign in", {
                            context.startActivity(Intent(context, LoginActivity::class.java).putExtra(LoginActivity.EXTRA_SITE, site.key))
                        })
                    }
                }
            }
        }

        Section("Other") {
            SettingRow(
                icon = Icons.Outlined.Refresh,
                iconWell = true,
                title = "Check for updates",
                caption = if (engine is EngineState.Failed) "Unavailable" else "v${BuildConfig.VERSION_NAME}",
                captionColor = if (engine is EngineState.Failed) c.danger else c.inkMuted,
            ) {
                GlassButton(
                    text = when (update) {
                        UpdateUi.Idle -> "Check"
                        UpdateUi.Checking -> "Checking…"
                        UpdateUi.UpToDate -> "Up to date"
                        UpdateUi.Failed -> "Retry"
                    },
                    enabled = update != UpdateUi.Checking && update != UpdateUi.UpToDate && engine is EngineState.Ready,
                    onClick = {
                        update = UpdateUi.Checking
                        scope.launch {
                            update = try {
                                container.engine.update()
                                UpdateUi.UpToDate
                            } catch (e: Throwable) {
                                UpdateUi.Failed
                            }
                        }
                    },
                )
            }
            Divider()
            SettingRow(icon = Icons.Outlined.HelpOutline, title = "How it works", onClick = { onOpenPage(MainActivity.PAGE_HOW) }) { Value(null) }
            Divider()
            SettingRow(icon = Icons.Outlined.Gavel, title = "Terms", onClick = { onOpenPage(MainActivity.PAGE_TERMS) }) { Value(null) }
            Divider()
            SettingRow(icon = Icons.Outlined.BugReport, title = "Report a bug", onClick = { sendFeedback(context, "Bug report") }) { Value(null) }
            Divider()
            SettingRow(icon = Icons.Outlined.Lightbulb, title = "Request a feature", onClick = { sendFeedback(context, "Feature request") }) { Value(null) }
        }
    }
}

/** Drops the saved access to a folder we no longer save to. */
private fun releaseFolder(context: Context, tree: String?) {
    if (tree == null) return
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            Uri.parse(tree),
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }
}

/** Where bug reports and feature requests go. */
private const val FEEDBACK_EMAIL = "sahedul.dev@gmail.com"

/** Opens the mail app with the app/device details filled in; no extra permissions or network code. */
private fun sendFeedback(context: Context, kind: String) {
    val body = "\n\n—\nClipSave ${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}"
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL).filter { it.isNotBlank() }.toTypedArray())
        .putExtra(Intent.EXTRA_SUBJECT, "ClipSave: $kind")
        .putExtra(Intent.EXTRA_TEXT, body)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No email app", Toast.LENGTH_SHORT).show()
    }
}

/** Small section label above a group. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = ClipType.Label, color = LocalClipColors.current.inkMuted, modifier = Modifier.padding(start = 16.dp))
        Group(content)
    }
}

/** Trailing value text with a chevron; no text means just the chevron. */
@Composable
private fun Value(text: String?, maxWidth: Dp = Dp.Unspecified) {
    val c = LocalClipColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (text != null) Text(text, style = ClipType.BodyStrong, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = maxWidth))
        Icon(Icons.Rounded.ChevronRight, null, tint = c.inkMuted, modifier = Modifier.size(20.dp))
    }
}

/** Sunken, rounded list group. */
@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().panel(RoundedCornerShape(Radius.Lg))) { content() }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(start = 56.dp), color = LocalClipColors.current.line)

/** Title-only row: icon, title, trailing control. A caption only when the value is data (a version). */
@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    iconWell: Boolean = false,
    caption: String? = null,
    captionColor: Color = LocalClipColors.current.inkMuted,
    trailing: @Composable () -> Unit,
) {
    val c = LocalClipColors.current
    Row(
        Modifier.fillMaxWidth().height(if (caption != null) 68.dp else 60.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconWell) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = c.onPrimarySoft, modifier = Modifier.size(18.dp))
            }
        } else {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = c.ink, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = ClipType.BodyStrong, color = c.ink)
            if (caption != null) Text(caption, style = ClipType.Caption, color = captionColor)
        }
        trailing()
    }
}
