package dev.brahmkshatriya.echo.playback.listener

import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.extensions.testLoadedState
import dev.brahmkshatriya.echo.extensions.testUnloadedState
import dev.brahmkshatriya.echo.utils.Serializer
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rules behind the post-start like verification (grey heart until the likes
 * call converges). Pure state math — no player needed.
 */
class LikeVerificationTest {

    private fun loaded(liked: Boolean?) = testLoadedState(liked)

    private fun unloaded() = testUnloadedState()

    @Test
    fun `unknown resolved state verifies`() {
        assertTrue(shouldVerifyLikeState(loaded(null)))
    }

    @Test
    fun `definitive states are left alone`() {
        assertFalse(shouldVerifyLikeState(loaded(true)))
        assertFalse(shouldVerifyLikeState(loaded(false)))
    }

    @Test
    fun `unresolved state waits for ready`() {
        assertFalse(shouldVerifyLikeState(unloaded()))
        assertFalse(shouldVerifyLikeState(null))
    }

    @Test
    fun `verified like flips isLiked and keeps the rest`() {
        val before = loaded(null)
        val after = before.withVerifiedLike(true)
        assertTrue(after.isLiked == true)
        assertEquals(before.item, after.item)
        assertEquals(before.extensionId, after.extensionId)
        assertEquals(before.isSaved, after.isSaved)
    }

    @Test
    fun `verified unlike flips back`() {
        assertTrue(loaded(null).withVerifiedLike(false).isLiked == false)
    }

    // The timeline extras reader is polymorphic (MediaItemUtils.getState reads
    // MediaState<Track>): what applyVerifiedLike writes must decode as the supertype.
    // A concrete Loaded encoding carries no discriminator and decodes to null, which
    // crashed applyCurrent on every verified track (2026-10-08).
    @Test
    fun `verified state written as supertype survives the timeline read`() {
        val updated: MediaState<Track> = loaded(null).withVerifiedLike(true)
        val decoded = with(Serializer) { updated.toJson().toData<MediaState<Track>>() }
            .getOrThrow() as MediaState.Loaded<Track>
        assertTrue(decoded.isLiked == true)
    }

    @Test
    fun `concrete-shaped json is unreadable as supertype - never write that to extras`() {
        val concreteJson = with(Serializer) { loaded(null).withVerifiedLike(true).toJson() }
        assertTrue(with(Serializer) { concreteJson.toData<MediaState<Track>>() }.isFailure)
    }
}
