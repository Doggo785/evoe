package dev.brahmkshatriya.echo.extensions

import dev.brahmkshatriya.echo.common.models.Track

// Shared MediaState fixtures: LikeVerificationTest and CachedLikeStateTest
// previously carried identical builders (flagged duplication).
internal fun testLoadedState(
    liked: Boolean?,
    extensionId: String = "deezer",
    trackId: String = "42",
    trackTitle: String = "Probe",
) = MediaState.Loaded(
    extensionId = extensionId,
    item = Track(id = trackId, title = trackTitle),
    isFollowed = null,
    followers = null,
    isSaved = true,
    isLiked = liked,
    isHidden = null,
    showRadio = false,
    showShare = false
)

internal fun testUnloadedState(
    extensionId: String = "deezer",
    trackId: String = "42",
    trackTitle: String = "Probe",
) = MediaState.Unloaded(
    extensionId = extensionId,
    item = Track(id = trackId, title = trackTitle)
)
