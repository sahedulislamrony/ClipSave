package com.sahed.clipsave.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahed.clipsave.core.FormatOption
import com.sahed.clipsave.core.MediaInfo
import com.sahed.clipsave.core.Numbering
import com.sahed.clipsave.core.formatDuration
import com.sahed.clipsave.core.formatSize
import com.sahed.clipsave.ui.theme.ClipType
import com.sahed.clipsave.ui.theme.GlassButton
import com.sahed.clipsave.ui.theme.GlassIconButton
import com.sahed.clipsave.ui.theme.LocalClipColors
import com.sahed.clipsave.ui.theme.PrimaryButton
import com.sahed.clipsave.ui.theme.Radius
import com.sahed.clipsave.ui.theme.SegOption
import com.sahed.clipsave.ui.theme.Segmented

/**
 * The quality sheet: a detached glass sheet (8dp inset, 34dp radius) opened by
 * [ShareReceiverActivity] when something is shared to ClipSave, and by Home's Download button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractSheet(
    vm: ExtractViewModel,
    onDismiss: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
) {
    val c = LocalClipColors.current
    val state by vm.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    (state as? ExtractState.Started)?.let { started ->
        LaunchedEffect(started) {
            Toast.makeText(context, "Downloading: ${started.title}", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = c.scrim,
        shape = RoundedCornerShape(0.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        val shape = RoundedCornerShape(Radius.Xl)
        Box(
            Modifier
                .imePadding()
                .navigationBarsPadding()
                .padding(8.dp)
                .shadow(24.dp, shape, ambientColor = c.shadowGlass, spotColor = c.shadowGlass)
                .clip(shape)
                .background(c.surface.copy(alpha = 0.98f))
                .background(Brush.verticalGradient(listOf(c.primary.copy(alpha = if (c.dark) 0.16f else 0.10f), Color.Transparent)))
                .border(1.dp, Brush.verticalGradient(listOf(c.glassHighlight, c.glassEdge)), shape),
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 5.dp).clip(CircleShape).background(c.inkFaint.copy(alpha = 0.5f)))
                Spacer(Modifier.height(12.dp))
                when (val s = state) {
                    is ExtractState.Loading, ExtractState.Idle, is ExtractState.Started -> LoadingBody()
                    is ExtractState.Ready -> ReadyBody(s.info, onClose = onDismiss, onPick = { option, name -> vm.download(s.info, option, name) })
                    is ExtractState.PlaylistReady -> PlaylistBody(
                        s,
                        onClose = onDismiss,
                        onPick = { option, folder, numbering -> vm.downloadPlaylist(s.playlist, option, folder, numbering) },
                        onSingle = s.playlist.videoUrl?.let { url -> { vm.load(url, single = true) } },
                    )
                    is ExtractState.Error -> ErrorBody(s, onDismiss, onOpenSettings)
                }
            }
        }
    }
}

@Composable
private fun LoadingBody() {
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LocalClipColors.current.primary, trackColor = Color.Transparent)
    }
}

@Composable
private fun ErrorBody(s: ExtractState.Error, onDismiss: () -> Unit, onOpenSettings: (() -> Unit)?) {
    val c = LocalClipColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Can't download", style = ClipType.Headline, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text(s.message, style = ClipType.BodyStrong, color = c.inkMuted)
        Spacer(Modifier.height(20.dp))
        if (s.needsLogin && onOpenSettings != null) {
            PrimaryButton("Sign in", onOpenSettings, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        GlassButton("Close", onDismiss, Modifier.fillMaxWidth(), height = 48.dp)
    }
}

@Composable
private fun ReadyBody(info: MediaInfo, onClose: () -> Unit, onPick: (FormatOption, String) -> Unit) {
    val c = LocalClipColors.current
    val videos = info.options.filter { !it.isAudio }.take(4)
    val audios = info.options.filter { it.isAudio }.take(4)
    var audio by remember(info) { mutableStateOf(videos.isEmpty() && audios.isNotEmpty()) }
    var picked by remember(info) { mutableStateOf<FormatOption?>(null) }
    var name by remember(info) { mutableStateOf(info.title.take(100)) }
    val shown = if (audio) audios else videos
    val selected = picked?.takeIf { it in shown } ?: shown.firstOrNull()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Thumb(info.thumbnail, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Choose quality", style = ClipType.Headline, color = c.ink)
            Text(
                listOfNotNull(info.platform.label, formatDuration(info.durationSec)).joinToString(" · "),
                style = ClipType.Caption,
                color = c.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GlassIconButton(Icons.Rounded.Close, "Close", onClose, size = 40.dp, shadow = false)
    }
    Spacer(Modifier.height(16.dp))
    NameField(name) { name = it }
    Spacer(Modifier.height(12.dp))
    if (videos.isNotEmpty() && audios.isNotEmpty()) {
        Segmented(
            options = listOf(SegOption(false, "Video", Icons.Outlined.Videocam), SegOption(true, "Audio", Icons.Outlined.MusicNote)),
            selected = audio,
            onSelect = { audio = it },
            fill = true,
        )
        Spacer(Modifier.height(12.dp))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.forEach { option -> OptionRow(option, option == selected) { picked = option } }
    }
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Download", { selected?.let { onPick(it, name.trim()) } }, Modifier.fillMaxWidth(), enabled = selected != null)
}

/**
 * Playlist sheet: folder name (defaults to the playlist title), how files are numbered, and one
 * quality cap for every video. Files land in `<download folder>/<folder>/`.
 */
