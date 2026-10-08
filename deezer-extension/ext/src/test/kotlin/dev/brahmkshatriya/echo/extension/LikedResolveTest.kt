package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.Track
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure merge for the session cache of fully-resolved liked tracks. Network-free:
 * the chunked song.getListData fetch lives in DeezerExtension and is covered on
 * device, this covers ordering, delta and eviction.
 */
class LikedResolveTest {

    private fun track(id: String) = Track(id = id, title = id)

    @Test
    fun `missing ids keep likes order`() {
        assertEquals(
            listOf("b", "c"),
            idsToResolve(listOf("a", "b", "c"), mapOf("a" to track("a")))
        )
    }

    @Test
    fun `nothing missing resolves nothing`() {
        assertEquals(
            emptyList<String>(),
            idsToResolve(listOf("a"), mapOf("a" to track("a")))
        )
    }

    @Test
    fun `merge keeps still-liked cached tracks and adds fresh ones`() {
        val merged = mergeFullCache(
            cached = mapOf("a" to track("a"), "gone" to track("gone")),
            likedIds = setOf("a", "b"),
            fresh = mapOf("b" to track("b"))
        )
        assertEquals(setOf("a", "b"), merged.keys)
    }

    @Test
    fun `merge drops unliked tracks even without fresh data`() {
        val merged = mergeFullCache(
            cached = mapOf("a" to track("a"), "gone" to track("gone")),
            likedIds = setOf("a"),
            fresh = emptyMap()
        )
        assertEquals(setOf("a"), merged.keys)
    }

    @Test
    fun `merge evicts nothing when the snapshot was truncated`() {
        val merged = mergeFullCache(
            cached = mapOf("a" to track("a"), "maybe" to track("maybe")),
            likedIds = setOf("a"),
            fresh = emptyMap(),
            complete = false
        )
        assertEquals(setOf("a", "maybe"), merged.keys)
    }
}
