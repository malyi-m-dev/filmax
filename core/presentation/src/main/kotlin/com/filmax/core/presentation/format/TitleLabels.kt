// Подписи тайтлов — чистые функции без UI: их зовут и модели экранов (готовые подписи в
// состоянии), и компоненты. Живут в core:presentation, а не в core:ui: модель экрана не зависит
// от Compose-компонентов.
package com.filmax.core.presentation.format

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.watching.model.WatchProgress
import java.util.Locale

/**
 * Оценка для пилюли: в домене число вида 8.312, на экране нужен один знак.
 *
 * Ноль — это «оценки нет», а не «ноль баллов»: kino.pub отдаёт `0` для тайтлов без рейтинга,
 * и печатать «0.0» под постером — врать зрителю.
 */
fun ratingLabel(value: Double?): String? =
    value?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.1f", it) }

/** С какой оценки она «высокая» и выделяется цветом. */
private const val HIGH_RATING = 8.0

/** Высокая ли оценка — такую пилюля выделяет вторым акцентом. */
fun isHighRating(value: Double?): Boolean = (value ?: 0.0) >= HIGH_RATING

/** Собирает мету `тип · год`, пропуская пустые части. */
fun posterMeta(type: String?, year: Int): String? {
    val parts = buildList {
        if (!type.isNullOrBlank()) add(type)
        if (year > 0) add(year.toString())
    }
    return parts.joinToString(" · ").ifBlank { null }
}

/**
 * Тип тайтла под постером и в мете деталей. `serial`/`docuserial` из API зрителю не
 * показываем: у kino.pub это технические значения, а не то, как о тайтле говорят.
 */
fun typeLabel(type: ItemType): String = when (type) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY, ItemType.DOCUMENTARY_MOVIE -> "Документальный"
    ItemType.TV_SHOW -> "ТВ-шоу"
    ItemType.CONCERT -> "Концерт"
    ItemType.TV -> "ТВ"
}

/**
 * Подпись карточки «продолжить»: «S2 · осталось 18 мин». Одна на телефон и ТВ — раньше жила
 * тремя одинаковыми копиями (обе Главных и «Моё»). Номера серии в подписи нет: его показывает
 * плашка на кадре.
 */
fun continueMeta(progress: WatchProgress?): String? {
    if (progress == null) return null
    val parts = buildList {
        progress.season?.takeIf { it > 0 }?.let { add("S$it") }
        val remaining = remainingMinutes(progress)
        when {
            remaining != null -> add("осталось ${durationLabel(remaining)}")
            // Остатка нет, а прогресс есть — досмотрено: иначе карточка молчала бы о том,
            // почему она вообще в «Продолжить».
            progress.fraction > 0f -> add("просмотрено")
        }
    }
    return parts.joinToString(" · ").ifBlank { null }
}

/** Сколько минут осталось до конца трека; null — прогресса нет или уже досмотрено. */
private fun remainingMinutes(progress: WatchProgress): Int? {
    val watched = progress.timeSeconds
    val total = progress.durationSeconds?.takeIf { it > 0 }
    if (watched == null || total == null) return null
    return ((total - watched) / SECONDS_IN_MINUTE).takeIf { it > 0 }
}

/** «1 ч 20 мин» / «45 мин» — часы показываем, только когда они есть. */
fun durationLabel(totalMinutes: Int): String {
    val hours = totalMinutes / MINUTES_IN_HOUR
    val minutes = totalMinutes % MINUTES_IN_HOUR
    return when {
        hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
        hours > 0 -> "$hours ч"
        else -> "$minutes мин"
    }
}

/**
 * Постер подборки: сначала средний, затем большой. null — картинки нет вовсе, такую подборку
 * ряды не показывают (карточку держит изображение).
 */
fun Collection.posterUrl(): String? =
    posters?.let { it.medium.ifEmpty { it.big } }?.takeIf { it.isNotBlank() }

private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_MINUTE = 60
