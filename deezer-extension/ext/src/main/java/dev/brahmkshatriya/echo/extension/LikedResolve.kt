package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.Track

// Likes fetch tuning: song.getListData resolves full records (artists included)
// for a batch of ids. Chunks of 100 match the shipped wrapper; batches of chunks
// run concurrently, capped so a cold 10k-likes library stays a few seconds.
internal const val LIKED_RESOLVE_CHUNK = 100
internal const val LIKED_RESOLVE_PARALLELISM = 8

/**
 * Ids still needing a full resolve, in likes order. Pure, unit-tested.
 */
internal fun idsToResolve(likedIds: List<String>, cached: Map<String, Track>): List<String> =
    likedIds.filter { it !in cached }

/**
 * Merges freshly-resolved tracks into the session cache: still-liked cached tracks
 * survive, fresh ones are added, unliked ones are evicted — unless the snapshot was
 * truncated ([complete] false: absence means "unknown", so nothing is evicted).
 * Pure, unit-tested.
 */
internal fun mergeFullCache(
    cached: Map<String, Track>,
    likedIds: Set<String>,
    fresh: Map<String, Track>,
    complete: Boolean = true,
): Map<String, Track> =
    if (complete) cached.filterKeys { it in likedIds } + fresh
    else cached + fresh
