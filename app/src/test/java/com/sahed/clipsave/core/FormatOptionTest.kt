package com.sahed.clipsave.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatOptionTest {

    @Test fun `best quality has no height cap`() {
        val best = FormatOption.video(null, null)
        assertEquals("best", best.key)
        assertEquals("Best quality", best.label)
        assertEquals("bv*+ba/b", best.selector)
        assertNull(best.height)
        assertFalse(best.isAudio)
    }

    @Test fun `video option caps the height in the yt-dlp selector`() {
        val p1080 = FormatOption.video(1080, 5_000_000)
        assertEquals("1080", p1080.key)
        assertEquals("1080p", p1080.label)
        assertEquals("bv*[height<=1080]+ba/b[height<=1080]/b", p1080.selector)
        assertEquals(1080, p1080.height)
        assertEquals(5_000_000L, p1080.sizeBytes)
    }

    @Test fun `audio options extract with ffmpeg`() {
        val mp3 = FormatOption.audio("mp3", null)
        assertTrue(mp3.isAudio)
        assertEquals("MP3 audio", mp3.label)
        assertEquals("mp3", mp3.audioFormat)
        assertEquals("ba/b", mp3.selector)
        assertEquals("M4A audio", FormatOption.audio("m4a", null).label)
    }

    @Test fun `playlists offer fixed quality caps`() {
        assertEquals(listOf("best", "1080", "720", "480", "mp3", "m4a"), FormatOption.playlist().map { it.key })
    }

    @Test fun `pick finds the saved default by key`() {
        val options = FormatOption.playlist()
        assertEquals("1080", FormatOption.pick(options, "1080")?.key)
        assertEquals("mp3", FormatOption.pick(options, "mp3")?.key)
        assertEquals("best", FormatOption.pick(options, "best")?.key)
    }

    @Test fun `pick falls back to the closest lower quality`() {
        val options = listOf(FormatOption.video(720, null), FormatOption.video(480, null), FormatOption.audio("mp3", null))
        assertEquals("720", FormatOption.pick(options, "1080")?.key)
        assertEquals("480", FormatOption.pick(options, "480")?.key)
    }

    @Test fun `pick uses the first video when nothing is low enough or the key is unknown`() {
        val options = listOf(FormatOption.video(720, null), FormatOption.video(480, null), FormatOption.audio("mp3", null))
        assertEquals("720", FormatOption.pick(options, "360")?.key)
        assertEquals("720", FormatOption.pick(options, "weird")?.key)
    }

    @Test fun `pick treats best-only videos as any height and returns null with no video`() {
        val bestOnly = listOf(FormatOption.video(null, null), FormatOption.audio("mp3", null))
        assertEquals("best", FormatOption.pick(bestOnly, "1080")?.key)
        assertNull(FormatOption.pick(listOf(FormatOption.audio("mp3", null)), "1080"))
    }
}
