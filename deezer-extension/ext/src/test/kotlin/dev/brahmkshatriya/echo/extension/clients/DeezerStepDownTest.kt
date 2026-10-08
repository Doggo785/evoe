package dev.brahmkshatriya.echo.extension.clients

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step-down before fallback substitution (ported from upstream Gladix 2419867c).
 *
 * Pure decision logic: no network, no Android. The ladder flac -> 320 -> 128 and the
 * alternate-recording gate are what keep a region-locked acoustic from silently playing
 * the standard recording under the acoustic title.
 */
class DeezerStepDownTest {

    @Test
    fun `flac steps down to 320`() {
        assertEquals("320", DeezerTrackClient.stepDownQuality("flac"))
    }

    @Test
    fun `320 steps down to 128`() {
        assertEquals("128", DeezerTrackClient.stepDownQuality("320"))
    }

    @Test
    fun `128 is the bottom rung`() {
        assertNull(DeezerTrackClient.stepDownQuality("128"))
    }

    @Test
    fun `mp3 misc is a bottom rung`() {
        assertNull(DeezerTrackClient.stepDownQuality("mp3"))
    }

    @Test
    fun `unknown quality has nowhere to step`() {
        assertNull(DeezerTrackClient.stepDownQuality("opus"))
    }

    @Test
    fun `no fallback track is not an alternate recording`() {
        assertFalse(DeezerTrackClient.isAlternateRecording(null, "111"))
    }

    @Test
    fun `same id refetch is not an alternate recording`() {
        assertFalse(DeezerTrackClient.isAlternateRecording("111", "111"))
    }

    @Test
    fun `fallback id is an alternate recording`() {
        assertTrue(DeezerTrackClient.isAlternateRecording("222", "111"))
    }
}
