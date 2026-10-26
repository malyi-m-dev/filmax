// Транспорт плеера в терминах модели: снимок воспроизведения, разгон перемотки с пульта и отсчёт
// карточки «Далее». Чистые данные и функции — их же проверяют тесты.
package com.filmax.feature.player.common

/** Тик прогресса (мс). Ровно секунда: на нём держатся сохранение позиции и отсчёт «Далее». */
internal const val PROGRESS_TICK_MS = 1000L

/** Пауза в нажатиях, после которой перемотка подтверждается seekTo: seekTo на каждый шаг рвал бы HLS-буфер. */
internal const val SCRUB_COMMIT_TIMEOUT_MS = 700L

/** Карточка «Далее» появляется за 30 с до конца серии (прокси «пошли титры» — маркеров у kino.pub нет). */
internal const val AUTO_NEXT_WINDOW_MS = 30_000L

/** Отсчёт до автостарта следующей серии с момента появления карточки. */
const val AUTO_NEXT_COUNTDOWN_SEC = 15

/** Шаги разгона перемотки (секунды): каждые [SEEK_ACCELERATION_MS] удержания стрелки — следующий. */
internal val SEEK_STEPS_SEC = listOf(10, 30, 60)
internal const val SEEK_ACCELERATION_MS = 2000L

/**
 * Шаг перемотки при удержании стрелки [heldMs] миллисекунд: 10 → 30 → 60 с каждые 2 с. Отдельные
 * нажатия — всегда по 10 с: точная подстройка не должна разгоняться сама.
 */
fun seekStepSeconds(heldMs: Long): Int =
    SEEK_STEPS_SEC[(heldMs / SEEK_ACCELERATION_MS).toInt().coerceIn(0, SEEK_STEPS_SEC.lastIndex)]

/**
 * Что экран знает о воспроизведении: идёт ли видео, буферизуется ли, позиция, длительность и
 * загруженное. Зеркало ExoPlayer в состоянии: сам Player — не Compose-state.
 */
data class PlaybackSnapshot(
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
)

/** Карточка «Далее»: видна ли, сколько секунд до автостарта и отменил ли её зритель «Назад». */
data class AutoNext(val visible: Boolean = false, val secondsLeft: Int = 0, val dismissed: Boolean = false) {
    /** Доля прошедшего отсчёта — полоса на карточке. */
    val elapsed: Float get() = 1f - secondsLeft.toFloat() / AUTO_NEXT_COUNTDOWN_SEC

    /** Отсчёт дошёл до нуля — пора включать следующую серию. */
    val expired: Boolean get() = visible && secondsLeft <= 0

    /**
     * Тик раз в секунду. Карточка появляется в последних [AUTO_NEXT_WINDOW_MS] серии и с этого
     * момента идёт фиксированный отсчёт: конца титров не ждём. На паузе отсчёт замирает,
     * перемотка прячет карточку и начинает отсчёт заново. Отменённая «Назад» больше не появляется.
     * Нижней границы у окна нет: у HLS позиция бывает за заявленной длительностью.
     */
    fun tick(remainingMs: Long, enabled: Boolean, playing: Boolean, scrubbing: Boolean): AutoNext {
        val inWindow = enabled && !dismissed && !scrubbing && remainingMs <= AUTO_NEXT_WINDOW_MS
        return when {
            !inWindow -> copy(visible = false)
            !visible -> copy(visible = true, secondsLeft = AUTO_NEXT_COUNTDOWN_SEC)
            playing -> copy(secondsLeft = (secondsLeft - 1).coerceAtLeast(0))
            else -> this
        }
    }
}
