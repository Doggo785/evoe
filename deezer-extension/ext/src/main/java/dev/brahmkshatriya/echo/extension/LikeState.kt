package dev.brahmkshatriya.echo.extension

/**
 * Generation-guarded memory of liked track ids.
 *
 * Pure state machine, no network and no Android: the playback path must never
 * wait for favorite_song.getList (measured 5.8s), so every read here is
 * synchronous and every write carries a generation that stale async work
 * (verification, rollback, prefetch sync) must present to take effect.
 *
 * Read order: pending user action, then memory, then the inline `loved`
 * flag, then unknown (null). Absence from memory means "not liked" ONLY
 * when [fullyLoaded] is true; a truncated fetch (see LIKES_FETCH_LIMIT in
 * DeezerTrack.getTracks) leaves it false and absence means unknown.
 *
 * Pending entries exit ONLY via [confirm] (server accepted) or [rollback]
 * (server refused): a failed tap must never be re-imposed by a later sync.
 */
class LikeState {

    private var generation = 0L
    private var likedIds: HashSet<String>? = null
    var fullyLoaded: Boolean = false
        private set
    private val pendingLiked = HashMap<String, Long>()
    private val pendingUnliked = HashMap<String, Long>()

    val currentGeneration: Long
        @Synchronized get() = generation

    @Synchronized
    fun isPending(id: String): Boolean = pendingLiked.containsKey(id) || pendingUnliked.containsKey(id)

    @Synchronized
    fun resolve(id: String, loved: Boolean?): Boolean? {
        val ids = likedIds
        return when {
            pendingLiked.containsKey(id) -> true
            pendingUnliked.containsKey(id) -> false
            ids == null -> loved
            id in ids -> true
            fullyLoaded -> false
            else -> loved
        }
    }

    @Synchronized
    fun userLike(id: String): Long {
        generation += 1
        likedIds?.add(id)
        pendingUnliked.remove(id)
        pendingLiked[id] = generation
        return generation
    }

    @Synchronized
    fun userUnlike(id: String): Long {
        generation += 1
        likedIds?.remove(id)
        pendingLiked.remove(id)
        pendingUnliked[id] = generation
        return generation
    }

    @Synchronized
    fun confirm(id: String, liked: Boolean, gen: Long) {
        val pending = if (liked) pendingLiked else pendingUnliked
        if (pending[id] == gen) pending.remove(id)
    }

    @Synchronized
    fun rollback(id: String, liked: Boolean, gen: Long): Boolean? {
        val pending = if (liked) pendingLiked else pendingUnliked
        if (pending[id] != gen) return null
        pending.remove(id)
        return if (liked) {
            likedIds?.remove(id)
            false
        } else {
            likedIds?.add(id)
            true
        }
    }

    @Synchronized
    fun merge(ids: Set<String>, complete: Boolean) {
        val fresh = HashSet(ids)
        pendingLiked.keys.forEach { fresh.add(it) }
        pendingUnliked.keys.forEach { fresh.remove(it) }
        likedIds = fresh
        fullyLoaded = complete
    }

    @Synchronized
    fun reset() {
        generation += 1
        likedIds = null
        fullyLoaded = false
        pendingLiked.clear()
        pendingUnliked.clear()
    }
}
