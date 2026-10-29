package com.sahedul.clipsave.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahedul.clipsave.AppContainer
import com.sahedul.clipsave.download.DownloadItem
import com.sahedul.clipsave.download.DownloadStatus
import com.sahedul.clipsave.download.DownloadWorker
import com.sahedul.clipsave.download.MediaStoreSaver
import com.sahedul.clipsave.download.Notifications
import com.sahedul.clipsave.ui.theme.ClipType
import com.sahedul.clipsave.ui.theme.GlassButton
import com.sahedul.clipsave.ui.theme.GlassIconButton
import com.sahedul.clipsave.ui.theme.LocalClipColors
import com.sahedul.clipsave.ui.theme.Radius
import com.sahedul.clipsave.ui.theme.panel
import com.sahedul.clipsave.ui.theme.SegOption
import com.sahedul.clipsave.ui.theme.Segmented

private enum class Filter { All, Active, Saved, Playlists }

private enum class Section(val title: String) {
    Downloading("Downloading"), Paused("Paused"), Queued("Queued"), Failed("Failed"), Saved("Saved");

    companion object {
        fun of(item: DownloadItem) = when (item.status) {
            DownloadStatus.RUNNING, DownloadStatus.PROCESSING -> Downloading
            DownloadStatus.PAUSED -> Paused
            DownloadStatus.QUEUED -> Queued
            DownloadStatus.FAILED, DownloadStatus.CANCELED -> Failed
            DownloadStatus.DONE -> Saved
        }
    }
}

private sealed interface Entry {
    val key: String
    class Header(val section: Section, val count: Int) : Entry { override val key = "h-${section.name}" }
    class Row(val item: DownloadItem, val queuePos: Int?) : Entry { override val key = item.id }
    class Playlist(val group: PlaylistGroup) : Entry { override val key = "p-${group.folder}" }
    class PlaylistHeader(val group: PlaylistGroup) : Entry { override val key = "ph-${group.folder}" }
}

/** Every playlist video saved into one folder, in playlist order. Downloading the same playlist again merges into it. */
private class PlaylistGroup(val folder: String, val items: List<DownloadItem>) {
    val saved = items.count { it.status == DownloadStatus.DONE }
    val active = items.count { it.isActive }
    val paused = items.count { it.status == DownloadStatus.PAUSED }
    val failed = items.count { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELED }
    val pausable = items.any { it.isPausable }
    val thumbnail = items.firstNotNullOfOrNull { it.thumbnail }
    val latest = items.maxOf { it.createdAt }

    val summary = buildList {
        add(if (saved == items.size) "All ${items.size} saved" else "$saved/${items.size} saved")
        if (active > 0) add("$active active")
        if (paused > 0) add("$paused paused")
        if (failed > 0) add("$failed failed")
    }.joinToString(" · ")

    fun has(item: DownloadItem) = item.folder == folder && item.batchId != null

    companion object {
        fun from(all: List<DownloadItem>): List<PlaylistGroup> =
            all.filter { it.batchId != null && it.folder != null }
                .groupBy { it.folder!! }
                .map { (folder, items) -> PlaylistGroup(folder, items.sortedBy { it.createdAt }) }
                .sortedByDescending { it.latest }
    }
}

