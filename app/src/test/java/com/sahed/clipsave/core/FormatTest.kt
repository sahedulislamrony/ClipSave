package com.sahed.clipsave.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FormatTest {
    private lateinit var saved: Locale

    @Before fun fixLocale() {
        saved = Locale.getDefault()
        Locale.setDefault(Locale.US)   // the helpers format with the default locale
    }

    @After fun restoreLocale() = Locale.setDefault(saved)

    @Test fun `size is hidden when unknown`() {
        assertNull(formatSize(null))
        assertNull(formatSize(0))
        assertNull(formatSize(-5))
    }

    @Test fun `size in megabytes and gigabytes`() {
        assertEquals("1.0 MB", formatSize(1_048_576))
        assertEquals("1.5 MB", formatSize(1_572_864))
        assertEquals("1.0 GB", formatSize(1_073_741_824))
    }

    @Test fun `duration is hidden when unknown`() {
        assertNull(formatDuration(null))
        assertNull(formatDuration(0))
    }

    @Test fun `duration as m ss or h mm ss`() {
        assertEquals("0:59", formatDuration(59))
        assertEquals("1:01", formatDuration(61))
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("1:02:05", formatDuration(3725))
    }
}
