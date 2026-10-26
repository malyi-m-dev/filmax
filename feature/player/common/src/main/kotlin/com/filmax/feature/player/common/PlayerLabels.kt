// Подписи плеера — чистые функции без UI, общие для телефона и ТВ: строка под названием, значения
// вкладок панели, карточка «Далее», заголовок панели и подписи серий.
package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.MediaTrack

/** `watching.status` kino.pub: 1 — досмотрено. */
const val WATCH_STATUS_FINISHED = 1

/**
 * Строка под названием: «Сезон 4 · Серия 7 · Тайный совет» у сериала, «год · качество» у фильма.
 * [paused] — открыта панель и видео стоит: вместо названия серии — «на паузе».
 */
fun playerSubtitle(state: PlayerState, paused: Boolean): String {
    val track = state.track ?: return ""
    val isEpisode = track.seasonNumber > 0 || state.isSeries
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

/** «Название · S4E7» над вкладками панели телефона — что именно сейчас настраивают. */
fun panelTitle(state: PlayerState): String {
    val title = state.item?.title.orEmpty()
    val track = state.track ?: return title
    val code = episodeCode(track).takeIf { state.isSeries }
    return listOfNotNull(title, code).joinToString(" · ")
}

/** «S4E8» у серии с сезоном, «Серия 8» — без. */
fun episodeCode(track: MediaTrack): String =
    if (track.seasonNumber > 0) "S${track.seasonNumber}E${track.number}" else "Серия ${track.number}"

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

/** «7. Тайный совет»; без названия — «7. Серия 7»: у kino.pub названий серий часто нет. */
fun episodeTitle(episode: MediaTrack): String =
    "${episode.number}. ${episode.title.ifBlank { "Серия ${episode.number}" }}"

/** Длительность серии: «53 мин»; неизвестна — null. */
fun episodeLength(episode: MediaTrack): String? =
    (episode.durationSeconds / SECONDS_IN_MINUTE).takeIf { it > 0 }?.let { "$it мин" }

/** Полоса просмотра серии: досмотренная — полная, начатая — доля, не начатая — без полосы. */
fun episodeProgress(episode: MediaTrack): Float? = when {
    episode.watchStatus == WATCH_STATUS_FINISHED -> 1f
    episode.watchedSeconds > 0 && episode.durationSeconds > 0 ->
        (episode.watchedSeconds.toFloat() / episode.durationSeconds).coerceIn(0f, 1f)
    else -> null
}

private const val SECONDS_IN_MINUTE = 60
