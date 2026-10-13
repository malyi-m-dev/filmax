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
    /**
     * Когда тайтл смотрели последний раз, мс от эпохи (`last_seen` в `api/v1/history`). По нему
     * «Моё» группирует историю по дням; null — сервер время не прислал, запись уходит в «Раньше».
     */
    val lastSeenAt: Long? = null,
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
            val watched = timeSeconds ?: return 0f
            val duration = durationSeconds?.takeIf { it > 0 } ?: return 0f
            return (watched.toFloat() / duration).coerceIn(0f, 1f)
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
