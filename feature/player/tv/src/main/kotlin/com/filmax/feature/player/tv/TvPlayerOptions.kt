// Варианты во вкладках нижней панели — чистые функции без UI: строки дорожек и субтитров,
// отметка выбранного, событие выбора, пояснение под вариантами и подписи плиток скорости.
package com.filmax.feature.player.tv

import com.filmax.core.domain.catalog.model.audioLanguageName
import com.filmax.feature.player.common.AudioOption
import com.filmax.feature.player.common.PlaybackSpeeds
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.SubtitleOption

/** Строка варианта в панели: название, пояснение под ним и значок формата справа. */
internal data class OptionText(val title: String, val detail: String? = null, val badge: String? = null)

/** «Русский · Многоголосый», под ним студия, справа — каналы («5.1»). */
internal fun audioOptionText(option: AudioOption): OptionText = OptionText(
    title = listOfNotNull(audioLanguageName(option.languageCode), option.voiceType).distinct().joinToString(" · "),
    detail = option.voiceAuthor,
    badge = channelsBadge(option.channels),
)

private fun channelsBadge(channels: Int): String? = when (channels) {
    MONO -> "1.0"
    STEREO -> "2.0"
    else -> surroundBadge(channels)
}

/** Объёмный звук «5.1» / «7.1»; моно, стерео и неизвестное — null. */
internal fun surroundBadge(channels: Int): String? = when (channels) {
    SURROUND_5_1 -> "5.1"
    SURROUND_7_1 -> "7.1"
    else -> null
}

private const val MONO = 1
private const val STEREO = 2
private const val SURROUND_5_1 = 6
private const val SURROUND_7_1 = 8

/** Выбранная дорожка — свойством: рядом с подписью вкладки и в плашке транспорта. */
internal val PlayerState.selectedAudio: AudioOption?
    get() = audioTracks.firstOrNull { it.label == currentAudio }

/**
 * Язык дорожки и объёмный звук: «RU · 5.1». Стерео не пишем — это норма, а не выбор, и kino.pub
 * подставляет 2 канала, когда не знает точно.
 */
internal fun audioValue(option: AudioOption): String? =
    listOfNotNull(languageShort(option.languageCode), surroundBadge(option.channels))
        .joinToString(" · ")
        .ifEmpty { null }

internal fun subtitleOptionText(option: SubtitleOption): OptionText =
    OptionText(title = if (option.lang == null) "Выключены" else option.label)

/** Вариант во вкладке выбора: [key] — подпись, по которой модель его выбирает. */
internal data class OptionItem(val key: String, val text: OptionText, val selected: Boolean)

/** Варианты вкладки с отметкой выбранного; у «Серий» и «Скорости» свой вид — для них пусто. */
internal fun optionItems(tab: PlayerTab, state: PlayerState): List<OptionItem> = when (tab) {
    PlayerTab.Audio -> state.audioTracks.map { option ->
        OptionItem(option.label, audioOptionText(option), option.label == state.currentAudio)
    }
    PlayerTab.Subtitles -> state.subtitles.map {
        OptionItem(it.label, subtitleOptionText(it), it.label == state.currentSubtitle)
    }
    PlayerTab.Quality -> state.qualities.map {
        OptionItem(it.label, OptionText(it.label), it.label == state.currentQuality)
    }
    PlayerTab.Episodes, PlayerTab.Speed -> emptyList()
}

/** Событие выбора варианта вкладки: дорожка меняется сразу, панель остаётся открытой. */
internal fun optionEvent(tab: PlayerTab, key: String): PlayerEvent? = when (tab) {
    PlayerTab.Audio -> PlayerEvent.SelectAudio(key)
    PlayerTab.Subtitles -> PlayerEvent.SelectSubtitle(key)
    PlayerTab.Quality -> PlayerEvent.SelectQuality(key)
    PlayerTab.Episodes, PlayerTab.Speed -> null
}

/**
 * Пояснение под вариантами — только там, где поведение неочевидно: озвучка запоминается на весь
 * тайтл, скорость живёт до конца серии (она сессионная).
 */
internal fun tabNote(tab: PlayerTab, series: Boolean): String? = when (tab) {
    PlayerTab.Audio -> if (series) "Выбор запоминается для всего сериала" else "Выбор запоминается для этого фильма"
    // Тон сохраняет сам плеер (setPlaybackSpeed меняет только темп) — об этом и говорим.
    PlayerTab.Speed -> if (series) {
        "Скорость действует до конца серии, звук без изменения тона"
    } else {
        "Скорость действует до выхода из плеера, звук без изменения тона"
    }
    PlayerTab.Episodes, PlayerTab.Subtitles, PlayerTab.Quality -> null
}

/**
 * Скорости на ТВ — шесть плиток от 0.5× до 2×, как в макете: крайние 0.25× и 1.75× на пульте почти
 * не выбирают, а восемь плиток в ряд панели не помещаются крупно.
 */
internal val TvPlaybackSpeeds: List<Float> = listOf(0.5f, 0.75f, PlaybackSpeeds.NormalSpeed, 1.25f, 1.5f, 2f)

/** Значение плитки скорости: «1×» вместо «Обычная» — в ряду чисел слово выбивалось бы. */
internal fun speedValue(speed: Float): String =
    if (speed == PlaybackSpeeds.NormalSpeed) "1×" else PlaybackSpeeds.labelFor(speed)

/** Подпись под плиткой: только у ориентиров — обычной скорости и самой быстрой. */
internal fun speedCaption(speed: Float): String? = when (speed) {
    PlaybackSpeeds.NormalSpeed -> "обычная"
    PlaybackSpeeds.options.last().value -> "быстро"
    else -> null
}
