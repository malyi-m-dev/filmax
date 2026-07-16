package com.filmax.core.domain.watching.model

data class WatchHistory(
    val itemId: Int,
    val title: String,
    val posterSmall: String?,
    val progress: WatchProgress?,
    /** Кадр 16:9 для широких карточек. Пусто — бэкенд его не отдал, тогда падаем на [posterSmall]. */
    val posterWide: String? = null,
) {
    /**
     * Картинка для карточки 16:9. Вертикальный постер 2:3 в такой рамке обрезается по центру
     * (от кадра остаётся случайная полоса), поэтому кадр предпочтительнее — но лучше кривой
     * кроп, чем пустой прямоугольник.
     */
    val wideOrPoster: String get() = posterWide?.takeIf { it.isNotBlank() } ?: posterSmall.orEmpty()
}

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
