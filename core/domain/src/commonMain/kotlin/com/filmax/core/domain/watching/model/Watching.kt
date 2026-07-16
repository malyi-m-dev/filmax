package com.filmax.core.domain.watching.model

data class WatchHistory(
    val itemId: Int,
    val title: String,
    val posterSmall: String?,
    val progress: WatchProgress?,
    /** Широкий постер тайтла 16:9. */
    val posterWide: String? = null,
    /** Кадр конкретной серии 16:9 — точнее постера: показывает, на чём человек остановился. */
    val episodeThumbnail: String? = null,
) {
    /**
     * Картинка для карточки 16:9, по убыванию точности: кадр серии → широкий постер → обложка.
     * Вертикальная обложка 2:3 в широкой рамке обрезается по центру и превращается в кашу —
     * но лучше кривой кроп, чем пустой прямоугольник.
     */
    val wideOrPoster: String
        get() = episodeThumbnail?.takeIf { it.isNotBlank() }
            ?: posterWide?.takeIf { it.isNotBlank() }
            ?: posterSmall.orEmpty()
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
