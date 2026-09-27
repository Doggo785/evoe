package dev.brahmkshatriya.echo.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins [SemVer.shouldOffer] — the whole decision the app updater makes with a release tag, both
 * modes. The network half (fetching the release, picking the asset) stays in AppUpdater and is not
 * covered here; this module is pure JVM by design so these cases run without Android.
 *
 * The two properties the shared function must never break, because three callers depend on them:
 * the "" sentinel still means "take whatever is latest", and third-party extension tags that do not
 * parse still compare by plain string inequality.
 */
class SemVerTest {

    // ── DEFAULT MODE (semver = false): exactly the `!=` every caller had before SemVer existed.

    // Different tag, any shape -> offered.
    @Test
    fun `string mode offers a different tag`() {
        assertTrue(SemVer.shouldOffer("v3.2.0", "v3.1.0", semver = false))
    }

    // Equal tag -> the running build is up to date.
    @Test
    fun `string mode skips an equal tag`() {
        assertFalse(SemVer.shouldOffer("v3.2.0", "v3.2.0", semver = false))
    }

    // AddViewModel's sentinel: no current version, take whatever is latest. Any non-empty tag
    // compares unequal, which is the intent — a parsing rule would break add-extension entirely.
    @Test
    fun `string mode takes the empty sentinel as take-latest`() {
        assertTrue(SemVer.shouldOffer("v3.2.0", "", semver = false))
    }

    // Extension tags are third-party shapes; the string path never parses them and must not care.
    @Test
    fun `string mode accepts a third-party tag shape`() {
        assertTrue(SemVer.shouldOffer("v0.9-beta", "v0.8", semver = false))
    }

    // ── SEMVER MODE (semver = true): the app update path only.

    // Higher tag -> offer.
    @Test
    fun `semver mode offers a higher tag`() {
        assertTrue(SemVer.shouldOffer("v3.2.0", "v3.1.9", semver = true))
    }

    // Equal tag -> no offer. Under plain != an equal tag already offered nothing; what semver adds
    // is equal DESPITE DIFFERENT SPELLING — see the next case.
    @Test
    fun `semver mode skips an equal tag`() {
        assertFalse(SemVer.shouldOffer("v3.2.0", "v3.2.0", semver = true))
    }

    // The regression this whole mode exists for: tag "3.2.0" against a running "v3.2.0" is the same
    // release, but plain != sees two different strings and would re-offer it on every check forever.
    // Parsing strips the optional v, the components are equal, no offer.
    @Test
    fun `semver mode sees past a missing v prefix`() {
        assertFalse(SemVer.shouldOffer("3.2.0", "v3.2.0", semver = true))
    }

    // Lower tag -> no downgrade offer (the OS would refuse the install anyway; this skips the
    // spurious prompt). Plain != WOULD offer this — behavior deliberately changed for the app path.
    @Test
    fun `semver mode skips a lower tag`() {
        assertFalse(SemVer.shouldOffer("v3.1.9", "v3.2.0", semver = true))
    }

    // Components are compared as NUMBERS. A naive string comparison sorts "3.1.10" below "3.1.9" —
    // the exact lexicographic trap that led to version.txt (see the Crashlytics note in
    // app/build.gradle.kts). Both directions pinned.
    @Test
    fun `semver mode compares components numerically`() {
        assertTrue(SemVer.shouldOffer("v3.1.10", "v3.1.9", semver = true))
        assertFalse(SemVer.shouldOffer("v3.1.9", "v3.1.10", semver = true))
    }

    // Unparseable TAG -> fall back to string inequality, never to "no update": a version we cannot
    // read must still be able to trigger an update.
    @Test
    fun `semver mode falls back to string inequality for an unparseable tag`() {
        assertTrue(SemVer.shouldOffer("v0.9-beta", "v0.8", semver = true))
    }

    // Unparseable CURRENT -> same fallback direction.
    @Test
    fun `semver mode falls back to string inequality for an unparseable current`() {
        assertTrue(SemVer.shouldOffer("v3.2.0", "junk", semver = true))
    }

    // The "" sentinel must survive the opt-in too: parse fails, fallback is !=, non-empty tag wins.
    // If this ever returned false, adding an extension would silently offer nothing.
    @Test
    fun `semver mode keeps the empty sentinel working`() {
        assertTrue(SemVer.shouldOffer("v3.2.0", "", semver = true))
    }

    // An absurd digit run does not fit an Int. toIntOrNull must make it take the fallback path
    // instead of throwing NumberFormatException out of the comparison.
    @Test
    fun `semver mode falls back instead of throwing on an oversized number`() {
        assertTrue(
            SemVer.shouldOffer("v99999999999999999999.0.0", "v1.0.0", semver = true)
        )
    }

    // Pre-release tags are not parsed (release-please prereleases are not configured here). The
    // fallback offers them by string inequality — documented behavior, not an accident: better an
    // offered pre-release than a silently ignored one. See SemVer's KDoc if that ever changes.
    @Test
    fun `semver mode offers a pre-release tag by the string fallback`() {
        assertTrue(SemVer.shouldOffer("v3.3.0-beta.1", "v3.2.0", semver = true))
    }

    // The historical padded format ("v3.1.01145", the gitCount scheme) still parses — leading zeros
    // are decimal, not octal — so a build straddling the version.txt migration compares numerically
    // instead of failing outright. 3.2.0 > 3.1.1145 by the minor component.
    @Test
    fun `semver mode parses the old zero-padded format numerically`() {
        assertTrue(SemVer.shouldOffer("v3.2.0", "v3.1.01145", semver = true))
    }
}
