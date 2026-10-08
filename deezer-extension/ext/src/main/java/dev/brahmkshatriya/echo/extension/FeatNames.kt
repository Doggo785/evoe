package dev.brahmkshatriya.echo.extension

private val featMarker = Regex("""(?i)\b(?:feat\.?|ft\.?|featuring)\b\s*""")
private val featTailCutoffs = listOf(" - ", " (", " [")
private val featNameSeparators = listOf(",", "&", " and ", " x ", " X ", "/", ";")

/**
 * Featuring guest names parsed from a display title ("Song (feat. B, C & D)").
 * Word-boundaried so "Feature" or "Often" never match; empty when the title
 * carries no feat./ft./featuring marker. Pure, unit-tested through the callers.
 */
internal fun featNamesFromTitle(title: String): List<String> {
    val match = featMarker.find(title) ?: return emptyList()
    val tail = title.substring(match.range.last + 1)
    val head = featTailCutoffs.fold(tail) { acc, cut -> acc.split(cut).first() }
    return featNameSeparators.fold(listOf(head)) { acc, sep -> acc.flatMap { it.split(sep) } }
        .map { it.trim('(', '[', ' ', ')', ']', '.', ',') }
        .filter { it.isNotEmpty() }
}
