package dev.brahmkshatriya.echo.ui.player

/**
 * Pure decision math for the full player's backward edge swipe (page 0, previous track).
 *
 * Framework-free on purpose, mirroring [dev.brahmkshatriya.echo.ui.feed.SwipeToQueue]:
 * the touch listener in PlayerFragment calls these so the behavior is tested, not copied.
 * Covered by BackSwipeMathTest. What cannot run on JVM (ViewPager2 drags need Android)
 * stays device-verified: the finger-follow tiling against the preview, the joint
 * exit animation, and the fade handoff onto the rebound track.
 *
 * Sign convention: dx is the raw finger displacement from ACTION_DOWN (positive = right).
 * dir is the exit direction (+1 LTR, -1 RTL). backwards is dx folded onto the swipe
 * direction, so every threshold below reads positive in both layouts.
 */
internal object BackSwipeMath {

    /** dx folded onto the swipe direction: positive means "toward the previous track". */
    fun backwards(dx: Float, isRtl: Boolean): Float = if (isRtl) -dx else dx

    /** Arm the drag once the finger clearly heads backward, not down toward the sheet. */
    fun shouldArm(backwards: Float, dyAbs: Float, slop: Float): Boolean =
        backwards > slop && backwards > dyAbs

    /** Commit condition at release: far enough and still horizontal-dominant. */
    fun isQualified(backwards: Float, dyAbs: Float, threshold: Float): Boolean =
        backwards > threshold && backwards > dyAbs

    /** Current page follow: 1:1 with the finger, clamped to the exit side. */
    fun clampDrag(dx: Float, width: Float, dir: Float): Float =
        if (dir > 0f) dx.coerceIn(0f, width) else dx.coerceIn(-width, 0f)

    /**
     * Preview (incoming previous page) position for the same dx: at rest it waits one
     * full width off-screen on the entry side, and it lands at 0 exactly when the
     * finger has travelled one width — the same tiling a native ViewPager2 scroll draws.
     */
    fun previewTx(dx: Float, dir: Float, width: Float): Float = dx - dir * width

    /** Finger reversed into forward territory past slop: hand the stream to native. */
    fun isReversed(backwards: Float, slop: Float): Boolean = backwards < -slop

    /** Exit edge the current page animates to on commit. */
    fun exitTx(dir: Float, width: Float): Float = dir * width
}
