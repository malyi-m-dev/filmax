// Нижняя панель плеера в терминах модели: вкладки и сезоны плейлиста. Какие вкладки есть, какая
// открывается и какой сезон листают — методы PlayerState.
package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.MediaTrack

/** Вкладка нижней панели — в порядке показа. */
enum class PlayerTab(val label: String) {
    Episodes("Серии"),
    Audio("Аудио"),
    Subtitles("Субтитры"),
    Quality("Качество"),
    Speed("Скорость"),
}

/** Сезон в панели: номер (0 — у сериала без сезонов) и его серии по порядку. */
data class PlayerSeason(val number: Int, val episodes: List<MediaTrack>)

/** Плейлист по сезонам: сезоны и серии внутри — по возрастанию номера. */
fun playerSeasons(tracks: List<MediaTrack>): List<PlayerSeason> = tracks
    .groupBy { it.seasonNumber }
    .toSortedMap()
    .map { (number, episodes) -> PlayerSeason(number, episodes.sortedBy { it.number }) }
