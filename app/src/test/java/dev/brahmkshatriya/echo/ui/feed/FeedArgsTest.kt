package dev.brahmkshatriya.echo.ui.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedArgsTest {

    @Test
    fun `arguments win over activity vm`() {
        assertEquals(
            Pair("deezer", "liked_123"),
            FeedArgs.resolveFeedIds("deezer", "liked_123", "unified", "other"),
        )
    }

    @Test
    fun `activity vm is fallback when arguments are missing`() {
        assertEquals(
            Pair("deezer", "liked_123"),
            FeedArgs.resolveFeedIds(null, null, "deezer", "liked_123"),
        )
    }

    @Test
    fun `partial arguments mix with activity vm`() {
        assertEquals(
            Pair("deezer", "liked_123"),
            FeedArgs.resolveFeedIds("deezer", null, "unified", "liked_123"),
        )
    }

    @Test
    fun `both missing stays missing instead of throwing`() {
        assertEquals(
            Pair(null, null),
            FeedArgs.resolveFeedIds(null, null, null, null),
        )
    }

    @Test
    fun `restored fragment after process kill keeps routing ids`() {
        // Lock -> process kill -> return: activity VM is fresh (nulls), arguments survive.
        val (extId, feedId) = FeedArgs.resolveFeedIds("deezer", "liked_123", null, null)
        assertTrue(FeedArgs.hasRoutingIds(extId, feedId))
    }

    @Test
    fun `missing ids are detected before extension lookup`() {
        assertFalse(FeedArgs.hasRoutingIds(null, "liked_123"))
        assertFalse(FeedArgs.hasRoutingIds("deezer", null))
        assertFalse(FeedArgs.hasRoutingIds(null, null))
        assertFalse(FeedArgs.hasRoutingIds("", "liked_123"))
    }
}
