package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceGroup
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.utils.ui.UiUtils.hasCreateDocument
import dev.brahmkshatriya.echo.utils.ui.UiUtils.hasOpenDocument

/**
 * Export/import preferences backed by the system file picker.
 *
 * The launchers are registered by the CALLER, once, at fragment init (property
 * or onCreate) — never here, and never inside a flow collector. Registering
 * after the fragment is created throws IllegalStateException, which is exactly
 * what happened when registration lived at preference-build time inside an
 * observe block (crash opening the extension settings on every build). The tap
 * only launches, inside an ActivityNotFoundException catch: a missing picker
 * is the one failure explained here. Preferences hide themselves where no
 * handler exists (see UiUtils checks + manifest queries).
 */
object FilePickerPrefs {

    fun extensionSettingsFileName(type: String, id: String) =
        "echo-$type-$id-settings.json".lowercase()

    data class FilePreference(val key: String, val title: String, val summary: String)

    fun Fragment.exportPreference(
        context: Context,
        config: FilePreference,
        fileName: () -> String,
        launcher: ActivityResultLauncher<String>,
    ): TransitionPreference =
        buildFilePreference(context, config, context.hasCreateDocument()) {
            launchSpeaking(launcher, fileName())
        }

    fun Fragment.importPreference(
        context: Context,
        config: FilePreference,
        launcher: ActivityResultLauncher<Array<String>>,
    ): TransitionPreference =
        buildFilePreference(context, config, context.hasOpenDocument()) {
            launchSpeaking(launcher, arrayOf("application/json"))
        }

    fun Fragment.addFilePickerPreferences(
        screen: PreferenceGroup,
        context: Context,
        fileName: () -> String,
        exportLauncher: ActivityResultLauncher<String>,
        importLauncher: ActivityResultLauncher<Array<String>>,
    ) {
        exportPreference(
            context,
            FilePreference(
                key = "export",
                title = getString(R.string.export_settings),
                summary = getString(R.string.export_settings_summary)
            ),
            fileName = fileName,
            launcher = exportLauncher,
        ).also { screen.addPreference(it) }
        importPreference(
            context,
            FilePreference(
                key = "import",
                title = getString(R.string.import_settings),
                summary = getString(R.string.import_settings_summary)
            ),
            launcher = importLauncher,
        ).also { screen.addPreference(it) }
    }

    private fun buildFilePreference(
        context: Context,
        config: FilePreference,
        visible: Boolean,
        onTap: () -> Unit,
    ): TransitionPreference {
        return TransitionPreference(context).apply {
            key = config.key
            title = config.title
            summary = config.summary
            layoutResource = R.layout.preference
            isIconSpaceReserved = false
            isVisible = visible
            setOnPreferenceClickListener {
                onTap()
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
