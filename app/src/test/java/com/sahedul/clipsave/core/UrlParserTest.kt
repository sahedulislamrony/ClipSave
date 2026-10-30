package com.sahedul.clipsave.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlParserTest {

    @Test fun `no link in null, blank or plain text`() {
        assertNull(UrlParser.firstUrl(null))
        assertNull(UrlParser.firstUrl("   "))
        assertNull(UrlParser.firstUrl("just some words, no link"))
    }

    @Test fun `pulls the link out of a share sentence`() {
        assertEquals(
            "https://www.instagram.com/reel/ABC123/",
            UrlParser.firstUrl("Look at this https://www.instagram.com/reel/ABC123/ via @someone"),
        )
    }

    @Test fun `drops punctuation that trails a pasted link`() {
        assertEquals("https://youtu.be/abc123", UrlParser.firstUrl("see https://youtu.be/abc123."))
        assertEquals("https://example.com/video", UrlParser.firstUrl("(https://example.com/video)"))
        assertEquals("https://youtu.be/abc123", UrlParser.firstUrl("really? https://youtu.be/abc123!!"))
    }

    @Test fun `takes the first of several links`() {
        assertEquals(
            "https://vimeo.com/111111",
            UrlParser.firstUrl("https://vimeo.com/111111 and https://vimeo.com/222222"),
        )
    }

    @Test fun `scheme is matched case-insensitively and links that are too short are ignored`() {
        assertEquals("HTTPS://EXAMPLE.COM/V", UrlParser.firstUrl("HTTPS://EXAMPLE.COM/V"))
        assertNull(UrlParser.firstUrl("http://x"))
    }

    @Test fun `recognises YouTube playlist links`() {
        assertTrue(UrlParser.isYoutubePlaylist("https://www.youtube.com/playlist?list=PL123"))
        assertTrue(UrlParser.isYoutubePlaylist("https://www.youtube.com/watch?v=abc&list=PL123"))
        assertTrue(UrlParser.isYoutubePlaylist("https://youtu.be/abc?list=PL123"))
    }

    @Test fun `plain videos and other sites are not playlists`() {
        assertFalse(UrlParser.isYoutubePlaylist("https://www.youtube.com/watch?v=abc"))
        assertFalse(UrlParser.isYoutubePlaylist("https://www.youtube.com/watch?v=abc&list="))
        assertFalse(UrlParser.isYoutubePlaylist("https://vimeo.com/123?list=PL123"))
    }

    @Test fun `video inside a playlist link is offered on its own`() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc",
            UrlParser.youtubeVideoInPlaylist("https://www.youtube.com/watch?v=abc&list=PL123"),
        )
        assertEquals(
            "https://www.youtube.com/watch?v=abc",
            UrlParser.youtubeVideoInPlaylist("https://youtu.be/abc?list=PL123"),
        )
    }

    @Test fun `a bare playlist link has no single video`() {
        assertNull(UrlParser.youtubeVideoInPlaylist("https://www.youtube.com/playlist?list=PL123"))
    }

    @Test fun `hostOf drops www and m prefixes and lowercases`() {
        assertEquals("youtube.com", hostOf("https://www.youtube.com/x"))
        assertEquals("facebook.com", hostOf("https://m.facebook.com/x"))
        assertEquals("example.com", hostOf("https://WWW.Example.COM/Path"))
        assertNull(hostOf("not a url"))
    }

    @Test fun `youtube video id from every link shape`() {
        val id = "dQw4w9WgXcQ"
        assertEquals(id, UrlParser.youtubeVideoId("https://www.youtube.com/watch?v=$id&t=42"))
        assertEquals(id, UrlParser.youtubeVideoId("https://m.youtube.com/watch?feature=share&v=$id"))
        assertEquals(id, UrlParser.youtubeVideoId("https://youtu.be/$id?si=abc"))
        assertEquals(id, UrlParser.youtubeVideoId("https://youtube.com/shorts/$id"))
        assertEquals(id, UrlParser.youtubeVideoId("https://www.youtube.com/live/$id?feature=shared"))
        assertEquals(id, UrlParser.youtubeVideoId("https://www.youtube-nocookie.com/embed/$id"))
    }

    @Test fun `no youtube video id for channels, playlists, bad ids or other sites`() {
        assertNull(UrlParser.youtubeVideoId("https://www.youtube.com/@channel"))
        assertNull(UrlParser.youtubeVideoId("https://www.youtube.com/playlist?list=PL123"))
        assertNull(UrlParser.youtubeVideoId("https://youtu.be/short"))
        assertNull(UrlParser.youtubeVideoId("https://vimeo.com/watch?v=dQw4w9WgXcQ"))
    }
}
