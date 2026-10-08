package dev.brahmkshatriya.echo.extension.clients

import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.extension.DeezerParser
import dev.brahmkshatriya.echo.extension.DeezerSession

// Shared pure-JVM fixtures for the playlist routing tests (FavoritesPlaylistTest,
// LovedPlaylistTest): one parser and one playlist builder so the helpers are defined
// once. Both stay side-effect free: the parser only reads JSON with null settings.
internal val testParser: DeezerParser = DeezerParser(DeezerSession())

internal fun testPlaylist(id: String, extras: Map<String, String> = mapOf()): Playlist =
    Playlist(id = id, title = "P", isEditable = true, extras = extras)
