package com.sahedul.clipsave.core

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("clipsave", Context.MODE_PRIVATE)

    /** "ask" shows the format picker; otherwise a [FormatOption] key ("best", "1080", "720", "mp3"…). */
    var defaultQuality: String
        get() = sp.getString("default_quality", "ask") ?: "ask"
        set(v) = sp.edit { putString("default_quality", v) }

    var wifiOnly: Boolean
        get() = sp.getBoolean("wifi_only", false)
        set(v) = sp.edit { putBoolean("wifi_only", v) }

    private val _theme = MutableStateFlow(sp.getString("theme", THEME_LIGHT) ?: THEME_LIGHT)

    /** Light is the default; [THEME_DARK] and [THEME_SYSTEM] are opt-in from Settings. */
    val themeFlow: StateFlow<String> = _theme
    var theme: String
        get() = _theme.value
        set(v) {
            sp.edit { putString("theme", v) }
            _theme.value = v
        }

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit { putBoolean("onboarded", v) }

    /** Tree URI picked with the system folder picker; null saves to Movies/ClipSave (Music/ClipSave for audio). */
    var downloadTree: String?
        get() = sp.getString("download_tree", null)
        set(v) = sp.edit { if (v == null) remove("download_tree") else putString("download_tree", v) }

    /** Last [Numbering] key picked for a playlist; ascending by default. */
    var playlistNumbering: String
        get() = sp.getString("playlist_numbering", Numbering.ASCENDING.key) ?: Numbering.ASCENDING.key
        set(v) = sp.edit { putString("playlist_numbering", v) }

    var lastUpdateMs: Long
        get() = sp.getLong("last_update", 0L)
        set(v) = sp.edit { putLong("last_update", v) }

    /** App release the user put off with "Update later"; the start-up check won't offer it again. */
    var dismissedUpdate: String?
        get() = sp.getString("dismissed_update", null)
        set(v) = sp.edit { if (v == null) remove("dismissed_update") else putString("dismissed_update", v) }

    companion object {
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }
}
