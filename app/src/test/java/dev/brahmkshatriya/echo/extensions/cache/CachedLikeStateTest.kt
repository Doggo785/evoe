package dev.brahmkshatriya.echo.extensions.cache

import com.mayakapps.kache.FileKache
import com.mayakapps.kache.KacheStrategy
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.extensions.cache.Cached.getData
import dev.brahmkshatriya.echo.extensions.cache.Cached.putData
import dev.brahmkshatriya.echo.extensions.cache.Cached.updateLikeState
import dev.brahmkshatriya.echo.extensions.testLoadedState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * FIX 2026-10-04 stale player heart: a player-path like must rewrite the durable
 * media-state entry, otherwise playback keeps serving the pre-like value. Runs
 * against a real FileKache in a temp dir — no Android needed.
 */
class CachedLikeStateTest {

    private suspend fun newCache(): FileKache {
        val dir = Files.createTempDirectory("cached-like-state").toFile()
        return FileKache(dir.toString(), MAX_CACHE_BYTES) {
            strategy = KacheStrategy.LRU
        }
    }

    private fun stored(liked: Boolean?): Pair<String, MediaState.Loaded<Track>> {
        val id = "media-$EXTENSION_ID-$TRACK_ID-state"
        return id to testLoadedState(liked, EXTENSION_ID, TRACK_ID, TRACK_TITLE)
    }

    private suspend fun roundTrip(
        cache: FileKache, initial: Boolean?, write: Boolean,
    ): Pair<MediaState.Loaded<Track>, MediaState.Loaded<Track>> = with(Cached) {
        val (id, before) = stored(initial)
        cache.putData(id, before)
        updateLikeState(cache, EXTENSION_ID, TRACK_ID, write)
        before to cache.getData<MediaState.Loaded<Track>>(id).getOrThrow()
    }

    @Test
    fun `like write flips the cached isLiked and keeps the rest`() = runBlocking {
        val (before, after) = roundTrip(newCache(), initial = false, write = true)
        assertTrue(after.isLiked == true)
        assertEquals(before.item, after.item)
        assertEquals(before.isSaved, after.isSaved)
        assertEquals(before.extensionId, after.extensionId)
    }

    @Test
    fun `missing entry is a silent no-op`() = runBlocking {
        with(Cached) { updateLikeState(newCache(), EXTENSION_ID, "unknown", true) }
    }

    @Test
    fun `unlike write flips the cached isLiked back`() = runBlocking {
        val (_, after) = roundTrip(newCache(), initial = true, write = false)
        assertTrue(after.isLiked == false)
    }

    @Test
    fun `absent isLiked takes the written value`() = runBlocking {
        val (_, after) = roundTrip(newCache(), initial = null, write = true)
        assertTrue(after.isLiked == true)
    }

    companion object {
        private const val EXTENSION_ID = "deezer"
        private const val TRACK_ID = "42"
        private const val TRACK_TITLE = "Probe"
        private const val MAX_CACHE_BYTES = 1048576L
    }
}
