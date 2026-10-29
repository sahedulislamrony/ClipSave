package com.sahedul.clipsave.download

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizeFileNameTest {

    @Test fun `removes characters Android and FAT reject`() {
        assertEquals("abcdefghi", sanitizeFileName("a/b:c*d?e\"f<g>h|i"))
        assertEquals("ab", sanitizeFileName("a\\b"))
    }

    @Test fun `removes control characters`() {
        assertEquals("abc", sanitizeFileName("a\u0000b\nc"))
    }

    @Test fun `trims spaces and leading or trailing dots`() {
        assertEquals("hello", sanitizeFileName("  hello.  "))
        assertEquals("hidden", sanitizeFileName("..hidden"))
    }

    @Test fun `nothing left becomes empty`() {
        assertEquals("", sanitizeFileName(""))
        assertEquals("", sanitizeFileName("///"))
        assertEquals("", sanitizeFileName(" . "))
    }

    @Test fun `names are capped at 100 characters without a trailing space`() {
        assertEquals(100, sanitizeFileName("a".repeat(150)).length)
        assertEquals("a".repeat(99), sanitizeFileName("a".repeat(99) + " b".repeat(5)))
    }

    @Test fun `keeps ordinary titles, accents and emoji`() {
        assertEquals("Café – Naïve 🎬 [HD]", sanitizeFileName("Café – Naïve 🎬 [HD]"))
    }
}
