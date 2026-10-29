package com.sahedul.clipsave.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sahedul.clipsave.ClipSaveApp
import com.sahedul.clipsave.core.ExtractFailure
import com.sahedul.clipsave.core.FormatOption
import com.sahedul.clipsave.core.MediaInfo
import com.sahedul.clipsave.core.Numbering
import com.sahedul.clipsave.core.PlaylistInfo
import com.sahedul.clipsave.core.UrlParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ExtractState {
    data object Idle : ExtractState
    data class Loading(val url: String) : ExtractState
    data class Ready(val info: MediaInfo) : ExtractState
    /**
     * [preferred] is the saved default quality, preselected; a playlist never auto-starts.
     * [selected] holds playlist indexes ([com.sahedul.clipsave.core.PlaylistEntry.index]), at most
     * [PlaylistInfo.MAX_SELECTED] across every page read so far.
     */
    data class PlaylistReady(
        val playlist: PlaylistInfo,
        val numbering: Numbering,
        val preferred: FormatOption?,
        val selected: Set<Int>,
        val loadingMore: Boolean = false,
        val moreError: String? = null,
    ) : ExtractState
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
                    _state.value = ExtractState.PlaylistReady(
                        playlist,
                        Numbering.of(container.prefs.playlistNumbering),
                        preferred,
                        // The first page starts selected, up to the cap.
                        selected = playlist.entries.take(PlaylistInfo.MAX_SELECTED).map { it.index }.toSet(),
                    )
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

    /** Reads the playlist's next page; its videos start unselected so the current pick stays as it is. */
    fun loadMore() {
        val s = _state.value as? ExtractState.PlaylistReady ?: return
        val start = s.playlist.nextStart ?: return
        if (s.loadingMore) return
        _state.value = s.copy(loadingMore = true, moreError = null)
        job = viewModelScope.launch {
            val result = runCatching { container.extractor.fetchPlaylist(s.playlist.url, start) }
            val now = _state.value as? ExtractState.PlaylistReady ?: return@launch
            _state.value = result.fold(
                onSuccess = { page -> now.copy(playlist = now.playlist.plus(page), loadingMore = false) },
                onFailure = { e -> now.copy(loadingMore = false, moreError = (e as? ExtractFailure)?.message ?: "Couldn't load more videos") },
            )
        }
    }

    /** @return false when selecting would go past [PlaylistInfo.MAX_SELECTED]. */
    fun toggle(index: Int): Boolean {
        val s = _state.value as? ExtractState.PlaylistReady ?: return true
        val sel = s.selected
        if (index !in sel && sel.size >= PlaylistInfo.MAX_SELECTED) return false
        _state.value = s.copy(selected = if (index in sel) sel - index else sel + index)
        return true
    }

    /** Clears the selection, or when nothing is selected, selects the first [PlaylistInfo.MAX_SELECTED] videos read. */
    fun clearOrSelectAll() {
        val s = _state.value as? ExtractState.PlaylistReady ?: return
        _state.value = s.copy(
            selected = if (s.selected.isNotEmpty()) emptySet()
            else s.playlist.entries.take(PlaylistInfo.MAX_SELECTED).map { it.index }.toSet(),
        )
    }

    fun downloadPlaylist(s: ExtractState.PlaylistReady, option: FormatOption, folder: String, numbering: Numbering) {
        container.prefs.playlistNumbering = numbering.key
        val picked = s.playlist.entries.filter { it.index in s.selected }
        val count = container.scheduler.enqueuePlaylist(s.playlist, picked, option, folder, numbering)
        _state.value = ExtractState.Started("${folder.ifBlank { s.playlist.title }} ($count videos)")
    }

    fun reset() {
        job?.cancel()
        _state.value = ExtractState.Idle
    }
}
