package dev.brahmkshatriya.echo.extensions.builtin.offline

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure feat. parsing shared by the offline artist split and the per-artist
 * liked filter. No Android here so it runs as a plain JVM test.
 */
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
    fun `feat matching is case-insensitive at the call sites`() {
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

    private fun likedTrack(title: String, vararg artistIds: String) = Track(
        id = title,
        title = title,
        artists = artistIds.map { Artist(id = it, name = it) }
    )

    @Test
    fun `liked by id match`() {
        assertTrue(isLikedByArtist(likedTrack("Song", "42"), Artist(id = "42", name = "A")))
    }

    @Test
    fun `liked by featuring title match`() {
        val track = likedTrack("Song (feat. A)", "7")
        assertTrue(isLikedByArtist(track, Artist(id = "42", name = "A")))
    }

    @Test
    fun `not liked when neither id nor featuring matches`() {
        val track = likedTrack("Plain Song", "7")
        assertFalse(isLikedByArtist(track, Artist(id = "42", name = "A")))
        assertFalse(isLikedByArtist(likedTrack("Song (feat. B)", "7"), Artist(id = "42", name = "A")))
    }
}
