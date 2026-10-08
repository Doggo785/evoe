package dev.brahmkshatriya.echo.extension.clients

import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.DeezerApi
import dev.brahmkshatriya.echo.extension.DeezerExtension
import dev.brahmkshatriya.echo.extension.DeezerParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class DeezerLibraryClient(
    private val deezerExtension: DeezerExtension,
    private val api: DeezerApi,
    private val parser: DeezerParser,
    private val cpuDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val tabs: List<Tab> = listOf(
        Tab(TabId.ALL.id, "All"),
        Tab(TabId.PLAYLISTS.id, "Playlists"),
        Tab(TabId.ALBUMS.id, "Albums"),
        Tab(TabId.TRACKS.id, "Tracks"),
        Tab(TabId.ARTISTS.id, "Artists"),
    )

    private data class TabConfig(
        val id: TabId,
        val title: String,
        val request: suspend DeezerApi.() -> JsonObject,
        val extractor: (JsonObject) -> JsonArray?
    )

    private enum class TabId(val id: String) {
        ALL("all"),
        PLAYLISTS("playlists"),
        ALBUMS("albums"),
        TRACKS("tracks"),
        ARTISTS("artists")
    }

    private val configs: Map<String, TabConfig> = listOf(
        TabConfig(TabId.PLAYLISTS, "Playlists", { getPlaylists() }) { it.tabDataArray("playlists") },
        TabConfig(TabId.ALBUMS, "Albums", { getAlbums() }) { it.tabDataArray("albums") },
        // Unreached at fetch time: loadAll/loadSingle read the shared likes snapshot
        // below (one favorite_song.getList per TTL for library, artist menu, favorites
        // playlist and isItemLiked together) instead of a dedicated call.
        TabConfig(TabId.TRACKS, "Tracks", { getTracks() }) { it.resultsDataArray() },
        TabConfig(TabId.ARTISTS, "Artists", { getArtists() }) { it.tabDataArray("artists") },
    ).associateBy { it.id.id }


    suspend fun loadLibraryFeed(): Feed<Shelf> {
        deezerExtension.handleArlExpiration()
        return Feed(tabs) { tab ->
            val id = tab?.id
            val data = when (id) {
                TabId.ALL.id -> loadAll()
                else -> loadSingle(id)
            }
            val buttons = if (id == TabId.TRACKS.id) Feed.Buttons(showPlayAndShuffle = true)
            else Feed.Buttons()
            data.toFeedData(buttons)
        }
    }

    private suspend fun loadAll(): List<Shelf> = supervisorScope {
        deezerExtension.handleArlExpiration()
        configs.values.map { cfg ->
            async(cpuDispatcher) {
                if (cfg.id == TabId.TRACKS) {
                    // Broken payload degrades to a missing shelf (what the null extractor
                    // did); network errors still fail the tab, as before.
                    val entries = try {
                        deezerExtension.getLikedEntriesCached()
                    } catch (_: IllegalStateException) {
                        return@async null
                    }
                    val grafted = entries.map { graftFavTrack(it) }
                    return@async grafted.takeIf { it.isNotEmpty() }
                        ?.let { Shelf.Lists.Items(id = cfg.title, title = cfg.title, list = it) }
                }
                val json = cfg.request(api)
                val items = cfg.extractor(json) ?: return@async null
                if (cfg.id == TabId.PLAYLISTS) {
                    val shelf = parser.run { items.toShelfItemsList(cfg.title) }
                    withLovedCard(cfg.title, shelf, resolveLovedPlaylist())
                } else parser.run { items.toShelfItemsList(cfg.title) }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun loadSingle(id: String?): List<Shelf> {
        if (id == TabId.TRACKS.id) {
            // Same contract as the null extractor: broken payload is an empty shelf,
            // network errors still throw.
            val entries = try {
                deezerExtension.getLikedEntriesCached()
            } catch (_: IllegalStateException) {
                return emptyList()
            }
            return entries.map { graftFavTrack(it).toShelf() }
        }
        val cfg = configs[id] ?: return emptyList()
        deezerExtension.handleArlExpiration()
        val json = cfg.request(api)
        val arr = cfg.extractor(json) ?: return emptyList()
        val items = parser.run { arr.mapNotNull { it.jsonObject.toEchoMediaItem()?.toShelf() } }
        if (id == TabId.PLAYLISTS.id) return prependLovedCard(items, resolveLovedPlaylist())
        return items
    }

    private fun JsonObject.results(): JsonObject? = this["results"]?.jsonObject
    private fun JsonObject.resultsDataArray(): JsonArray? =
        results()?.get("data")?.jsonArray
    private fun JsonObject.tabDataArray(tabId: String): JsonArray? =
        results()?.get("TAB")?.jsonObject?.get(tabId)?.jsonObject?.get("data")?.jsonArray

    // FALLBACK-graft for favorites/liked TRACKS — now DeezerParser.graftFavTrack (shared
    // with DeezerPlaylistClient's virtual Favorite Tracks playlist). Kept as a one-line
    // delegate rather than inlining the call sites so both shelves keep reading identically.
    private fun graftFavTrack(entry: JsonObject): Track = parser.graftFavTrack(entry)

    // The real loved-tracks playlist, resolved from Home's Recently played where Deezer
    // actually sends it (the library payload never carries it — verified on device
    // 2026-10-08). A failed or missing resolve degrades to hidden, never to a
    // synthesized card (user choice): library stays truthful with no placeholder.
    // Cancellation is rethrown — a generic catch here would launder it into "hidden".
    private suspend fun resolveLovedPlaylist(): Playlist? {
        deezerExtension.handleArlExpiration()
        val home = homePageOrNull()
        val loved = home?.let { runCatching { parser.findLovedPlaylist(it) }.getOrNull() }
        return loved?.let { item ->
            item.copy(extras = item.extras + mapOf(DeezerPlaylistClient.FAVORITES_EXTRA to "1"))
        }
    }

    private suspend fun homePageOrNull(): JsonObject? {
        return try {
            api.page("home")
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        // LEGACY synthetic "Favorite Tracks" card: kept for already-cached items carrying
        // the FAVORITES_EXTRA routing key. New loads resolve the real loved playlist from
        // Home instead (resolveLovedPlaylist) and never synthesize. The id stays non-numeric
        // so it can never collide with a real playlist id. Opening the card yields a Playlist
        // context, so taps play the likes in order via the existing ordered-collection path —
        // no tap-logic change needed.
        const val FAVORITES_ID = "favorites"
        const val FAVORITES_TITLE = "Favorite Tracks"

        fun favoritesCard(): Playlist = Playlist(
            id = FAVORITES_ID,
            title = FAVORITES_TITLE,
            isEditable = false,
            // Not a real playlist: nothing to save/follow/share, and radio has no id
            // Deezer could resolve. Ordered playback (the card's whole job) needs none.
            isRadioSupported = false,
            isFollowable = false,
            isSaveable = false,
            isShareable = false,
            extras = mapOf(DeezerPlaylistClient.FAVORITES_EXTRA to "1")
        )

        // Head-of-shelf placement for both Playlists surfaces. Internal for tests;
        // loadAll (All tab carousel) and prependCardToPlaylists (Playlists tab rows)
        // are the only callers.
        internal fun withFavoritesCard(title: String, shelf: Shelf?): Shelf? {
            val card = favoritesCard()
            val list = (shelf as? Shelf.Lists.Items)?.list.orEmpty()
            return Shelf.Lists.Items(id = title, title = title, list = listOf(card) + list)
        }

        internal fun prependCardToPlaylists(items: List<Shelf>): List<Shelf> =
            listOf(favoritesCard().toShelf()) + items

        // Real-card placement for both Playlists surfaces: the loved playlist heads the
        // shelf when Deezer sends it and the shelf is left untouched otherwise (hidden,
        // never synthesized). Internal for tests; loadAll and loadSingle are the callers.
        internal fun withLovedCard(title: String, shelf: Shelf?, loved: Playlist?): Shelf? {
            if (loved == null) return shelf
            val list = (shelf as? Shelf.Lists.Items)?.list.orEmpty()
            return Shelf.Lists.Items(id = title, title = title, list = listOf(loved) + list)
        }

        internal fun prependLovedCard(items: List<Shelf>, loved: Playlist?): List<Shelf> =
            if (loved == null) items else listOf(loved.toShelf()) + items
    }
}