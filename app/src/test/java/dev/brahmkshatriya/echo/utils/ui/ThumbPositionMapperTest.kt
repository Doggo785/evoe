package dev.brahmkshatriya.echo.utils.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Thumb fraction <-> adapter position math for the absolute fast-scroll jump.
 * Pure JVM — no views, no RecyclerView.
 *
 * SCOPE NOTE: keeping the thumb under the finger on mixed-height feeds and the
 * reachability of the very bottom stay device-verified (GladixScroll trace +
 * Davey! durations on hardware). What decides the jump target — clamping,
 * rounding, empty and single-item lists — is extracted here and pinned below.
 */
class ThumbPositionMapperTest {

    @Test
    fun `top of the rail is the first item`() {
        assertEquals(0, ThumbPositionMapper.positionFor(0.0, 100))
    }

    @Test
    fun `bottom of the rail is the last item`() {
        assertEquals(99, ThumbPositionMapper.positionFor(1.0, 100))
    }

    @Test
    fun `middle of the rail rounds to the middle item`() {
        assertEquals(
            (0.5 * 100).roundToInt(),
            ThumbPositionMapper.positionFor(0.5, 101)
        )
    }

    @Test
    fun `fraction above one clamps to the last item`() {
        assertEquals(99, ThumbPositionMapper.positionFor(1.4, 100))
    }

    @Test
    fun `fraction below zero clamps to the first item`() {
        assertEquals(0, ThumbPositionMapper.positionFor(-0.2, 100))
    }

    @Test
    fun `nan fraction lands on the first item`() {
        assertEquals(0, ThumbPositionMapper.positionFor(Double.NaN, 100))
    }

    @Test
    fun `empty list lands on zero without crashing`() {
        assertEquals(0, ThumbPositionMapper.positionFor(0.7, 0))
    }

    @Test
    fun `single item list always lands on zero`() {
        assertEquals(0, ThumbPositionMapper.positionFor(0.0, 1))
        assertEquals(0, ThumbPositionMapper.positionFor(1.0, 1))
    }

    @Test
    fun `very large list reaches its last item`() {
        assertEquals(9999, ThumbPositionMapper.positionFor(1.0, 10_000))
    }

    @Test
    fun `offset over span is the thumb fraction`() {
        assertEquals(0.5, ThumbPositionMapper.fractionFor(500, 1000), 0.0)
    }

    @Test
    fun `fraction clamps past the rail end`() {
        assertEquals(1.0, ThumbPositionMapper.fractionFor(1500, 1000), 0.0)
    }

    @Test
    fun `zero span never divides`() {
        assertEquals(0.0, ThumbPositionMapper.fractionFor(500, 0), 0.0)
    }
}
