package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.exportSettings
import dev.brahmkshatriya.echo.utils.importSettings
import dev.brahmkshatriya.echo.utils.ui.prefs.FilePickerPrefs.FilePreference
import dev.brahmkshatriya.echo.utils.ui.prefs.FilePickerPrefs.exportPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.FilePickerPrefs.importPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.SwitchLongClickPreference
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SettingsOtherFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.other_settings)
    override val icon get() = R.drawable.ic_more_horiz.toResourceImageHolder()
    override val creator = { OtherPreference() }

    class OtherPreference : PreferenceFragmentCompat() {

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            configure()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = SETTINGS_NAME
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            SwitchLongClickPreference(context).apply {
                title = getString(R.string.check_for_updates)
                summary = getString(R.string.check_for_updates_summary)
                key = "check_for_updates"
                layoutResource = R.layout.preference_switch
                isIconSpaceReserved = false
                setDefaultValue(true)
                screen.addPreference(this)
                setOnLongClickListener {
                    val viewModel by activityViewModel<ExtensionsViewModel>()
                    viewModel.update(requireActivity(), true)
                }
            }

            exportPreference(
                context,
                FilePreference(
                    key = "export",
                    title = getString(R.string.export_settings),
                    summary = getString(R.string.export_settings_summary)
                ),
                fileName = { "echo-settings.json" }
            ) { uri ->
                uri?.let { context.exportSettings(it) }
            }.also { screen.addPreference(it) }

            importPreference(
                context,
                FilePreference(
                    key = "import",
                    title = getString(R.string.import_settings),
                    summary = getString(R.string.import_settings_summary)
                )
            ) {
                it?.let {
                    if (context.importSettings(it)) requireActivity().recreate()
                    else createSnack(R.string.invalid_settings_file)
                }
            }.also { screen.addPreference(it) }
        }
    }
}