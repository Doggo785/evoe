package dev.brahmkshatriya.echo.utils

/**
 * Version comparison for the app self-updater. Pure JVM — no Android imports — so the decision
 * logic is unit-testable (SemVerTest); AppUpdater holds the network half.
 *
 * The accepted shape is exactly MAJOR.MINOR.PATCH with an optional leading "v", which is what
 * release-please tags ("v3.2.0") and what versionName's prefix is since versionName started
 * reading version.txt ("v" + version.txt + "_hash(count)").
 *
 * ⚠️ DELIBERATELY NOT a full SemVer implementation: no pre-release identifiers ("3.3.0-beta.1"),
 * no build metadata ("+build"), no ordering between pre-releases. release-please-config.json
 * enables none of that here, and every shape we do not parse falls back to plain string inequality
 * in [shouldOffer] instead of to "no update" — an unrecognised version must still be able to offer
 * an update. If pre-releases are ever enabled, the fallback keeps the updater working unchanged
 * (string comparison) and this file is the one place to extend.
 */
object SemVer {

    // Anchoring comes from matchEntire; ^$ would be redundant with it.
    private val PATTERN = Regex("""v?(\d+)\.(\d+)\.(\d+)""")

    /**
     * Whether [latestTag] should be offered as an update against [current].
     *
     * - `semver = false` — plain string inequality, the behaviour every caller had before this file
     *   existed. This is the DEFAULT and it is load-bearing for two callers: extension tags are
     *   third-party shapes that do not parse, and AddViewModel passes "" as "no current version,
     *   take whatever is latest" — any non-empty tag compares unequal and is offered.
     * - `semver = true` — the app update path only. True only when both sides parse as x.y.z AND
     *   the tag is strictly greater, component by component: equal offers nothing (a tag differing
     *   from the running version only in spelling, e.g. "3.2.0" vs "v3.2.0", would loop forever
     *   under plain `!=`), lower offers nothing (no downgrade). Either side unparseable falls back
     *   to `!=`, never to false.
     */
    fun shouldOffer(latestTag: String, current: String, semver: Boolean): Boolean =
        if (!semver) latestTag != current
        else {
            val latest = parse(latestTag)
            val now = parse(current)
            if (latest == null || now == null) latestTag != current
            // First differing component decides — standard version ordering. Both lists have exactly
            // three entries (three capture groups), so index-wise access is safe. All-equal (null)
            // means the same release under two spellings at most: no offer.
            else latest.indices.firstOrNull { latest[it] != now[it] }
                ?.let { latest[it] > now[it] } ?: false
        }

    // Three Ints, or null when the string is not plain x.y.z. toIntOrNull guards an absurd digit
    // run ("v99999999999999999999.0.0") from throwing out of the comparison; a value that does not
    // fit an Int simply does not parse and takes the fallback path.
    private fun parse(raw: String): List<Int>? {
        val parts = PATTERN.matchEntire(raw)?.groupValues?.drop(1) ?: return null
        return parts.mapNotNull { it.toIntOrNull() }.takeIf { it.size == parts.size }
    }
}
