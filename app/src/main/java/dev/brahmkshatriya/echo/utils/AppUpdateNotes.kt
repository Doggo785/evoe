package dev.brahmkshatriya.echo.utils

/**
 * Which releases make up the changelog shown in the app-update prompt.
 * Pure JVM — no Android imports — so it stays unit-testable like [SemVer].
 */
object AppUpdateNotes {

    private const val MAX_NOTES = 10

    data class AppRelease(val tag: String, val body: String?)

    /**
     * The releases newer than [current], newest first, capped to [max].
     * Newness is [SemVer.shouldOffer] in semver mode, so unparseable tags
     * still offer via the string fallback instead of being silently dropped.
     */
    fun pendingReleases(
        current: String,
        releases: List<AppRelease>,
        max: Int = MAX_NOTES
    ): List<AppRelease> =
        releases.filter { SemVer.shouldOffer(it.tag, current, semver = true) }
            .take(max.coerceAtLeast(0))
}
