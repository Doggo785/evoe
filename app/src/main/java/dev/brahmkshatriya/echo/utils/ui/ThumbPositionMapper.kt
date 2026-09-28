package dev.brahmkshatriya.echo.utils.ui

import kotlin.math.roundToInt

/**
 * Thumb fraction <-> adapter position math for the absolute fast-scroll jump.
 *
 * Pure JVM on purpose: PixelFastScrollViewHelper calls this from scrollTo, and
 * the decision itself must stay testable without Android. Every input is
 * clamped, so a caller can pass a raw library offset without pre-checks.
 */
object ThumbPositionMapper {

    /**
     * Recovers the 0..1 thumb fraction from the absolute [offset] FastScroller
     * hands to scrollTo. [span] must be the exact span the library multiplied
     * by (getScrollRange() - view height), so the division undoes it.
     */
    fun fractionFor(offset: Int, span: Int): Double {
        if (span <= 0) return 0.0
        return (offset.toDouble() / span).coerceIn(0.0, 1.0)
    }

    /**
     * Maps a 0..1 [fraction] of the rail to an adapter position. Rounds to the
     * nearest item so the bottom of the rail (1.0) is exactly the last item and
     * the jump target only depends on the stable item count, never on the
     * pixel-range estimate that wobbles with the attached window.
     */
    fun positionFor(fraction: Double, itemCount: Int): Int {
        if (itemCount <= 0 || fraction.isNaN()) return 0
        val last = itemCount - 1
        return (fraction.coerceIn(0.0, 1.0) * last).roundToInt().coerceIn(0, last)
    }
}
