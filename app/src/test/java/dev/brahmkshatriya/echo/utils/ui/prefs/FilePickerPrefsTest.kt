package dev.brahmkshatriya.echo.utils.ui.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Export filename shape. Pure JVM, no picker involved.
 * Pinned because the filename is the contract a previously exported file is found by.
 */
class FilePickerPrefsTest {

    @Test
    fun `extension export filename is lowercased echo type id settings`() {
        assertEquals(
            "echo-music-deezer-settings.json",
            FilePickerPrefs.extensionSettingsFileName("music", "Deezer")
        )
    }
}
