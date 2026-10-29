package com.sahed.clipsave.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PlatformTest {

    @Test fun `detects each supported site`() {
        assertEquals(Platform.YOUTUBE, Platform.detect("https://www.youtube.com/watch?v=abc"))
        assertEquals(Platform.YOUTUBE, Platform.detect("https://youtu.be/abc"))
        assertEquals(Platform.INSTAGRAM, Platform.detect("https://www.instagram.com/reel/abc/"))
        assertEquals(Platform.FACEBOOK, Platform.detect("https://fb.watch/abc/"))
        assertEquals(Platform.TIKTOK, Platform.detect("https://www.tiktok.com/@u/video/1"))
        assertEquals(Platform.TWITTER, Platform.detect("https://x.com/u/status/1"))
        assertEquals(Platform.TWITTER, Platform.detect("https://t.co/abc"))
        assertEquals(Platform.VIMEO, Platform.detect("https://vimeo.com/123"))
        assertEquals(Platform.REDDIT, Platform.detect("https://redd.it/abc"))
    }

    @Test fun `subdomains belong to their site`() {
        assertEquals(Platform.YOUTUBE, Platform.detect("https://music.youtube.com/watch?v=abc"))
        assertEquals(Platform.FACEBOOK, Platform.detect("https://m.facebook.com/video/1"))
    }

    @Test fun `look-alike hosts are not matched`() {
        assertEquals(Platform.OTHER, Platform.detect("https://notyoutube.com/watch?v=abc"))
        assertEquals(Platform.OTHER, Platform.detect("https://youtube.com.evil.example/x"))
    }

    @Test fun `unknown sites and bad input fall back to web`() {
        assertEquals(Platform.OTHER, Platform.detect("https://example.org/video"))
        assertEquals(Platform.OTHER, Platform.detect("not a url"))
    }
}
