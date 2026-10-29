package com.sahedul.clipsave.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistPagingTest {

    private fun entries(range: IntRange, duration: Long? = 60) =
        range.map { PlaylistEntry("https://youtu.be/$it", "Video $it", null, index = it, durationSec = duration) }

    private fun playlist(range: IntRange, total: Int?, nextStart: Int?) = PlaylistInfo(
        url = "https://www.youtube.com/playlist?list=PL1",
        title = "List",
        uploader = null,
        thumbnail = null,
        platform = Platform.YOUTUBE,
        entries = entries(range),
        videoUrl = null,
        total = total,
        nextStart = nextStart,
    )

    @Test fun `next page is a full hundred while plenty are left`() {
        assertEquals(100, playlist(0..99, total = 2791, nextStart = 101).nextPageSize)
    }

    @Test fun `last page offers only the remainder`() {
        assertEquals(42, playlist(0..199, total = 242, nextStart = 201).nextPageSize)
    }

    @Test fun `unknown length still pages by a hundred`() {
        assertEquals(100, playlist(0..99, total = null, nextStart = 101).nextPageSize)
    }

    @Test fun `nothing left means no next page`() {
        assertNull(playlist(0..41, total = 42, nextStart = null).nextPageSize)
    }

    @Test fun `adding a page appends, keeps order and drops repeats`() {
        val merged = playlist(0..99, total = 250, nextStart = 101).plus(playlist(95..199, total = 250, nextStart = 201))
        assertEquals((0..199).toList(), merged.entries.map { it.index })
        assertEquals(201, merged.nextStart)
    }

    @Test fun `files are numbered against the whole playlist`() {
        assertEquals(2791, playlist(0..99, total = 2791, nextStart = 101).numberingTotal)
        // Without a reported length, the furthest video read sets the width.
        assertEquals(200, playlist(0..199, total = null, nextStart = 201).numberingTotal)
        assertEquals("0150 - T", Numbering.ASCENDING.fileName("T", 149, 2791))
    }

    @Test fun `size estimate scales with duration and quality`() {
        val ten = entries(0..9, duration = 600) // ten 10-minute videos
        val mb = 1_048_576.0
        assertEquals(2145.8, FormatOption.estimateBytes(ten, FormatOption.video(1080, null)) / mb, 1.0)
        assertEquals(1216.0, FormatOption.estimateBytes(ten, FormatOption.video(720, null)) / mb, 1.0)
        assertEquals(175.2, FormatOption.estimateBytes(ten, FormatOption.audio("mp3", null)) / mb, 1.0)
        assertEquals(0L, FormatOption.estimateBytes(entries(0..3, duration = null), FormatOption.video(720, null)))
    }
}
