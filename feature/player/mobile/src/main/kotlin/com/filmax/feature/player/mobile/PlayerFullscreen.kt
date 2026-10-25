// Полноэкранный режим мобильного плеера: скрытые системные панели и альбомная ориентация на всё
// время экрана, отступы контролов от выреза камеры.

package com.filmax.feature.player.mobile

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import com.filmax.core.ui.components.HideSystemBars
import com.filmax.core.ui.components.LockLandscape

/**
 * Отступы контролов поверх видео. Системные панели скрыты, и safeDrawing сводится к вырезу
 * камеры: в альбомной он сбоку, и без отступа кнопки заезжали бы под него.
 */
internal val OverlayTopInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

internal val OverlayBottomInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)

/**
 * Плеер — альбомный экран без системных панелей, как в макете: видео 16:9 в портрете занимало бы
 * треть экрана, а с выключенным автоповоротом в альбомной было бы не посмотреть вовсе. На выходе
 * ориентация отдаётся системе, а при переходе на следующую серию держится: новый экран плеера
 * входит раньше, чем уходит старый, и снимать блокировку должен последний из них.
 */
@Composable
internal fun PlayerFullscreenEffects() {
    HideSystemBars()
    LockLandscape()
}
