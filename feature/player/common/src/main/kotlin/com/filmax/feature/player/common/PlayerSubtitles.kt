package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.catalog.model.subtitleLanguageName
import com.filmax.core.domain.playback.PlaybackSettings

/** Субтитры трека для меню: «Выкл» первым, дальше языки трека. */
internal fun subtitleOptions(subtitles: List<SubtitleTrack>): List<SubtitleOption> =
    listOf(SubtitleOption(PlaybackSettings.SubtitleOff, null)) +
        subtitles.map { SubtitleOption(subtitleLanguageName(it.lang), it.lang) }

/** Вариант по языку из настроек; «Выкл» или языка у трека нет — первый, то есть «Выкл». */
internal fun List<SubtitleOption>.preferred(language: String): SubtitleOption =
    firstOrNull { language != PlaybackSettings.SubtitleOff && it.label == language } ?: first()
