package com.sahed.clipsave.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sahed.clipsave.ClipSaveApp
import com.sahed.clipsave.core.ExtractFailure
import com.sahed.clipsave.core.FormatOption
import com.sahed.clipsave.core.MediaInfo
import com.sahed.clipsave.core.Numbering
import com.sahed.clipsave.core.PlaylistInfo
import com.sahed.clipsave.core.UrlParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ExtractState {
    data object Idle : ExtractState
    data class Loading(val url: String) : ExtractState
    data class Ready(val info: MediaInfo) : ExtractState
    /** [preferred] is the saved default quality, preselected; a playlist never auto-starts. */
    data class PlaylistReady(val playlist: PlaylistInfo, val numbering: Numbering, val preferred: FormatOption?) : ExtractState
    data class Error(val message: String, val needsLogin: Boolean) : ExtractState
    data class Started(val title: String) : ExtractState
}

/** Shared by the share sheet and the in-app "paste link" sheet: link -> qualities -> enqueue. */
class ExtractViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as ClipSaveApp).container
    private val _state = MutableStateFlow<ExtractState>(ExtractState.Idle)
    val state: StateFlow<ExtractState> = _state
    private var job: Job? = null

    /** [single] forces one video even when the link also names a playlist ("Only this video"). */
    fun load(url: String, single: Boolean = false) {
        job?.cancel()
        _state.value = ExtractState.Loading(url)
        job = viewModelScope.launch {
            try {
                if (!single && UrlParser.isYoutubePlaylist(url)) {
                    val playlist = container.extractor.fetchPlaylist(url)
                    val preferred = container.prefs.defaultQuality.takeIf { it != "ask" }?.let { FormatOption.pick(FormatOption.playlist(), it) }
                    _state.value = ExtractState.PlaylistReady(playlist, Numbering.of(container.prefs.playlistNumbering), preferred)
                    return@launch
                }
                val info = container.extractor.fetch(url)
                val preferred = container.prefs.defaultQuality
                // "One tap" mode: skip the picker and start with the saved default quality.
                val auto = if (preferred != "ask") FormatOption.pick(info.options, preferred) else null
                if (auto != null) download(info, auto) else _state.value = ExtractState.Ready(info)
            } catch (e: ExtractFailure) {
                _state.value = ExtractState.Error(e.message.orEmpty(), e.needsLogin)
            }
        }
    }

    fun download(info: MediaInfo, option: FormatOption, fileName: String? = null) {
        container.scheduler.enqueue(info, option, fileName)
        _state.value = ExtractState.Started(fileName?.takeIf { it.isNotBlank() } ?: info.title)
    }

    fun downloadPlaylist(playlist: PlaylistInfo, option: FormatOption, folder: String, numbering: Numbering) {
        container.prefs.playlistNumbering = numbering.key
        val count = container.scheduler.enqueuePlaylist(playlist, option, folder, numbering)
        _state.value = ExtractState.Started("${folder.ifBlank { playlist.title }} ($count videos)")
    }

    fun reset() {
        job?.cancel()
        _state.value = ExtractState.Idle
    }
}