@Composable
private fun PlaylistBody(
    s: ExtractState.PlaylistReady,
    onClose: () -> Unit,
    onPick: (FormatOption, String, Numbering) -> Unit,
    onSingle: (() -> Unit)?,
) {
    val c = LocalClipColors.current
    val playlist = s.playlist
    val total = playlist.entries.size
    val options = remember { FormatOption.playlist() }
    val videos = options.filter { !it.isAudio }
    val audios = options.filter { it.isAudio }
    var audio by remember(playlist) { mutableStateOf(s.preferred?.isAudio == true) }
    var picked by remember(playlist) { mutableStateOf(s.preferred) }
    var folder by remember(playlist) { mutableStateOf(playlist.title.take(100)) }
    var numbering by remember(playlist) { mutableStateOf(s.numbering) }
    val shown = if (audio) audios else videos
    val selected = picked?.takeIf { it in shown } ?: shown.first()

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumb(playlist.thumbnail, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Download playlist", style = ClipType.Headline, color = c.ink)
                Text(
                    listOfNotNull(playlist.platform.label, if (total == 1) "1 video" else "$total videos", playlist.uploader).joinToString(" · "),
                    style = ClipType.Caption,
                    color = c.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GlassIconButton(Icons.Rounded.Close, "Close", onClose, size = 40.dp, shadow = false)
        }
        Spacer(Modifier.height(16.dp))
        FieldLabel("Folder")
        NameField(folder, placeholder = "Folder name") { folder = it }
        Spacer(Modifier.height(12.dp))
        FieldLabel("Number files")
        Segmented(
            options = listOf(
                SegOption(Numbering.ASCENDING, "1 → $total"),
                SegOption(Numbering.DESCENDING, "$total → 1"),
                SegOption(Numbering.NONE, "None"),
            ),
            selected = numbering,
            onSelect = { numbering = it },
            fill = true,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "First file: " + numbering.fileName(playlist.entries.first().title, 0, total),
            style = ClipType.Caption,
            color = c.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        Segmented(
            options = listOf(SegOption(false, "Video", Icons.Outlined.Videocam), SegOption(true, "Audio", Icons.Outlined.MusicNote)),
            selected = audio,
            onSelect = { audio = it },
            fill = true,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shown.forEach { option -> OptionRow(option, option == selected) { picked = option } }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            if (total == 1) "Download 1 video" else "Download $total videos",
            { onPick(selected, folder.trim(), numbering) },
            Modifier.fillMaxWidth(),
        )
        if (onSingle != null) {
            Spacer(Modifier.height(8.dp))
            GlassButton("Only this video", onSingle, Modifier.fillMaxWidth(), height = 48.dp, shadow = false)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = ClipType.Label,
        color = LocalClipColors.current.inkMuted,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )
}

/** File name, editable. Starts as the clip's title; the extension is added on save. */
@Composable
private fun NameField(value: String, placeholder: String = "Name", onValue: (String) -> Unit) {
    val c = LocalClipColors.current
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(CircleShape)
            .background(if (focused) c.surface else c.surfaceSunken)
            .then(if (focused) Modifier.border(2.dp, c.primary, CircleShape) else Modifier)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = ClipType.BodyStrong, color = c.inkFaint)
            BasicTextField(
                value = value,
                onValueChange = { onValue(it.take(100)) },
                singleLine = true,
                textStyle = ClipType.BodyStrong.copy(color = c.ink),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Outlined.Edit, "Rename", tint = c.inkMuted, modifier = Modifier.size(18.dp))
    }
}

/** Resolution, optional tag, size. Selected = iris-soft fill, 2dp iris ring, tick. */
@Composable
private fun OptionRow(option: FormatOption, selected: Boolean, onClick: () -> Unit) {
    val c = LocalClipColors.current
    val shape = RoundedCornerShape(Radius.Md)
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(shape)
            .background(if (selected) c.primarySoft else c.surfaceSunken)
            .then(if (selected) Modifier.border(2.dp, c.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(option.label, style = ClipType.Headline, color = c.ink)
        val tag = when {
            (option.height ?: 0) >= 2160 -> "4K"
            (option.height ?: 0) >= 1080 -> "HD"
            else -> null
        }
        if (tag != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                tag,
                style = ClipType.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = if (tag == "4K") c.bg else c.onPrimary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (tag == "4K") c.ink else c.primary).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        formatSize(option.sizeBytes)?.let {
            Text(it, style = ClipType.BodyStrong, color = if (selected) c.onPrimarySoft else c.inkMuted)
        }
        if (selected) {
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(24.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = c.onPrimary, modifier = Modifier.size(15.dp))
            }
        }
    }
}
