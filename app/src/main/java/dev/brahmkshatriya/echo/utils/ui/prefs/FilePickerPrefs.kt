package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.utils.ui.UiUtils.hasCreateDocument
import dev.brahmkshatriya.echo.utils.ui.UiUtils.hasOpenDocument

/**
 * Export/import preferences backed by the system file picker.
 *
 * Launchers are registered here, at preference-build time (fragment init, before STARTED),
 * so results survive an activity recreation while the picker is open. The tap only launches,
 * inside an ActivityNotFoundException catch: a missing picker is the one failure explained here.
 * Preferences hide themselves where no handler exists (see UiUtils checks + manifest queries).
 */
object FilePickerPrefs {

    fun extensionSettingsFileName(type: String, id: String) =
        "echo-$type-$id-settings.json".lowercase()

    data class FilePreference(val key: String, val title: String, val summary: String)

    fun Fragment.exportPreference(
        context: Context,
        config: FilePreference,
        fileName: () -> String,
        onPick: Fragment.(Uri?) -> Unit
    ): TransitionPreference {
        val fragment = this
        val launcher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri -> fragment.onPick(uri) }
        return TransitionPreference(context).apply {
            key = config.key
            title = config.title
            summary = config.summary
            layoutResource = R.layout.preference
            isIconSpaceReserved = false
            isVisible = context.hasCreateDocument()
            setOnPreferenceClickListener {
                launchSpeaking(launcher, fileName())
                true
            }
        }
    }

    fun Fragment.importPreference(
        context: Context,
        config: FilePreference,
        onPick: Fragment.(Uri?) -> Unit
    ): TransitionPreference {
        val fragment = this
        val launcher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri -> fragment.onPick(uri) }
        return TransitionPreference(context).apply {
            key = config.key
            title = config.title
            summary = config.summary
            layoutResource = R.layout.preference
            isIconSpaceReserved = false
            isVisible = context.hasOpenDocument()
            setOnPreferenceClickListener {
                launchSpeaking(launcher, arrayOf("application/json"))
                true
            }
        }
    }

    private fun <I> Fragment.launchSpeaking(launcher: ActivityResultLauncher<I>, input: I) {
        try {
            launcher.launch(input)
        } catch (_: ActivityNotFoundException) {
            createSnack(R.string.no_file_picker)
        }
    }
}
