package com.sahedul.clipsave.core

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberingTest {

    @Test fun `ascending numbers from one, zero-padded to the playlist size`() {
        assertEquals("01 - Title", Numbering.ASCENDING.fileName("Title", 0, 12))
        assertEquals("07 - Title", Numbering.ASCENDING.fileName("Title", 6, 12))
        assertEquals("12 - Title", Numbering.ASCENDING.fileName("Title", 11, 12))
    }

    @Test fun `descending counts down from the total`() {
        assertEquals("12 - Title", Numbering.DESCENDING.fileName("Title", 0, 12))
        assertEquals("01 - Title", Numbering.DESCENDING.fileName("Title", 11, 12))
    }

    @Test fun `padding width follows the total so names sort in playlist order`() {
        assertEquals("1 - T", Numbering.ASCENDING.fileName("T", 0, 9))
        assertEquals("001 - T", Numbering.ASCENDING.fileName("T", 0, 100))
        assertEquals("100 - T", Numbering.DESCENDING.fileName("T", 0, 100))
    }

    @Test fun `none leaves the title alone`() {
        assertEquals("Title", Numbering.NONE.fileName("Title", 3, 12))
    }

    @Test fun `saved keys map back, defaulting to ascending`() {
        assertEquals(Numbering.DESCENDING, Numbering.of("desc"))
        assertEquals(Numbering.NONE, Numbering.of("none"))
        assertEquals(Numbering.ASCENDING, Numbering.of("asc"))
        assertEquals(Numbering.ASCENDING, Numbering.of(null))
        assertEquals(Numbering.ASCENDING, Numbering.of("junk"))
    }
}
