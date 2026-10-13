// Транспорт ТВ-плеера: что делает клавиша пульта, разгон перемотки, отсчёт «Далее» и состояние
// оверлея поверх кадра. Решения — чистые функции, класс состояния их только применяет.
package com.filmax.feature.player.tv

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.media3.common.Player

/** Тик прогресса и сохранения позиции (мс). Ровно секунда: на нём же держится SaveProgress и отсчёт «Далее». */
internal const val PROGRESS_TICK_MS = 1000L

/** Бездействие, после которого контролы уходят с кадра (на паузе не уходят). */
internal const val OVERLAY_AUTO_HIDE_MS = 5000L

/** Пауза в нажатиях, после которой перемотка подтверждается seekTo: seekTo на каждый шаг рвал бы HLS-буфер. */
internal const val SCRUB_COMMIT_TIMEOUT_MS = 700L

/** Карточка «Далее» появляется за 30 с до конца серии (прокси «пошли титры» — маркеров у kino.pub нет). */
internal const val AUTO_NEXT_WINDOW_MS = 30_000L

/** Отсчёт до автостарта следующей серии с момента появления карточки. */
internal const val AUTO_NEXT_COUNTDOWN_SEC = 15

private const val MILLIS_IN_SECOND = 1000L

/** Шаги разгона перемотки (секунды): каждые [SEEK_ACCELERATION_MS] удержания стрелки — следующий. */
internal val SEEK_STEPS_SEC = listOf(10, 30, 60)
internal const val SEEK_ACCELERATION_MS = 2000L

/**
 * Шаг перемотки при удержании стрелки [heldMs] миллисекунд: 10 → 30 → 60 с каждые 2 с. Отдельные
 * нажатия — всегда по 10 с: точная подстройка не должна разгоняться сама.
 */
internal fun seekStepSeconds(heldMs: Long): Int =
    SEEK_STEPS_SEC[(heldMs / SEEK_ACCELERATION_MS).toInt().coerceIn(0, SEEK_STEPS_SEC.lastIndex)]

/**
 * Что транспорт делает по клавише (нижняя панель закрыта). [repeats] — повторять ли команду, пока
 * клавишу держат: перемотка разгоняется, а пауза и панель срабатывают один раз на нажатие.
 */
internal enum class TransportCommand(val repeats: Boolean = false) {
    SeekBack(repeats = true),
    SeekForward(repeats = true),
    TogglePlay,
    OpenPanel,
    Peek,
    PlayNext,
}

/**
 * Раскладка пульта по гайдлайну Google для плеера: ←/→ — перемотка (воспроизведение не
 * останавливают), OK — пауза, ↓ — панель настроек и серий, ↑ — показать контролы, не трогая
 * воспроизведение. Пока видна карточка «Далее», OK запускает следующую серию. Чужие клавиши
 * (громкость, системные) — null: их плеер не трогает.
 */
internal fun transportCommand(key: Key, nextUpVisible: Boolean): TransportCommand? = when (key) {
    Key.DirectionLeft, Key.MediaRewind -> TransportCommand.SeekBack
    Key.DirectionRight, Key.MediaFastForward -> TransportCommand.SeekForward
    Key.DirectionCenter, Key.Enter, Key.NumPadEnter ->
        if (nextUpVisible) TransportCommand.PlayNext else TransportCommand.TogglePlay
    Key.MediaPlayPause -> TransportCommand.TogglePlay
    Key.DirectionDown -> TransportCommand.OpenPanel
    Key.DirectionUp -> TransportCommand.Peek
    else -> null
}

/** Карточка «Далее»: видна ли, сколько секунд до автостарта и отменил ли её зритель «Назад». */
internal data class AutoNext(val visible: Boolean = false, val secondsLeft: Int = 0, val dismissed: Boolean = false) {
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

/**
 * Состояние транспорта поверх кадра: видимость контролов, зеркала плеера (сам Player не
 * Compose-state), перемотка и карточка «Далее». Позицию читают узко — полоса перемотки в фазе
 * отрисовки, время — отдельной строкой: тик раз в секунду не перекомпоновывает экран.
 */
@Stable
internal class TvTransportState(private val player: Player) {

    var visible by mutableStateOf(true)
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var bufferedMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)

    /** Перемотка: позиция, которую двигает пульт до подтверждения seekTo. */
    var isScrubbing by mutableStateOf(false)
        private set
    var scrubTargetMs by mutableLongStateOf(0L)
        private set

    var autoNext by mutableStateOf(AutoNext())

    /** Счётчик действий пользователя — перезапускает таймер автоскрытия. */
    var interactionTick by mutableIntStateOf(0)
        private set

    /** Когда зажали стрелку перемотки — от этого считается разгон. */
    private var holdStartedAtMs = 0L

    /** Позиция, которую показывает полоса: при перемотке — цель, иначе — плеер. */
    val shownPositionMs: Long get() = if (isScrubbing) scrubTargetMs else positionMs

    /** Контролы уходят сами, только пока видео идёт и пульт не перематывает. */
    val idleHides: Boolean get() = visible && isPlaying && !isScrubbing

    /** Было действие пользователя: контролы на экран, таймер автоскрытия — с нуля. */
    fun touch() {
        visible = true
        interactionTick++
    }

    /**
     * Шаг перемотки по нажатию стрелки. [repeatCount] и [eventTimeMs] — из события пульта: первое
     * событие удержания (repeatCount 0) запоминает начало, следующие считают разгон от него.
     */
    fun seek(direction: Int, repeatCount: Int, eventTimeMs: Long) {
        val duration = durationMs
        if (duration <= 0) return
        if (repeatCount == 0) holdStartedAtMs = eventTimeMs
        if (!isScrubbing) {
            scrubTargetMs = positionMs
            isScrubbing = true
        }
        val stepMs = seekStepSeconds(eventTimeMs - holdStartedAtMs) * MILLIS_IN_SECOND
        scrubTargetMs = (scrubTargetMs + direction * stepMs).coerceIn(0L, duration)
        touch()
    }

    /** Подтверждение перемотки: плеер уходит на выбранную позицию. */
    fun commitScrub() {
        if (!isScrubbing) return
        player.seekTo(scrubTargetMs)
        positionMs = scrubTargetMs
        isScrubbing = false
    }

    fun togglePlay() {
        // Незакоммиченную перемотку не теряем: сначала на выбранную позицию, потом переключаем.
        commitScrub()
        if (player.isPlaying) player.pause() else player.play()
        touch()
    }

    /** «Назад» при карточке «Далее» — отмена: серия дотечёт до конца и остановится. */
    fun dismissAutoNext(): Boolean {
        if (!autoNext.visible) return false
        autoNext = autoNext.copy(visible = false, dismissed = true)
        touch()
        return true
    }
}
