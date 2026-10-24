// Подписи плеера — чистые функции без UI, общие для телефона и ТВ: строка под названием, вкладки
// панели и их текущие значения, карточка «Далее».
package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.MediaTrack

/**
 * Строка под названием: «Сезон 4 · Серия 7 · Тайный совет» у сериала, «год · качество» у фильма.
 * [paused] — открыта панель и видео стоит: вместо названия серии — «на паузе».
 */
fun playerSubtitle(state: PlayerState, paused: Boolean): String {
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

val PlayerTab.label: String
    get() = when (this) {
        PlayerTab.Episodes -> "Серии"
        PlayerTab.Audio -> "Аудио"
        PlayerTab.Subtitles -> "Субтитры"
        PlayerTab.Quality -> "Качество"
        PlayerTab.Speed -> "Скорость"
    }

/**
 * Плашка состояния в ряду транспорта: «Субтитры», «Аудио · RU», «1080p», «Серии». Скорости среди
 * плашек нет: обычная скорость — не состояние, о котором стоит напоминать.
 */
fun statusLabel(tab: PlayerTab, state: PlayerState): String? = when (tab) {
    PlayerTab.Episodes -> tab.label
    PlayerTab.Audio -> listOfNotNull(tab.label, state.selectedAudio?.languageCode?.let(::languageShort))
        .joinToString(" · ")
    PlayerTab.Subtitles -> tab.label
    PlayerTab.Quality -> state.currentQuality
    PlayerTab.Speed -> null
}

/** Текущее значение рядом с подписью вкладки: «RU · 5.1», «Выкл», «1080p», «1×». */
fun tabValue(tab: PlayerTab, state: PlayerState): String? = when (tab) {
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
fun languageShort(code: String?): String? = when (code?.lowercase()) {
    null, "" -> null
    "rus", "ru" -> "RU"
    "eng", "en" -> "EN"
    "ukr", "uk" -> "UK"
    else -> code.take(LANGUAGE_CODE_LENGTH).uppercase()
}

private const val LANGUAGE_CODE_LENGTH = 3

/** Название следующей серии в карточке «Далее»: «Серия 8 · Перевал». */
fun nextUpTitle(next: MediaTrack): String =
    listOf("Серия ${next.number}", next.title).filter { it.isNotBlank() }.joinToString(" · ")
