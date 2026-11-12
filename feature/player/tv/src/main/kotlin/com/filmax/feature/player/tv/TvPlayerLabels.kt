// Подписи ТВ-плеера — чистые функции без UI: остаток времени, иконки вкладок, подсказки пульта.
// Общие с телефоном подписи (серии, вкладки, «Далее») и варианты вкладок — в player.common.
package com.filmax.feature.player.tv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.filmax.core.presentation.format.formatPlayerTime
import com.filmax.feature.player.common.PlayerTab

/** Остаток до конца: «−21:48». Длительность неизвестна — пусто, а не «−0:00». */
internal fun remainingLabel(positionMs: Long, durationMs: Long): String =
    if (durationMs <= 0) "" else "−" + formatPlayerTime((durationMs - positionMs).coerceAtLeast(0))

internal val PlayerTab.icon: ImageVector
    get() = when (this) {
        PlayerTab.Episodes -> Icons.Rounded.VideoLibrary
        PlayerTab.Audio -> Icons.Rounded.GraphicEq
        PlayerTab.Subtitles -> Icons.Rounded.Subtitles
        PlayerTab.Quality -> Icons.Rounded.Hd
        PlayerTab.Speed -> Icons.Rounded.Speed
    }

/** Что делают ←/→ во вкладке — подсказка внизу панели. */
internal val PlayerTab.moveHint: String
    get() = when (this) {
        PlayerTab.Episodes -> "серия"
        PlayerTab.Audio -> "дорожка"
        PlayerTab.Subtitles, PlayerTab.Quality -> "вариант"
        PlayerTab.Speed -> "скорость"
    }
