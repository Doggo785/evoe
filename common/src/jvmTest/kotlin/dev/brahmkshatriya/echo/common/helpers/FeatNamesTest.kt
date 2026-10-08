package dev.brahmkshatriya.echo.common.helpers

import kotlin.test.Test
import kotlin.test.assertEquals

class FeatNamesTest {

    @Test
    fun `feat names from parenthesized marker`() {
        assertEquals(listOf("B"), featNamesFromTitle("Song (feat. B)"))
    }

    @Test
    fun `feat names without parens`() {
        assertEquals(listOf("B"), featNamesFromTitle("Song feat. B"))
    }

    @Test
    fun `feat names support ft and featuring spellings`() {
        assertEquals(listOf("B"), featNamesFromTitle("Song (ft. B)"))
        assertEquals(listOf("B"), featNamesFromTitle("Song featuring B"))
    }

    @Test
    fun `feat names support several guests`() {
        assertEquals(listOf("B", "C", "D"), featNamesFromTitle("Song (feat. B, C & D)"))
    }

    @Test
    fun `feat marker matching keeps the original case`() {
        assertEquals(listOf("b"), featNamesFromTitle("Song (FEAT. b)"))
    }

    @Test
    fun `plain titles have no feat names`() {
        assertEquals(emptyList<String>(), featNamesFromTitle("Plain Song"))
        assertEquals(emptyList<String>(), featNamesFromTitle("A Feature Film Soundtrack"))
    }

    @Test
    fun `split artist field on feat marker`() {
        assertEquals(listOf("A", "B"), splitArtistField("A feat. B"))
        assertEquals(listOf("A", "B"), splitArtistField("A (feat. B)"))
        assertEquals(listOf("A", "B", "C"), splitArtistField("A & B feat. C"))
    }

    @Test
    fun `split artist field keeps comma and ampersand behavior`() {
        assertEquals(listOf("A", "B", "C"), splitArtistField("A, B & C"))
    }
}
