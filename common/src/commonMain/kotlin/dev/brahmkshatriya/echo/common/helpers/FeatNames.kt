package dev.brahmkshatriya.echo.common.helpers

private val featMarker = Regex("""(?i)\b(?:feat\.?|ft\.?|featuring)\b\s*""")
private val featTailCutoffs = listOf(" - ", " (", " [")
private val featNameSeparators = listOf(",", "&", " and ", " x ", " X ", "/", ";")

/**
 * Featuring guest names parsed from a display title ("Song (feat. B, C & D)").
 * Word-boundaried so "Feature" or "Often" never match; empty when the title
 * carries no feat./ft./featuring marker. Used by the per-artist liked menus to
 * match guests credited as display text only. Pure, unit-tested.
 */
fun featNamesFromTitle(title: String): List<String> {
    val match = featMarker.find(title) ?: return emptyList()
    val tail = title.substring(match.range.last + 1)
    val head = featTailCutoffs.fold(tail) { acc, cut -> acc.split(cut).first() }
    return featNameSeparators.fold(listOf(head)) { acc, sep -> acc.flatMap { it.split(sep) } }
        .map { it.trim('(', '[', ' ', ')', ']', '.', ',') }
        .filter { it.isNotEmpty() }
}

private val artistFieldSeparators = listOf(",", "&", " and ")

/**
 * Splits a performer credit string ("A & B feat. C") into names. Same delimiters
 * as a plain split plus feat./ft./featuring markers, so featuring guests become
 * artists of their own instead of hiding inside one string. Pure, unit-tested.
 */
fun splitArtistField(raw: String): List<String> {
    val match = featMarker.find(raw)
    val head = match
        ?.let { raw.substring(0, it.range.first).trim().trimEnd('(', '[', '-').trim() }
        ?: raw.trim()
    val headNames = artistFieldSeparators.fold(listOf(head)) { acc, sep ->
        acc.flatMap { it.split(sep) }
    }.map { it.trim() }.filter { it.isNotEmpty() }
    return headNames + if (match != null) featNamesFromTitle(raw) else emptyList()
}
