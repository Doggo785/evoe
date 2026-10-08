package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.extension.api.DeezerTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Single parse of a favorite_song.getList response feeding every likes consumer
 * (isItemLiked ids, artist menu entries, favorites playlist) from one fetch.
 * Pure, no network.
 */
class LikedSnapshotTest {

    private fun entry(id: String) = """{"SNG_ID":"$id","SNG_TITLE":"T"}"""

    private fun results(vararg entries: String, total: Int? = null): JsonObject =
        buildJsonObject {
            total?.let { put("total", it) }
            putJsonArray("data") {
                entries.forEach { add(Json.parseToJsonElement(it)) }
            }
        }

    @Test
    fun `missing results or data is null`() {
        assertNull(parseLikedResults(null))
        assertNull(parseLikedResults(buildJsonObject { }))
    }

    @Test
    fun `entries and ids come from the same payload`() {
        val snapshot = parseLikedResults(results(entry("1"), entry("2"), total = 2))!!
        assertEquals(2, snapshot.entries.size)
        assertEquals(setOf("1", "2"), snapshot.ids)
        assertTrue(snapshot.complete)
    }

    @Test
    fun `short of total is incomplete`() {
        val snapshot = parseLikedResults(results(entry("1"), total = 2))!!
        assertFalse(snapshot.complete)
    }

    @Test
    fun `no total and small page is complete`() {
        val snapshot = parseLikedResults(results(entry("1"), entry("2")))!!
        assertTrue(snapshot.complete)
    }

    @Test
    fun `no total and full page is probably truncated`() {
        val full = buildJsonObject {
            putJsonArray("data") {
                repeat(DeezerTrack.LIKES_FETCH_LIMIT) { add(Json.parseToJsonElement(entry("$it"))) }
            }
        }
        assertFalse(parseLikedResults(full)!!.complete)
    }

    @Test
    fun `entries without id stay visible but add no id`() {
        val snapshot = parseLikedResults(results("""{"SNG_TITLE":"T"}""", entry("1"), total = 2))!!
        assertEquals(2, snapshot.entries.size)
        assertEquals(setOf("1"), snapshot.ids)
    }
}
