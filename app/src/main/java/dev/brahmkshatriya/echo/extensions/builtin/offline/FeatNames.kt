package dev.brahmkshatriya.echo.extensions.builtin.offline

import dev.brahmkshatriya.echo.common.helpers.featNamesFromTitle
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Per-artist liked predicate: id match on the tagged artists, plus featuring
 * guests credited only in the display title ("Song (feat. A)"), matched by name.
 */
internal fun isLikedByArtist(track: Track, artist: Artist): Boolean =
    track.artists.any { it.id == artist.id } ||
        (artist.name.isNotBlank() && featNamesFromTitle(track.title).any {
            it.equals(artist.name, ignoreCase = true)
        })
