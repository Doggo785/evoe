@file:Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package dev.brahmkshatriya.echo.utils.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback

object ViewPager2Utils {

    fun ViewPager2.supportBottomSheetBehavior() {
        val recycler = getChildAt(0) as RecyclerView
        recycler.run {
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
    }

    // NOTE: the backward edge swipe (page 0 → previous track) used to live here as
    // onFirstPageBackSwipe. It moved to PlayerFragment.setupBackSwipe: showing the previous
    // cover needs the adapter's cover rect, the preview overlay and playerState.previous,
    // none of which belong in a ViewPager2 helper. This file keeps the pager behaviors
    // that are genuinely generic.

    fun ViewPager2.registerOnUserPageChangeCallback(
        listener: (position: Int, userInitiated: Boolean) -> Unit
    ) {
        var previousState: Int = -1
        var userScrollChange = false
        registerOnPageChangeCallback(object : OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                listener(position, userScrollChange)
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (previousState == ViewPager.SCROLL_STATE_DRAGGING &&
                    state == ViewPager.SCROLL_STATE_SETTLING
                ) {
                    userScrollChange = true
                } else if (previousState == ViewPager.SCROLL_STATE_SETTLING &&
                    state == ViewPager.SCROLL_STATE_IDLE
                ) {
                    userScrollChange = false
                }
                previousState = state
            }
        })
    }
}