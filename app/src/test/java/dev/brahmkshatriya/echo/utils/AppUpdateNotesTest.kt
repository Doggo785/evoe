package dev.brahmkshatriya.echo.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins [AppUpdateNotes.pendingReleases] — which releases' bodies make up the
 * changelog shown in the app-update prompt. Pure JVM so it runs without Android.
 */
class AppUpdateNotesTest {

    private fun release(tag: String, body: String? = "notes for $tag") =
        AppUpdateNotes.AppRelease(tag, body)

    // Newest-first input, two newer than current -> both kept, order preserved.
    @Test
    fun `keeps every newer release newest-first`() {
        val releases = listOf(release("v3.3.0"), release("v3.2.0"), release("v3.1.0"))
        val pending = AppUpdateNotes.pendingReleases("v3.1.0", releases)
        assertEquals(listOf(release("v3.3.0"), release("v3.2.0")), pending)
    }

    // Nothing newer -> empty, so the caller offers no prompt at all.
    @Test
    fun `returns empty when nothing is newer`() {
        val releases = listOf(release("v3.1.0"), release("v3.0.0"))
        assertEquals(emptyList<AppUpdateNotes.AppRelease>(), AppUpdateNotes.pendingReleases("v3.1.0", releases))
    }

    // Same release under another spelling ("3.2.0" vs "v3.2.0") is not an update.
    @Test
    fun `skips an equal tag under another spelling`() {
        val releases = listOf(release("3.2.0"))
        assertEquals(emptyList<AppUpdateNotes.AppRelease>(), AppUpdateNotes.pendingReleases("v3.2.0", releases))
    }

    // Lower tags are never offered as updates (no downgrade prompt).
    @Test
    fun `skips lower tags`() {
        val releases = listOf(release("v3.3.0"), release("v3.0.0"))
        val pending = AppUpdateNotes.pendingReleases("v3.2.0", releases)
        assertEquals(listOf(release("v3.3.0")), pending)
    }

    // A tag we cannot parse still offers via the string fallback, never silently dropped.
    @Test
    fun `offers an unparseable tag by the string fallback`() {
        val releases = listOf(release("v0.9-beta"))
        val pending = AppUpdateNotes.pendingReleases("v0.8", releases)
        assertEquals(releases, pending)
    }

    // Bounds the dialog: only the newest `max` releases are kept.
    @Test
    fun `caps to the newest max releases`() {
        val releases = (10 downTo 1).map { release("v3.$it.0") }
        val pending = AppUpdateNotes.pendingReleases("v3.0.0", releases, max = 3)
        assertEquals(releases.take(3), pending)
    }
}
