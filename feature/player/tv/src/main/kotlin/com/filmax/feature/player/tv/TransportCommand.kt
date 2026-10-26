// Раскладка пульта в транспорте ТВ-плеера: какая клавиша что значит. Решение — чистая функция,
// исполняет его экран событиями модели.
package com.filmax.feature.player.tv

import androidx.compose.ui.input.key.Key

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
