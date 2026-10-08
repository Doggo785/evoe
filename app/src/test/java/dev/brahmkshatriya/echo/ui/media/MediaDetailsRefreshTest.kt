package dev.brahmkshatriya.echo.ui.media

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.media.MediaDetailsViewModel.Companion.shouldBustLikedCache
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pull-to-refresh on the virtual Favorite Tracks card must bust the shared likes
 * snapshot (its rows ARE the snapshot); ordinary playlists must not — busting there
 * would nuke the cache every unrelated refresh for no gain.
 */
class MediaDetailsRefreshTest {

    private fun playlist(vararg extras: Pair<String, String>) = Playlist(
        id = "1",
        title = "P",
        isEditable = false,
        extras = mapOf(*extras)
    )

    @Test
    fun `favorites card busts the likes snapshot`() {
        assertTrue(shouldBustLikedCache(playlist("favorites" to "1")))
    }

    @Test
    fun `ordinary playlist does not`() {
        assertFalse(shouldBustLikedCache(playlist()))
    }

    @Test
    fun `non playlists never do`() {
        assertFalse(shouldBustLikedCache(Artist(id = "1", name = "A")))
        assertFalse(shouldBustLikedCache(Track(id = "1", title = "T")))
    }
}
