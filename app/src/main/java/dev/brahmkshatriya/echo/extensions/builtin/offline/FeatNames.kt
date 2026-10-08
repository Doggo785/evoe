package dev.brahmkshatriya.echo.extensions.builtin.offline

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Track

private val featMarker = Regex("""(?i)\b(?:feat\.?|ft\.?|featuring)\b\s*""")
private val featTailCutoffs = listOf(" - ", " (", " [")
private val featNameSeparators = listOf(",", "&", " and ", " x ", " X ", "/", ";")
private val artistFieldSeparators = listOf(",", "&", " and ")

/**
 * Featuring guest names parsed from a display title ("Song (feat. B, C & D)").
 * Word-boundaried so "Feature" or "Often" never match; empty when the title
 * carries no feat./ft./featuring marker. Pure, unit-tested.
 */
internal fun featNamesFromTitle(title: String): List<String> {
    val match = featMarker.find(title) ?: return emptyList()
    val tail = title.substring(match.range.last + 1)
    val head = featTailCutoffs.fold(tail) { acc, cut -> acc.split(cut).first() }
    return featNameSeparators.fold(listOf(head)) { acc, sep -> acc.flatMap { it.split(sep) } }
        .map { it.trim('(', '[', ' ', ')', ']', '.', ',') }
        .filter { it.isNotEmpty() }
}

/**
 * Splits a MediaStore ARTIST field ("A & B feat. C") into performer names.
 * Same delimiters as before plus feat./ft./featuring markers, so featuring
 * guests become artists of their own instead of hiding inside one string.
 */
internal fun splitArtistField(raw: String): List<String> {
    val match = featMarker.find(raw)
    val head = match
        ?.let { raw.substring(0, it.range.first).trim().trimEnd('(', '[', '-').trim() }
        ?: raw.trim()
    val headNames = artistFieldSeparators.fold(listOf(head)) { acc, sep ->
        acc.flatMap { it.split(sep) }
    }.map { it.trim() }.filter { it.isNotEmpty() }
    return headNames + if (match != null) featNamesFromTitle(raw) else emptyList()
}

/**
 * Per-artist liked predicate: id match on the tagged artists, plus featuring
 * guests credited only in the display title ("Song (feat. A)"), matched by name.
 */
internal fun isLikedByArtist(track: Track, artist: Artist): Boolean =
    track.artists.any { it.id == artist.id } ||
        (artist.name.isNotBlank() && featNamesFromTitle(track.title).any {
            it.equals(artist.name, ignoreCase = true)
        })
