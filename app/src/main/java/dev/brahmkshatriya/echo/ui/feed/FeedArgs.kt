package dev.brahmkshatriya.echo.ui.feed

object FeedArgs {
    fun resolveFeedIds(
        argExtensionId: String?,
        argFeedId: String?,
        activityExtensionId: String?,
        activityFeedId: String?,
    ): Pair<String?, String?> = Pair(
        argExtensionId ?: activityExtensionId,
        argFeedId ?: activityFeedId,
    )

    fun hasRoutingIds(extensionId: String?, feedId: String?): Boolean =
        !extensionId.isNullOrEmpty() && !feedId.isNullOrEmpty()
}
