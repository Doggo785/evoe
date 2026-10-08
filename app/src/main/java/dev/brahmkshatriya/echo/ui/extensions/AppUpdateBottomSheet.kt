package dev.brahmkshatriya.echo.ui.extensions

import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogAppUpdateBinding
import dev.brahmkshatriya.echo.databinding.ItemAppUpdateReleaseBinding
import dev.brahmkshatriya.echo.utils.AppUpdater.PendingAppUpdate
import dev.brahmkshatriya.echo.utils.ContextUtils.appVersion
import dev.brahmkshatriya.echo.utils.ReleaseNotes
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Asks whether to install an available app update, showing what it adds.
 * Each skipped release gets its own card with its notes styled natively.
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
        showReleases(tag)
        binding.installButton.setOnClickListener {
            install = true
            dismiss()
        }
        binding.laterButton.setOnClickListener { dismiss() }
    }

    private fun showReleases(tag: String) {
        val tags = args.getStringArrayList("tags").orEmpty()
        val bodies = args.getStringArrayList("bodies").orEmpty()
        val releases = tags.mapIndexed { index, release ->
            release to bodies.getOrNull(index).orEmpty()
        }.ifEmpty { listOf(tag to "") }
        val container = binding.releasesContainer
        container.removeAllViews()
        releases.forEach { (release, body) ->
            val card = ItemAppUpdateReleaseBinding.inflate(layoutInflater, container, false)
            val date = ReleaseNotes.date(body)
            card.releaseTitle.text = date?.let {
                getString(R.string.app_update_release_on_x, release, it)
            } ?: release
            card.releaseNotes.text = buildSpanned(body)
            container.addView(card.root)
        }
    }

    private fun buildSpanned(body: String): CharSequence {
        val blocks = ReleaseNotes.blocks(body)
        if (blocks.isEmpty()) return getString(R.string.app_update_no_notes)
        val out = SpannableStringBuilder()
        blocks.forEach { block ->
            if (out.isNotEmpty()) out.append(if (block is ReleaseNotes.Block.SubHeader) "\n\n" else "\n")
            when (block) {
                is ReleaseNotes.Block.SubHeader ->
                    out.append(block.text, StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                is ReleaseNotes.Block.Bullet -> {
                    val start = out.length
                    if (block.scope != null) {
                        out.append(block.scope, StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        out.append(" ")
                    }
                    out.append(block.text)
                    out.setSpan(BulletSpan(), start, out.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                is ReleaseNotes.Block.Paragraph -> out.append(block.text)
            }
        }
        return out
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.appUpdateDecided(args.getString("tag").orEmpty(), install)
    }
}
