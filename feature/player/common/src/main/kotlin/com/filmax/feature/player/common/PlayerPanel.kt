// Нижняя панель ТВ-плеера в терминах модели: какие вкладки есть, какая открывается и что лежит
// во вкладке «Серии». Чистые функции — их же проверяют тесты.
package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.MediaTrack

/** Вкладка нижней панели — в порядке показа. */
enum class PlayerTab { Episodes, Audio, Subtitles, Quality, Speed }

/** Сезон в панели: номер (0 — у сериала без сезонов) и его серии по порядку. */
data class PlayerSeason(val number: Int, val episodes: List<MediaTrack>)

/**
 * Вкладки, в которых есть из чего выбрать: серии — у сериала, дорожки и качество — когда их больше
 * одной. Скорость есть всегда — набор фиксированный.
 */
fun PlayerState.panelTabs(): List<PlayerTab> = buildList {
    if (item?.tracklist.orEmpty().size > 1) add(PlayerTab.Episodes)
    if (audioTracks.size > 1) add(PlayerTab.Audio)
    if (subtitles.size > 1) add(PlayerTab.Subtitles)
    if (qualities.size > 1) add(PlayerTab.Quality)
    add(PlayerTab.Speed)
}

/**
 * Вкладка, на которой открывается панель: та, что была выбрана в прошлый раз (если она ещё есть),
 * иначе первая — у сериала это «Серии»: за панелью чаще всего идут переключить серию.
 */
fun PlayerState.openingTab(): PlayerTab {
    val tabs = panelTabs()
    return panelTab?.takeIf { it in tabs } ?: tabs.first()
}

/** Плейлист по сезонам: сезоны и серии внутри — по возрастанию номера. */
fun playerSeasons(tracks: List<MediaTrack>): List<PlayerSeason> = tracks
    .groupBy { it.seasonNumber }
    .toSortedMap()
    .map { (number, episodes) -> PlayerSeason(number, episodes.sortedBy { it.number }) }

/** Сезон, который сейчас листают во вкладке «Серии»; по умолчанию — сезон играющей серии. */
fun PlayerState.shownSeason(): PlayerSeason? {
    val seasons = playerSeasons(item?.tracklist.orEmpty())
    val number = panelSeason ?: track?.seasonNumber
    return seasons.firstOrNull { it.number == number } ?: seasons.firstOrNull()
}
