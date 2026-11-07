package com.sahedul.clipsave.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitleCleanerTest {
    private val fb = "https://www.facebook.com/watch/?v=1"
    private val ig = "https://www.instagram.com/reel/abc/"

    @Test fun `facebook counters are dropped`() {
        assertEquals("Sunset at the beach", TitleCleaner.name("12K views · 345 reactions | Sunset at the beach | Travel Page", fb))
        assertEquals("Sunset at the beach", TitleCleaner.name("1.2M views · 3K reactions · 120 comments | Sunset at the beach", fb))
        assertEquals("Sunset at the beach", TitleCleaner.name("345 reactions · 12 comments · Sunset at the beach", fb))
    }

    @Test fun `facebook with only counters falls back`() {
        assertEquals("Facebook video", TitleCleaner.name("12K views · 345 reactions", fb))
        assertEquals("Facebook video", TitleCleaner.name(null, fb))
        assertEquals("Facebook video", TitleCleaner.name("", fb))
    }

    @Test fun `generic instagram titles fall back`() {
        assertEquals("Instagram video", TitleCleaner.name("Video by someone", ig))
        assertEquals("Instagram video", TitleCleaner.name("Instagram", ig))
        assertEquals("Instagram video", TitleCleaner.name("1234567890", ig))
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
        assertEquals("example.com video", TitleCleaner.name(null, "https://www.example.com/v/1"))
    }
}
