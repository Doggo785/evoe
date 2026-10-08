package dev.brahmkshatriya.echo.extensions.builtin.offline

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Per-artist liked predicate. No Android here so it runs as a plain JVM test.
 * The feat parsing it builds on is covered in :common (FeatNamesTest).
 */
class FeatNamesTest {

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
