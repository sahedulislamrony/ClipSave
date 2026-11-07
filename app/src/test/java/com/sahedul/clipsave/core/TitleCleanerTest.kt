package com.sahedul.clipsave.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleCleanerTest {
    private val fb = "https://www.facebook.com/watch/?v=1"
    private val ig = "https://www.instagram.com/reel/abc/"

    @Test fun `facebook counters are dropped`() {
        assertEquals("Sunset at the beach", TitleCleaner.name("12K views · 345 reactions | Sunset at the beach | Travel Page", fb))
        assertEquals("Sunset at the beach", TitleCleaner.name("1.2M views · 3K reactions · 120 comments | Sunset at the beach", fb))
        assertEquals("Sunset at the beach", TitleCleaner.name("345 reactions · 12 comments · Sunset at the beach", fb))
    }

    private val stamped = Regex("""^%s video \d{4}-\d{2}-\d{2} \d{2}\.\d{2}\.\d{2}$""")

    @Test fun `facebook with only counters falls back to a timestamped name`() {
        assertTrue(stamped.pattern.format("Facebook").toRegex().matches(TitleCleaner.name("12K views · 345 reactions", fb)))
        assertTrue(stamped.pattern.format("Facebook").toRegex().matches(TitleCleaner.name(null, fb)))
        assertTrue(stamped.pattern.format("Facebook").toRegex().matches(TitleCleaner.name("", fb)))
    }

    @Test fun `generic instagram titles fall back`() {
        val re = stamped.pattern.format("Instagram").toRegex()
        assertTrue(re.matches(TitleCleaner.name("Video by someone", ig)))
        assertTrue(re.matches(TitleCleaner.name("Instagram", ig)))
        assertTrue(re.matches(TitleCleaner.name("1234567890", ig)))
    }

    @Test fun `fallback names the uploader when known`() {
        assertEquals("Video by sabbirahmed58375", TitleCleaner.name("Video by sabbirahmed58375", ig, uploader = "sabbirahmed58375"))
        assertEquals("Video by someone", TitleCleaner.fallback(ig, " someone "))
    }

    @Test fun `fallback without an uploader differs by time`() {
        assertNotEquals(TitleCleaner.fallback(ig, null, 1_000_000_000_000), TitleCleaner.fallback(ig, null, 1_000_000_001_000))
    }

    @Test fun `instagram caption wrapper is removed`() {
        assertEquals("Morning run", TitleCleaner.name("someone on Instagram: \"Morning run\"", ig))
    }

    @Test fun `long captions keep their first words`() {
        val caption = "We spent the whole weekend building this treehouse for the kids and it turned out amazing #diy #family"
        val name = TitleCleaner.name(caption, ig)
        assertEquals("We spent the whole weekend building this treehouse for the", name)
        assertEquals(true, name.length <= TitleCleaner.MAX_CHARS)
    }

    @Test fun `links and trailing hashtags are removed`() {
        assertEquals("Look at this", TitleCleaner.name("Look at this https://t.co/xyz #wow #cool", "https://x.com/a/status/1"))
    }

    @Test fun `youtube titles are untouched`() {
        val title = "A very long YouTube video title that goes well past sixty characters on purpose"
        assertEquals(title, TitleCleaner.name(title, "https://www.youtube.com/watch?v=abc"))
    }

    @Test fun `other sites fall back to their host`() {
        assertNull(TitleCleaner.clean("Video", Platform.OTHER))
        assertTrue(Regex("""^example\.com video \d{4}-.*""").matches(TitleCleaner.name(null, "https://www.example.com/v/1")))
    }
}
