package dev.brahmkshatriya.echo.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogAppUpdateBinding
import dev.brahmkshatriya.echo.utils.AppUpdater.PendingAppUpdate
import dev.brahmkshatriya.echo.utils.ContextUtils.appVersion
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Asks whether to install an available app update, showing what it adds.
 * Reports back through [ExtensionsViewModel.appUpdateDecided]; anything but the
 * update button — Later, the close icon, tapping outside — counts as declined,
 * and the update is re-offered on the next check.
 */
class AppUpdateBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(pending: PendingAppUpdate) = AppUpdateBottomSheet().apply {
            arguments = Bundle().apply {
                putString("tag", pending.tag)
                putStringArrayList("tags", ArrayList(pending.notes.map { it.tag }))
                putStringArrayList("bodies", ArrayList(pending.notes.map { it.body.orEmpty() }))
            }
        }
    }

    private var binding by autoCleared<DialogAppUpdateBinding>()
    private val viewModel by activityViewModel<ExtensionsViewModel>()

    private val args by lazy { requireArguments() }
    private var install = false

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = DialogAppUpdateBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        val tag = args.getString("tag").orEmpty()
        binding.updateDetails.text = getString(
            R.string.app_update_to_x, appVersion().substringBefore('_'), tag
        )
        binding.updateNotes.text = buildNotes()
        binding.installButton.setOnClickListener {
            install = true
            dismiss()
        }
        binding.laterButton.setOnClickListener { dismiss() }
    }

    private fun buildNotes(): String {
        val tags = args.getStringArrayList("tags").orEmpty()
        val bodies = args.getStringArrayList("bodies").orEmpty()
        val fallback = getString(R.string.app_update_no_notes)
        return buildString {
            tags.forEachIndexed { index, release ->
                val body = bodies.getOrNull(index).orEmpty().ifBlank { fallback }
                appendLine(release)
                appendLine(body)
                appendLine()
            }
        }.trimEnd()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.appUpdateDecided(args.getString("tag").orEmpty(), install)
    }
}
