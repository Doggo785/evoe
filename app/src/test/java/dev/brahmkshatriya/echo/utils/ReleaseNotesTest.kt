package dev.brahmkshatriya.echo.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins [ReleaseNotes] — turns release-please markdown into plain blocks the
 * update sheet can style natively. Pure JVM so it runs without Android.
 */
class ReleaseNotesTest {

    private val body = """
        ## [3.3.0](https://github.com/Doggo785/evoe/compare/v3.2.0...v3.3.0) (2026-10-07)


        ### Features

        * **player:** start playback without waiting for likes ([658fc80](https://github.com/Doggo785/evoe/commit/658fc80db590c4417107bdf2c11d416c1b50a547))


        ### Bug Fixes

        * **extensions:** register file pickers before fragment creation ([b10b326](https://github.com/Doggo785/evoe/commit/b10b326c909aafad42db29a27f379b035bde92fa))
        * fix **crash** on rotation without a scope
    """.trimIndent()

    // The card header shows when the release went out.
    @Test
    fun `reads the release date from the header line`() {
        assertEquals("2026-10-07", ReleaseNotes.date(body))
    }

    // No header, no date — the card just shows the tag.
    @Test
    fun `returns no date without a header line`() {
        assertNull(ReleaseNotes.date("* **player:** something"))
        assertNull(ReleaseNotes.date(null))
        assertNull(ReleaseNotes.date("   "))
    }

    // The ## line is the card header, not a content block.
    @Test
    fun `skips the title line in blocks`() {
        val blocks = ReleaseNotes.blocks(body)
        assertEquals(
            listOf(
                ReleaseNotes.Block.SubHeader("Features"),
                ReleaseNotes.Block.Bullet(
                    scope = "player:",
                    text = "start playback without waiting for likes"
                ),
                ReleaseNotes.Block.SubHeader("Bug Fixes"),
                ReleaseNotes.Block.Bullet(
                    scope = "extensions:",
                    text = "register file pickers before fragment creation"
                ),
                ReleaseNotes.Block.Bullet(
                    scope = null,
                    text = "fix crash on rotation without a scope"
                )
            ),
            blocks
        )
    }

    // Issue refs and commit hashes in parens are GitHub noise, not changelog text.
    @Test
    fun `drops issue refs and hashes from bullet text`() {
        val line = "* **updater:** offer app updates by semver comparison " +
            "(([#13](https://github.com/Doggo785/evoe/issues/13)) " +
            "([e32cbf1](https://github.com/Doggo785/evoe/commit/e32cbf1)))"
        assertEquals(
            listOf(
                ReleaseNotes.Block.Bullet(
                    scope = "updater:",
                    text = "offer app updates by semver comparison"
                )
            ),
            ReleaseNotes.blocks(line)
        )
    }

    // Plain prose lines survive as paragraphs, links keeping only their text.
    @Test
    fun `keeps prose lines as cleaned paragraphs`() {
        assertEquals(
            listOf(
                ReleaseNotes.Block.Paragraph("See the full list on GitHub")
            ),
            ReleaseNotes.blocks("See the [full list](https://github.com/Doggo785/evoe) on GitHub")
        )
    }

    // Nothing to show — the sheet falls back to its no-notes string.
    @Test
    fun `returns no blocks for a blank body`() {
        assertEquals(emptyList<ReleaseNotes.Block>(), ReleaseNotes.blocks(null))
        assertEquals(emptyList<ReleaseNotes.Block>(), ReleaseNotes.blocks("  \n "))
    }
}
