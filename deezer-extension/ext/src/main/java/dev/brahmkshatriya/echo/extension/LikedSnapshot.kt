package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.extension.api.DeezerTrack
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * One parsed favorite_song.getList response serving every likes consumer (isItemLiked
 * ids, per-artist menu entries, favorites playlist, library Tracks tab) from a single
 * fetch. [complete] is false when the page was probably truncated, in which case
 * absence of an id means "unknown", never "not liked".
 */
internal data class LikedSnapshot(
    val entries: List<JsonObject>,
    val ids: Set<String>,
    val complete: Boolean,
)

// Null when results/data are missing: callers degrade (empty shelf, unknown like),
// never throw over a broken payload.
internal fun parseLikedResults(results: JsonObject?): LikedSnapshot? {
    val data = results?.get("data")?.jsonArray ?: return null
    val entries = data.filterIsInstance<JsonObject>()
    val ids = entries.mapNotNullTo(HashSet()) { entry ->
        runCatching { entry["SNG_ID"]?.jsonPrimitive?.content }.getOrNull()
    }
    val total = listOf("total", "TOTAL", "count", "nb").firstNotNullOfOrNull { key ->
        (results[key] as? JsonPrimitive)?.content?.toIntOrNull()
    }
    val complete = total?.let { ids.size >= it } ?: (ids.size < DeezerTrack.LIKES_FETCH_LIMIT)
    return LikedSnapshot(entries, ids, complete)
}
