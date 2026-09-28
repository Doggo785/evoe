@file:Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package dev.brahmkshatriya.echo.utils.ui

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import kotlin.math.abs

object ViewPager2Utils {

    fun ViewPager2.supportBottomSheetBehavior() {
        val recycler = getChildAt(0) as RecyclerView
        recycler.run {
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
    }

    /**
     * Calls [onSwipe] when the user drags backwards while already on the FIRST page.
     * ViewPager2 reports nothing for that gesture: at the start edge there is no page
     * to scroll to, overscroll is off and nested scrolling is disabled, so there is no
     * scroll, no fraction and no page selection. The forward-swipe guard in PlayerFragment
     * lives in the page-change callback and is never reached from here, so the two cannot
     * collide.
     *
     * Observe-only: onInterceptTouchEvent always returns false, so this listener can never
     * latch the gesture stream, starve ViewPager2's own drag handling, or steal vertical
     * drags from the BottomSheet. Registration order is therefore irrelevant.
     *
     * Horizontal-dominant with a threshold, so a vertical drag still belongs to the sheet
     * and a tap still reaches the panel. Fires on ACTION_UP, not mid-gesture: acting while
     * the finger is down would churn the queue and page position underneath the drag.
     * ACTION_CANCEL clears without firing. Does not touch currentItem: the page position
     * keeps its single writer.
     */
    fun ViewPager2.onFirstPageBackSwipe(onSwipe: () -> Unit) {
        val recycler = getChildAt(0) as? RecyclerView ?: return
        val threshold = ViewConfiguration.get(context).scaledPagingTouchSlop * 2
        recycler.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private var downX = 0f
            private var downY = 0f
            private var qualified = false

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.x
                        downY = e.y
                        qualified = false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (currentItem != 0) {
                            qualified = false
                            return false
                        }
                        val dx = e.x - downX
                        val dy = e.y - downY
                        // dx > 0 is a drag to the RIGHT, reaching for the page before this one.
                        // RTL is handled by ViewPager2's own layout direction.
                        val backwards = if (rv.layoutDirection == View.LAYOUT_DIRECTION_RTL) -dx else dx
                        qualified = backwards > threshold && backwards > abs(dy)
                    }

                    MotionEvent.ACTION_UP -> {
                        val fire = qualified && currentItem == 0
                        qualified = false
                        if (fire) onSwipe()
                    }

                    MotionEvent.ACTION_CANCEL -> qualified = false
                }
                return false
            }
        })
    }

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