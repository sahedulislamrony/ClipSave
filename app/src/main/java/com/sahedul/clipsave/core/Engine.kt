package com.sahedul.clipsave.core

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface EngineState {
    data object Starting : EngineState
    data class Ready(val version: String?) : EngineState
    data class Failed(val message: String) : EngineState
}

/**
 * Owns the embedded yt-dlp + ffmpeg runtime. Site extractors break whenever Instagram/Facebook
 * change their pages, so [update] pulls the latest yt-dlp and keeps extractors current without
 * shipping a new app version.
 */
class Engine(private val app: Application, scope: CoroutineScope, private val prefs: Prefs) {
    private val ready = CompletableDeferred<Unit>()
    private val updateLock = Mutex()
    private val _state = MutableStateFlow<EngineState>(EngineState.Starting)
    val state: StateFlow<EngineState> = _state

    init {
        scope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
                _state.value = EngineState.Ready(version(app))
                ready.complete(Unit)
            } catch (e: Throwable) {
                Log.e("Engine", "yt-dlp init failed", e)
                _state.value = EngineState.Failed(e.message ?: e.javaClass.simpleName)
                ready.completeExceptionally(e)
            }
        }
    }

    suspend fun awaitReady() = ready.await()

    private fun version(app: Application): String? =
        runCatching { YoutubeDL.version(app) }.getOrNull()

    /** @return the yt-dlp version after the update attempt. */
    suspend fun update(): String? = updateLock.withLock {
        awaitReady()
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().updateYoutubeDL(app, YoutubeDL.UpdateChannel.STABLE)
            prefs.lastUpdateMs = System.currentTimeMillis()
            val v = version(app)
            _state.value = EngineState.Ready(v)
            v
        }
    }

    /** Refresh at most once a day, and never while a download might be using the files. */
    suspend fun updateIfStale(hasActiveDownloads: Boolean) {
        val dayMs = 24L * 3600 * 1000
        if (hasActiveDownloads || System.currentTimeMillis() - prefs.lastUpdateMs < dayMs) return
        runCatching { update() }
    }
}
