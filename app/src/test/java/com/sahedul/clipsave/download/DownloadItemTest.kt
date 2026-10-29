package com.sahedul.clipsave.download

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadItemTest {

    private fun item(status: DownloadStatus) = DownloadItem(
        id = "1", url = "https://example.com/v", title = "T", thumbnail = null, platform = "Web",
        optionKey = "best", optionLabel = "Best quality", selector = "bv*+ba/b", audioFormat = null,
        createdAt = 0L, status = status,
    )

    @Test fun `active means queued, running or saving`() {
        val active = setOf(DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.PROCESSING)
        DownloadStatus.entries.forEach { assertEquals(it.name, it in active, item(it).isActive) }
    }

    @Test fun `only queued and running can be paused`() {
        assertTrue(item(DownloadStatus.QUEUED).isPausable)
        assertTrue(item(DownloadStatus.RUNNING).isPausable)
        assertFalse(item(DownloadStatus.PROCESSING).isPausable)   // saving is left to finish
        assertFalse(item(DownloadStatus.PAUSED).isPausable)
        assertFalse(item(DownloadStatus.DONE).isPausable)
        assertFalse(item(DownloadStatus.FAILED).isPausable)
    }

    @Test fun `history written by an older version still loads`() {
        // Same settings as DownloadRepository: an old file lacks newer fields and may carry unknown ones.
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val old = """[{"id":"1","url":"https://example.com/v","title":"T","thumbnail":null,"platform":"Web",
            "optionKey":"best","optionLabel":"Best quality","selector":"b","audioFormat":null,"createdAt":1,
            "someFutureField":42}]"""
        val items = json.decodeFromString(ListSerializer(DownloadItem.serializer()), old)
        assertEquals(1, items.size)
        assertEquals(DownloadStatus.QUEUED, items[0].status)
        assertEquals(0f, items[0].progress)
        assertNull(items[0].folder)
        assertNull(items[0].batchId)
    }

    @Test fun `a saved item survives a round trip`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val serializer = ListSerializer(DownloadItem.serializer())
        val original = item(DownloadStatus.PAUSED).copy(progress = 0.4f, folder = "Playlist", batchId = "b1", customName = "01 - T")
        val back = json.decodeFromString(serializer, json.encodeToString(serializer, listOf(original)))
        assertEquals(listOf(original), back)
    }
}
