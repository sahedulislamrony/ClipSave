package com.sahedul.clipsave.download

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Download history, persisted as JSON. A few hundred rows at most, so no database needed. */
class DownloadRepository(context: Context) {
    private val file = File(context.filesDir, "downloads.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(DownloadItem.serializer())

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<DownloadItem>> = _items

    fun get(id: String) = _items.value.firstOrNull { it.id == id }

    fun add(item: DownloadItem) = mutate { listOf(item) + it }

    /** Adds [items] as if added one by one in order, so the last one is newest. */
    fun addAll(items: List<DownloadItem>) = mutate { items.asReversed() + it }

    fun update(id: String, transform: (DownloadItem) -> DownloadItem) =
        mutate { list -> list.map { if (it.id == id) transform(it) else it } }

    /** One write for many rows (pause/resume all), instead of persisting once per row. */
    fun updateWhere(predicate: (DownloadItem) -> Boolean, transform: (DownloadItem) -> DownloadItem) =
        mutate { list -> list.map { if (predicate(it)) transform(it) else it } }

    fun remove(id: String) = mutate { list -> list.filterNot { it.id == id } }

    fun clearFinished() = mutate { list -> list.filter { it.isActive || it.status == DownloadStatus.PAUSED } }

    fun removeWhere(predicate: (DownloadItem) -> Boolean) = mutate { list -> list.filterNot(predicate) }

    private fun mutate(block: (List<DownloadItem>) -> List<DownloadItem>) {
        synchronized(this) {
            _items.update(block)
            persist()
        }
    }

    private fun load(): List<DownloadItem> = runCatching {
        if (!file.exists()) return emptyList()
        json.decodeFromString(serializer, file.readText()).map {
            // The process died mid-download: don't show a phantom running task.
            if (it.isActive) it.copy(status = DownloadStatus.FAILED, error = INTERRUPTED) else it
        }
    }.getOrDefault(emptyList())

    private fun persist() {
        runCatching {
            val tmp = File(file.parentFile, "downloads.json.tmp")
            tmp.writeText(json.encodeToString(serializer, _items.value))
            tmp.renameTo(file)
        }
    }

    companion object {
        const val INTERRUPTED = "Interrupted"
    }
}
