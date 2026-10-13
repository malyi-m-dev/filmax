// Подписи ТВ-плеера — чистые функции без UI: строка под названием, остаток времени, вкладки панели
// и плашки состояния, подписи серий и карточки «Далее». Варианты вкладок — в TvPlayerOptions.
package com.filmax.feature.player.tv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.SubtitleOption
import com.filmax.feature.player.common.formatPlayerTime

/** `watching.status` kino.pub: 1 — досмотрено. */
private const val WATCH_STATUS_FINISHED = 1
private const val SECONDS_IN_MINUTE = 60

/**
 * Строка под названием: «Сезон 4 · Серия 7 · Тайный совет» у сериала, «год · качество» у фильма.
 * [paused] — открыта панель и видео стоит: вместо названия серии — «на паузе».
 */
internal fun playerSubtitle(state: PlayerState, paused: Boolean): String {
    val track = state.track ?: return ""
    val isEpisode = track.seasonNumber > 0 || state.item?.tracklist.orEmpty().size > 1
    val head = when {
        track.seasonNumber > 0 -> listOf("Сезон ${track.seasonNumber}", "Серия ${track.number}")
        isEpisode -> listOf("Серия ${track.number}")
        else -> listOfNotNull(state.item?.year?.takeIf { it > 0 }?.toString(), state.currentQuality)
    }
    val tail = when {
        paused -> "на паузе"
        isEpisode -> track.title
        else -> ""
    }
    return (head + tail).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Остаток до конца: «−21:48». Длительность неизвестна — пусто, а не «−0:00». */
internal fun remainingLabel(positionMs: Long, durationMs: Long): String =
    if (durationMs <= 0) "" else "−" + formatPlayerTime((durationMs - positionMs).coerceAtLeast(0))

internal val PlayerTab.label: String
    get() = when (this) {
        PlayerTab.Episodes -> "Серии"
        PlayerTab.Audio -> "Аудио"
        PlayerTab.Subtitles -> "Субтитры"
        PlayerTab.Quality -> "Качество"
        PlayerTab.Speed -> "Скорость"
    }

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

/**
 * Плашка состояния в ряду транспорта: «Субтитры», «Аудио · RU», «1080p», «Серии». Скорости среди
 * плашек нет: обычная скорость — не состояние, о котором стоит напоминать.
 */
internal fun statusLabel(tab: PlayerTab, state: PlayerState): String? = when (tab) {
    PlayerTab.Episodes -> tab.label
    PlayerTab.Audio -> listOfNotNull(tab.label, state.selectedAudio?.languageCode?.let(::languageShort))
        .joinToString(" · ")
    PlayerTab.Subtitles -> tab.label
    PlayerTab.Quality -> state.currentQuality
    PlayerTab.Speed -> null
}

/** Текущее значение рядом с подписью вкладки: «RU · 5.1», «Выкл», «1080p», «1×». */
internal fun tabValue(tab: PlayerTab, state: PlayerState): String? = when (tab) {
    PlayerTab.Episodes -> null
    PlayerTab.Audio -> state.selectedAudio?.let(::audioValue)
    PlayerTab.Subtitles -> subtitleValue(state.subtitles.firstOrNull { it.label == state.currentSubtitle })
    PlayerTab.Quality -> state.currentQuality
    PlayerTab.Speed -> speedValue(state.currentSpeed)
}

private fun subtitleValue(option: SubtitleOption?): String? = when {
    option == null -> null
    option.lang == null -> "Выкл"
    else -> languageShort(option.lang)
}

/** Короткий код языка для подписи: «RU», «EN», «UK»; незнакомый — первые три буквы кода. */
internal fun languageShort(code: String?): String? = when (code?.lowercase()) {
    null, "" -> null
    "rus", "ru" -> "RU"
    "eng", "en" -> "EN"
    "ukr", "uk" -> "UK"
    else -> code.take(LANGUAGE_CODE_LENGTH).uppercase()
}

private const val LANGUAGE_CODE_LENGTH = 3

/** «7. Тайный совет»; без названия — «7. Серия 7»: у kino.pub названий серий часто нет. */
internal fun episodeTitle(episode: MediaTrack): String =
    "${episode.number}. ${episode.title.ifBlank { "Серия ${episode.number}" }}"

/**
 * Подпись под кадром серии: играющая — «Идёт · 34:50», досмотренная — «Просмотрено», остальные —
 * длительность. Длительности нет — подписи нет.
 */
internal fun episodeMeta(episode: MediaTrack, playing: Boolean, positionMs: Long): String? = when {
    playing -> "Идёт · ${formatPlayerTime(positionMs)}"
    episode.watchStatus == WATCH_STATUS_FINISHED -> "Просмотрено"
    episode.durationSeconds > 0 -> "${episode.durationSeconds / SECONDS_IN_MINUTE} мин"
    else -> null
}

/** Полоса просмотра серии: досмотренная — полная, начатая — доля, не начатая — без полосы. */
internal fun episodeProgress(episode: MediaTrack): Float? = when {
    episode.watchStatus == WATCH_STATUS_FINISHED -> 1f
    episode.watchedSeconds > 0 && episode.durationSeconds > 0 ->
        (episode.watchedSeconds.toFloat() / episode.durationSeconds).coerceIn(0f, 1f)
    else -> null
}

/** Название следующей серии в карточке «Далее»: «Серия 8 · Перевал». */
internal fun nextUpTitle(next: MediaTrack): String =
    listOf("Серия ${next.number}", next.title).filter { it.isNotBlank() }.joinToString(" · ")
