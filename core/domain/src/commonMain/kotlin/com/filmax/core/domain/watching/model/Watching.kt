package com.filmax.core.domain.watching.model

data class WatchHistory(
    val itemId: Int,
    val title: String,
    val posterSmall: String?,
    val progress: WatchProgress?,
)

data class WatchProgress(
    val status: Int,
    val timeSeconds: Int?,
    val durationSeconds: Int?,
    val videoId: Int?,
    val season: Int?,
) {
    val fraction: Float
        get() {
            val t = timeSeconds ?: return 0f
            val d = durationSeconds?.takeIf { it > 0 } ?: return 0f
            return (t.toFloat() / d).coerceIn(0f, 1f)
        }
}

data class Notification(
    val id: Int,
    val title: String?,
    val text: String?,
    val createdAt: Long?,
    val read: Boolean,
    val itemId: Int?,
)
