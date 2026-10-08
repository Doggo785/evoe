package dev.brahmkshatriya.echo.extension.clients

import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.DeezerParser
import dev.brahmkshatriya.echo.extension.DeezerSession
import dev.brahmkshatriya.echo.extension.clients.DeezerLibraryClient.Companion.prependLovedCard
import dev.brahmkshatriya.echo.extension.clients.DeezerLibraryClient.Companion.withLovedCard
import dev.brahmkshatriya.echo.extension.clients.DeezerPlaylistClient.Companion.FAVORITES_EXTRA
import dev.brahmkshatriya.echo.extension.clients.DeezerPlaylistClient.Companion.isFavoritesPlaylist
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real loved-tracks playlist ("Favourite tracks") resolved from Home's Recently played.
 * Pure JSON-to-model mapping only: detection, cover, exact Deezer title, shelf placement
 * and the hidden-when-absent contract. Network resolution (resolveLovedPlaylist) and the
 * opened-card playback stay device-verified.
 */
class LovedPlaylistTest {

    private val parser = DeezerParser(DeezerSession())

    private fun page(json: String): JsonObject =
        Json.parseToJsonElement(json).jsonObject

    private fun lovedEntry(withFlag: Boolean = true): String {
        val layout = if (withFlag) {
            ""","layout_parameters":{"picture":{"local_loved_playlist":true}}"""
        } else {
            ""
        }
        return """
            {
                "id":"13601863481","type":"playlist",
                "data":{
                    "PLAYLIST_ID":"13601863481","TITLE":"Favourite tracks",
                    "PICTURE_TYPE":"playlist",
                    "PLAYLIST_PICTURE":"22c7b5b2848c9db6f3b8d24e8e15d174",
                    "NB_SONG":3670,"TYPE":"4","__TYPE__":"playlist"
                }$layout,"title":"Favourite tracks","subtitle":"3 670 tracks"
            }
        """.trimIndent()
    }

    private fun lovedPage(withFlag: Boolean = true): JsonObject = page(
        """{"results":{"sections":[{"title":"Recently played","items":[${lovedEntry(withFlag)}]}]}}"""
    )

    private fun normalPage(): JsonObject = page(
        """{"results":{"sections":[{"title":"Recently played","items":[{
            "id":"14595282941","type":"playlist",
            "data":{
                "PLAYLIST_ID":"14595282941","TITLE":"Anime",
                "PICTURE_TYPE":"playlist",
                "PLAYLIST_PICTURE":"e591ad9b54d92f5840a317185bcc5a58",
                "NB_SONG":89,"TYPE":"0","__TYPE__":"playlist"
            },"title":"Anime","subtitle":"89 tracks"
        }]}]}}"""
    )

    private fun playlist(id: String, extras: Map<String, String> = mapOf()) =
        Playlist(id = id, title = "P", isEditable = true, extras = extras)

    // Detection.

    @Test
    fun `flagged entry resolves with real id, exact title and cover`() {
        val loved = parser.findLovedPlaylist(lovedPage())!!
        assertEquals("13601863481", loved.id)
        assertEquals("Favourite tracks", loved.title)
        assertNotNull(loved.cover)
    }

    @Test
    fun `type 4 entry resolves without the flag`() {
        val loved = parser.findLovedPlaylist(lovedPage(withFlag = false))!!
        assertEquals("13601863481", loved.id)
    }

    @Test
    fun `normal playlists do not resolve`() {
        assertNull(parser.findLovedPlaylist(normalPage()))
    }

    @Test
    fun `empty page does not resolve`() {
        assertNull(parser.findLovedPlaylist(page("""{"results":{"sections":[]}}""")))
    }

    @Test
    fun `artist entry is not a loved playlist entry`() {
        val entry = page(
            """{"data":{"ART_ID":"577666","ART_NAME":"Childish Gambino","__TYPE__":"artist"}}"""
        )
        assertFalse(parser.run { entry.isLovedPlaylistEntry() })
    }

    // Routing tag: the resolved card keeps the favorites key so open/play route
    // through the favorites branches, while its numeric id selects the real fetch.

    @Test
    fun `tagged real card still routes to favorites`() {
        val loved = parser.findLovedPlaylist(lovedPage())!!
        val tagged = loved.copy(extras = loved.extras + mapOf(FAVORITES_EXTRA to "1"))
        assertTrue(isFavoritesPlaylist(tagged))
        assertEquals("13601863481", tagged.id)
        assertNotNull(tagged.cover)
    }

    // Shelf placement: head when present, untouched when absent.

    @Test
    fun `all tab heads the real card`() {
        val loved = parser.findLovedPlaylist(lovedPage())!!
        val shelf = Shelf.Lists.Items(
            id = "Playlists", title = "Playlists",
            list = listOf(playlist("1"), playlist("2"))
        )
        val result = withLovedCard("Playlists", shelf, loved) as Shelf.Lists.Items
        assertEquals(listOf("13601863481", "1", "2"), result.list.map { it.id })
        assertEquals("Favourite tracks", (result.list.first() as Playlist).title)
    }

    @Test
    fun `all tab hides the card when deezer does not send it`() {
        val shelf = Shelf.Lists.Items(
            id = "Playlists", title = "Playlists",
            list = listOf(playlist("1"))
        )
        val result = withLovedCard("Playlists", shelf, null) as Shelf.Lists.Items
        assertEquals(listOf("1"), result.list.map { it.id })
    }

    @Test
    fun `all tab stays empty when shelf and loved are both absent`() {
        assertNull(withLovedCard("Playlists", null, null))
    }

    @Test
    fun `playlists tab heads the real card row`() {
        val loved = parser.findLovedPlaylist(lovedPage())!!
        val rows = listOf(Shelf.Item(playlist("1")))
        val result = prependLovedCard(rows, loved)
        assertEquals(2, result.size)
        assertEquals("13601863481", result.first().id)
    }

    @Test
    fun `playlists tab hides the card row when deezer does not send it`() {
        val rows = listOf(Shelf.Item(playlist("1")))
        assertEquals(rows, prependLovedCard(rows, null))
    }
}
