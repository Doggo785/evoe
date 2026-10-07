package dev.brahmkshatriya.echo.extension

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Generation-guarded like memory for the playback path.
 * Pure state machine, no network: the 5.8s favorite_song.getList must never
 * run between tap and first sound. Covers resolve order, stale-drop,
 * rollback, late-sync merge, truncation guard and account reset.
 */
class LikeStateTest {

    @Test
    fun `unknown when never loaded and no flag`() {
        assertNull(LikeState().resolve("1", null))
    }

    @Test
    fun `flag answers when memory empty`() {
        val s = LikeState()
        assertEquals(true, s.resolve("1", true))
        assertEquals(false, s.resolve("1", false))
    }

    @Test
    fun `absent from a complete list is definitively false`() {
        val s = LikeState()
        s.merge(setOf("1"), complete = true)
        assertTrue(s.fullyLoaded)
        assertEquals(false, s.resolve("2", null))
        assertEquals(false, s.resolve("2", true))
    }

    @Test
    fun `present in memory is true even against a stale flag`() {
        val s = LikeState()
        s.merge(setOf("1"), complete = true)
        assertEquals(true, s.resolve("1", false))
    }

    @Test
    fun `truncated list stays partial and absence is unknown`() {
        val s = LikeState()
        s.merge(setOf("1"), complete = false)
        assertFalse(s.fullyLoaded)
        assertEquals(true, s.resolve("1", null))
        assertEquals(true, s.resolve("2", true))
        assertNull(s.resolve("2", null))
    }

    @Test
    fun `pending like overrides memory until confirmed`() {
        val s = LikeState()
        s.merge(setOf(), complete = true)
        assertEquals(false, s.resolve("1", null))
        s.userLike("1")
        assertEquals(true, s.resolve("1", null))
    }

    @Test
    fun `confirm clears pending so the next sync reflects the server`() {
        val s = LikeState()
        s.merge(setOf(), complete = true)
        val gen = s.userLike("1")
        s.confirm("1", liked = true, gen = gen)
        assertFalse(s.isPending("1"))
        s.merge(setOf(), complete = true)
        assertEquals(false, s.resolve("1", null))
    }

    @Test
    fun `rollback restores memory when nothing newer happened`() {
        val s = LikeState()
        s.merge(setOf(), complete = true)
        val gen = s.userLike("1")
        assertEquals(false, s.rollback("1", liked = true, gen = gen))
        assertFalse(s.isPending("1"))
        assertEquals(false, s.resolve("1", null))
    }

    @Test
    fun `stale rollback never overwrites a newer tap`() {
        val s = LikeState()
        s.merge(setOf(), complete = true)
        val old = s.userLike("1")
        s.userUnlike("1")
        assertNull(s.rollback("1", liked = true, gen = old))
        assertEquals(false, s.resolve("1", null))
    }

    @Test
    fun `failed tap then next sync reflects the server`() {
        val s = LikeState()
        s.merge(setOf(), complete = true)
        val gen = s.userLike("1")
        s.rollback("1", liked = true, gen = gen)
        s.merge(setOf(), complete = true)
        assertEquals(false, s.resolve("1", null))
    }

    @Test
    fun `late sync keeps a tap newer than the snapshot`() {
        val s = LikeState()
        s.merge(setOf(), complete = true)
        s.userLike("9")
        s.merge(setOf(), complete = true)
        assertEquals(true, s.resolve("9", null))
    }

    @Test
    fun `reset clears all and stales in-flight generations`() {
        val s = LikeState()
        s.merge(setOf("1"), complete = true)
        val gen = s.userLike("2")
        s.reset()
        assertFalse(s.fullyLoaded)
        // Memory is gone so absence is unknown; the inline flag still answers.
        assertNull(s.resolve("1", null))
        assertEquals(true, s.resolve("1", true))
        assertNull(s.rollback("2", liked = true, gen = gen))
    }
}