@Composable
fun DownloadsScreen(container: AppContainer, insets: PaddingValues) {
    val c = LocalClipColors.current
    val all by container.downloads.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    // The playlist opened from the Playlists filter; null shows the list of playlists.
    var openFolder by rememberSaveable { mutableStateOf<String?>(null) }

    val bySection = remember(all) { all.groupBy { Section.of(it) } }
    val playlists = remember(all) { PlaylistGroup.from(all) }
    val opened = playlists.firstOrNull { it.folder == openFolder }.takeIf { filter == Filter.Playlists }
    BackHandler(enabled = opened != null) { openFolder = null }
    val activeCount = all.count { it.isActive }
    val pausable = all.any { it.isPausable }
    val pausedCount = all.count { it.status == DownloadStatus.PAUSED }
    val shown = when (filter) {
        Filter.All -> Section.entries
        Filter.Active -> listOf(Section.Downloading, Section.Paused, Section.Queued, Section.Failed)
        Filter.Saved -> listOf(Section.Saved)
        Filter.Playlists -> emptyList()
    }
    val entries = remember(all, filter, opened) {
        buildList {
            when {
                opened != null -> {
                    add(Entry.PlaylistHeader(opened))
                    var queued = 0
                    opened.items.forEach { add(Entry.Row(it, if (it.status == DownloadStatus.QUEUED) ++queued else null)) }
                }
                filter == Filter.Playlists -> playlists.forEach { add(Entry.Playlist(it)) }
            }
            for (section in shown) {
                // Waiting rows read in the order they will run (playlist order), not newest first.
                val inRunOrder = section == Section.Queued || section == Section.Paused
                val rows = bySection[section].orEmpty().let { if (inRunOrder) it.sortedBy { i -> i.createdAt } else it }
                if (rows.isEmpty()) continue
                add(Entry.Header(section, rows.size))
                rows.forEachIndexed { i, item -> add(Entry.Row(item, if (section == Section.Queued) i + 1 else null)) }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "top") {
            Column {
                Spacer(Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Downloads", style = ClipType.Title, color = c.ink, modifier = Modifier.weight(1f))
                    // One toggle: pause while anything is moving, otherwise resume what was paused.
                    when {
                        pausable -> GlassButton("Pause all", { container.scheduler.pauseAll() }, icon = Icons.Rounded.Pause)
                        pausedCount > 0 -> GlassButton("Resume all", { container.scheduler.resumeAll() }, icon = Icons.Rounded.PlayArrow)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Segmented(
                    options = listOf(
                        SegOption(Filter.All, "All"),
                        SegOption(Filter.Active, if (activeCount > 0) "Active $activeCount" else "Active"),
                        SegOption(Filter.Saved, "Saved"),
                        SegOption(Filter.Playlists, "Playlists"),
                    ),
                    selected = filter,
                    onSelect = { filter = it; openFolder = null },
                    glass = true,
                    fill = true,
                    itemPadding = 6.dp,
                )
            }
        }
        if (entries.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(top = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val lists = filter == Filter.Playlists
                    Box(Modifier.size(96.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
                        Icon(if (lists) Icons.AutoMirrored.Outlined.PlaylistPlay else Icons.Outlined.Download, null, tint = c.onPrimarySoft, modifier = Modifier.size(36.dp))
                    }
                    if (lists) {
                        Spacer(Modifier.height(16.dp))
                        Text("Playlists you download show up here", style = ClipType.BodyStrong, color = c.inkMuted)
                    }
                }
            }
        }
        items(entries, key = { it.key }) { entry ->
            when (entry) {
                is Entry.Header -> SectionHeader(entry, container)
                is Entry.Playlist -> PlaylistCard(entry.group) { openFolder = entry.group.folder }
                is Entry.PlaylistHeader -> PlaylistHeader(
                    entry.group,
                    onBack = { openFolder = null },
                    onPause = { container.scheduler.pauseAll(entry.group::has) },
                    onResume = { container.scheduler.resumeAll(entry.group::has) },
                )
                is Entry.Row -> DownloadRow(
                    entry.item, entry.queuePos,
                    onOpen = { openDownload(context, entry.item) },
                    onShare = { shareDownload(context, entry.item) },
                    onRetry = { container.scheduler.retry(entry.item.id) },
                    onResume = { container.scheduler.resume(entry.item.id) },
                    onPause = { container.scheduler.pause(entry.item.id) },
                    onDelete = { deleteDownload(context, container, entry.item) },
                )
            }
        }
    }
}

/** A playlist folder in the Playlists filter: cover, video count, how much is saved. */
@Composable
private fun PlaylistCard(group: PlaylistGroup, onOpen: () -> Unit) {
    val c = LocalClipColors.current
    val shape = RoundedCornerShape(Radius.Lg)
    Row(
        Modifier.fillMaxWidth().panel(shape)
            .then(if (group.active > 0) Modifier.background(c.primary.copy(alpha = 0.10f)) else Modifier)
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 96.dp, height = 68.dp).clip(RoundedCornerShape(Radius.Sm))) {
            Thumb(group.thumbnail, Modifier.fillMaxSize())
            Row(
                Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.62f)).padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("${group.items.size}", style = ClipType.Caption, color = Color.White)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(group.folder, style = ClipType.BodyStrong, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            if (group.saved < group.items.size) {
                ProgressBar(group.saved / group.items.size.toFloat(), true, c.primary, track = c.surface)
                Spacer(Modifier.height(6.dp))
            }
            Text(group.summary, style = ClipType.Caption, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = c.inkMuted, modifier = Modifier.size(20.dp))
    }
}

/** Top of an opened playlist: back, name and counts, and a pause/resume toggle for just this playlist. */
@Composable
private fun PlaylistHeader(group: PlaylistGroup, onBack: () -> Unit, onPause: () -> Unit, onResume: () -> Unit) {
    val c = LocalClipColors.current
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack, size = 40.dp, shadow = false)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(group.folder, style = ClipType.Headline, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(group.summary, style = ClipType.Caption, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        when {
            group.pausable -> GlassButton("Pause", onPause, icon = Icons.Rounded.Pause, shadow = false)
            group.paused > 0 -> GlassButton("Resume", onResume, icon = Icons.Rounded.PlayArrow, shadow = false)
        }
    }
}

@Composable
private fun SectionHeader(h: Entry.Header, container: AppContainer) {
    val c = LocalClipColors.current
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(h.section.title, style = ClipType.Headline, color = c.ink)
        Text("  ${h.count}", style = ClipType.Headline, color = c.inkMuted, modifier = Modifier.weight(1f))
        when (h.section) {
            Section.Queued -> GlassButton("Cancel all", {
                container.downloads.items.value.filter { it.status == DownloadStatus.QUEUED }.forEach { container.scheduler.cancel(it.id) }
            }, shadow = false)
            Section.Failed -> GlassButton("Clear", {
                container.downloads.removeWhere { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELED }
            }, shadow = false)
            Section.Saved -> GlassButton("Clear", { container.downloads.removeWhere { it.status == DownloadStatus.DONE } }, shadow = false)
            Section.Paused, Section.Downloading -> Unit
        }
    }
}

/** One download: thumbnail, name, a status line that fits the state, and one trailing control. */
@Composable
private fun DownloadRow(
    item: DownloadItem,
    queuePos: Int?,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalClipColors.current
    val running = item.status == DownloadStatus.RUNNING
    val processing = item.status == DownloadStatus.PROCESSING
    val failed = item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELED
    val done = item.status == DownloadStatus.DONE
    val paused = item.status == DownloadStatus.PAUSED
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(Radius.Lg)

    Row(
        Modifier.fillMaxWidth().panel(shape)
            .then(if (running || processing) Modifier.background(c.primary.copy(alpha = 0.10f)) else Modifier)
            .clickable(enabled = done || failed || paused) { if (done) onOpen() else if (paused) onResume() else onRetry() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 96.dp, height = 68.dp).clip(RoundedCornerShape(Radius.Sm))) {
            Thumb(item.thumbnail, Modifier.fillMaxSize())
            if (done) PlayDot(item.audioFormat != null, Modifier.align(Alignment.Center), size = 28.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = ClipType.BodyStrong, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            when {
                running || processing -> {
                    // Before the first byte yt-dlp starts up, re-reads the video and solves YouTube's player
                    // challenge (several seconds); a frozen "0%" there looked stuck.
                    val preparing = running && item.progress <= 0f && item.etaSec == null
                    ProgressBar(item.progress, running && !preparing, c.primary, track = c.surface)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            preparing -> listOf("Preparing…", item.optionLabel).joinToString(" · ")
                            running -> listOfNotNull("${(item.progress * 100).toInt()}%", item.etaSec?.let { "${it}s" }, item.optionLabel).joinToString(" · ")
                            else -> "Saving…"
                        },
                        style = ClipType.Caption, color = c.onPrimarySoft, maxLines = 1,
                    )
                }
                paused -> {
                    ProgressBar(item.progress, true, c.inkFaint, track = c.surface)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOf("Paused", "${(item.progress * 100).toInt()}%", item.optionLabel).joinToString(" · "),
                        style = ClipType.Caption, color = c.inkMuted, maxLines = 1,
                    )
                }
                item.status == DownloadStatus.QUEUED ->
                    Text("#$queuePos · ${item.optionLabel}", style = ClipType.Caption, color = c.inkMuted, maxLines = 1)
                failed ->
                    Text(if (item.status == DownloadStatus.CANCELED) "Canceled" else item.error ?: "Failed", style = ClipType.Caption, color = c.danger, maxLines = 1, overflow = TextOverflow.Ellipsis)
                else ->
                    Text("${item.optionLabel} · ${item.platform}", style = ClipType.Caption, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            // Downloading or waiting: pause only. Removing it takes a pause first, so nothing is lost by a stray tap.
            item.isPausable -> GlassIconButton(Icons.Rounded.Pause, "Pause", onPause, size = 40.dp, shadow = false)
            processing -> Unit
            paused -> {
                GlassIconButton(Icons.Rounded.PlayArrow, "Resume", onResume, size = 40.dp, shadow = false)
                Spacer(Modifier.width(6.dp))
                GlassIconButton(Icons.Rounded.Delete, "Delete", onDelete, size = 40.dp, tint = c.danger, shadow = false)
            }
            else -> Box {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable { menu = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.MoreVert, "More", tint = c.inkMuted) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(Radius.Md)) {
                    if (done) DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { menu = false; onShare() })
                    if (failed) DropdownMenuItem(text = { Text("Retry") }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) }, onClick = { menu = false; onRetry() })
                    DropdownMenuItem(
                        text = { Text("Delete", color = c.danger) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = c.danger) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}

internal fun openDownload(context: Context, item: DownloadItem) {
    val uri = item.outputUri ?: return
    try {
        context.startActivity(Notifications.viewIntent(uri, item.mimeType))
    } catch (e: Exception) {
        Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
    }
}

internal fun shareDownload(context: Context, item: DownloadItem) {
    val uri = item.outputUri ?: return
    val send = Intent(Intent.ACTION_SEND)
        .setType(item.mimeType ?: "*/*")
        .putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

internal fun deleteDownload(context: Context, container: AppContainer, item: DownloadItem) {
    if (item.status == DownloadStatus.DONE) MediaStoreSaver.delete(context, item.outputUri)
    // Paused or interrupted downloads leave a partial file behind for resuming.
    else DownloadWorker.workDir(context, item.id).deleteRecursively()
    container.downloads.remove(item.id)
}
