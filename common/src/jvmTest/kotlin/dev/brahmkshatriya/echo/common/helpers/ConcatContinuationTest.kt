package dev.brahmkshatriya.echo.common.helpers

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The continuation token Concat hands back to the app is opaque, but its encoding decides which source
 * serves the next page and with what token. These are the rules that encoding has to keep.
 */
class ConcatContinuationTest {

    @Test
    fun `null continuation addresses the first source from its start`() {
        assertEquals(0 to null, ConcatContinuation.split(null))
    }

    @Test
    fun `source boundary addresses the next source with a null token`() {
        assertEquals(1 to null, ConcatContinuation.split("1_"))
    }

    @Test
    fun `token inside a source round-trips`() {
        assertEquals(0 to "abc", ConcatContinuation.split(ConcatContinuation.combine(0, "abc")))
        assertEquals(2 to "abc", ConcatContinuation.split("2_abc"))
    }

    @Test
    fun `null token round-trips to null`() {
        assertEquals(3 to null, ConcatContinuation.split(ConcatContinuation.combine(3, null)))
    }

    @Test
    fun `garbage continuation addresses no source`() {
        val (index, _) = ConcatContinuation.split("not-a-number")
        assertEquals(-1, index)
    }
}
