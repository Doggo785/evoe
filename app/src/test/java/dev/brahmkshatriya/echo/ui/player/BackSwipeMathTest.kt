package dev.brahmkshatriya.echo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decision math for the full player's backward edge swipe. Pure JVM — no pager, no views.
 *
 * SCOPE NOTE: the finger-follow tiling, the joint exit animation and the fade handoff
 * need ViewPager2 and stay device-verified on hardware. What decides those paths —
 * direction folding, arming, commit, and both translations — is pinned below.
 */
class BackSwipeMathTest {

    // Direction folding: backwards reads positive toward the previous track in both layouts.

    @Test
    fun `ltr right drag is backwards`() {
        assertEquals(60f, BackSwipeMath.backwards(60f, isRtl = false))
    }

    @Test
    fun `rtl left drag is backwards`() {
        assertEquals(60f, BackSwipeMath.backwards(-60f, isRtl = true))
    }

    @Test
    fun `ltr left drag is not backwards`() {
        assertTrue(BackSwipeMath.backwards(-60f, isRtl = false) < 0f)
    }

    // Arming: a clear backward start, never a vertical sheet drag.

    @Test
    fun `arms past slop when horizontal dominant`() {
        assertTrue(BackSwipeMath.shouldArm(backwards = 20f, dyAbs = 5f, slop = 16f))
    }

    @Test
    fun `does not arm within slop`() {
        assertFalse(BackSwipeMath.shouldArm(backwards = 10f, dyAbs = 2f, slop = 16f))
    }

    @Test
    fun `does not arm a vertical drag`() {
        assertFalse(BackSwipeMath.shouldArm(backwards = 30f, dyAbs = 40f, slop = 16f))
    }

    // Commit: far enough and still horizontal at release.

    @Test
    fun `qualifies past threshold when horizontal`() {
        assertTrue(BackSwipeMath.isQualified(backwards = 100f, dyAbs = 20f, threshold = 48f))
    }

    @Test
    fun `does not qualify a short drag`() {
        assertFalse(BackSwipeMath.isQualified(backwards = 30f, dyAbs = 5f, threshold = 48f))
    }

    @Test
    fun `does not qualify a diagonal ending`() {
        assertFalse(BackSwipeMath.isQualified(backwards = 100f, dyAbs = 120f, threshold = 48f))
    }

    // Current page follow: 1:1, clamped to the exit side, mirrored in RTL.

    @Test
    fun `ltr follow tracks finger and clamps`() {
        assertEquals(60f, BackSwipeMath.clampDrag(60f, 1000f, 1f))
        assertEquals(0f, BackSwipeMath.clampDrag(-50f, 1000f, 1f))
        assertEquals(1000f, BackSwipeMath.clampDrag(1500f, 1000f, 1f))
    }

    @Test
    fun `rtl follow mirrors`() {
        assertEquals(-60f, BackSwipeMath.clampDrag(-60f, 1000f, -1f))
        assertEquals(0f, BackSwipeMath.clampDrag(50f, 1000f, -1f))
        assertEquals(-1000f, BackSwipeMath.clampDrag(-1500f, 1000f, -1f))
    }

    // Preview tiling: off-screen at rest, landed exactly at one width of travel.

    @Test
    fun `preview waits one width off-screen at rest`() {
        assertEquals(-1000f, BackSwipeMath.previewTx(0f, 1f, 1000f))
        assertEquals(1000f, BackSwipeMath.previewTx(0f, -1f, 1000f))
    }

    @Test
    fun `preview lands at zero after one width of travel`() {
        assertEquals(0f, BackSwipeMath.previewTx(1000f, 1f, 1000f))
        assertEquals(0f, BackSwipeMath.previewTx(-1000f, -1f, 1000f))
    }

    @Test
    fun `preview tracks mid-drag`() {
        assertEquals(-600f, BackSwipeMath.previewTx(400f, 1f, 1000f))
    }

    // Reversal: the finger escaped forward, hand the stream to native.

    @Test
    fun `reversal past slop hands over`() {
        assertTrue(BackSwipeMath.isReversed(backwards = -20f, slop = 16f))
    }

    @Test
    fun `no reversal inside the dead zone`() {
        assertFalse(BackSwipeMath.isReversed(backwards = -10f, slop = 16f))
        assertFalse(BackSwipeMath.isReversed(backwards = 50f, slop = 16f))
    }

    // Exit edge: full width on the drag side.

    @Test
    fun `exit edge is one width on the drag side`() {
        assertEquals(1000f, BackSwipeMath.exitTx(1f, 1000f))
        assertEquals(-1000f, BackSwipeMath.exitTx(-1f, 1000f))
    }
}
