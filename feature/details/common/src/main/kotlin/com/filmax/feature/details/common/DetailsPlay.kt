// Главная кнопка деталей — что она играет и как подписана. Раньше это считали два экрана: телефон
// играл «продолжить», иначе первую серию сезона, иначе первую вообще, а ТВ — без последнего шага,
// и подписи у каждого были свои. Теперь цель и подписи одни, а ТВ и телефон берут каждый своё поле.
package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.core.domain.catalog.model.ref
import com.filmax.core.presentation.format.durationLabel

private const val SECONDS_IN_MINUTE = 60

/** Главная кнопка: адрес для плеера и подписи телефона и ТВ. */
data class PlayButton(
    /** Что играть; null — фильм целиком: плеер возьмёт первый трек. */
    val video: VideoRef?,
    /** «Продолжить» или «Смотреть» — телефон: что именно сыграет, говорит строка под ней. */
    val title: String,
    /** «Продолжить · S4E7» — ТВ: строки под кнопкой нет, серия названа в самой подписи. */
    val titleWithEpisode: String,
    /** У фильма «2 ч 11 мин · 4K», у сериала «S4E7 «Мост» · осталось 29 мин»; null — сказать нечего. */
    val subtitle: String?,
    /** Досмотренная доля серии, которую продолжают; null — полоски нет. */
    val progress: Float?,
)

/**
 * Кнопка «Смотреть» тайтла на экране: фильм — целиком; сериал — серия «продолжить», иначе первая
 * серия выбранного сезона, иначе первая вообще. null — тайтл не загружен или у сериала нет серий:
 * играть нечем.
 */
val DetailsState.play: PlayButton?
    get() {
        val item = item ?: return null
        val series = series ?: return moviePlay(item)
        val episode = series.resume ?: seasonEpisodes.firstOrNull() ?: item.tracklist.firstOrNull() ?: return null
        val resumed = episode == series.resume
        return PlayButton(
            video = episode.ref,
            title = if (resumed) "Продолжить" else "Смотреть",
            titleWithEpisode = if (resumed) "Продолжить · ${episodeCode(episode)}" else "Смотреть",
            subtitle = episodeSubtitle(episode, resumed),
            progress = episodeProgress(episode).takeIf { resumed && it > 0f },
        )
    }

private fun moviePlay(item: Item) = PlayButton(
    video = null,
    title = "Смотреть",
    titleWithEpisode = "Смотреть",
    subtitle = listOfNotNull(volumeLabel(item, null), "4K".takeIf { hasUhd(item) })
        .joinToString(" · ")
        .ifBlank { null },
    progress = null,
)

/** «S4E7»; серия вне сезона — «Серия 7». */
private fun episodeCode(episode: MediaTrack): String =
    if (episode.seasonNumber > 0) "S${episode.seasonNumber}E${episode.number}" else "Серия ${episode.number}"

/** «S4E7 «Мост» · осталось 29 мин»: остаток — только у серии, которую продолжают. */
private fun episodeSubtitle(episode: MediaTrack, resumed: Boolean): String {
    val title = episode.title.takeIf { it.isNotBlank() }?.let { "«$it»" }
    val left = remainingMinutes(episode)?.let { "осталось ${durationLabel(it)}" }.takeIf { resumed }
    return listOfNotNull(listOfNotNull(episodeCode(episode), title).joinToString(" "), left).joinToString(" · ")
}

private fun remainingMinutes(episode: MediaTrack): Int? {
    if (episode.watchedSeconds <= 0) return null
    return ((episode.durationSeconds - episode.watchedSeconds) / SECONDS_IN_MINUTE).takeIf { it > 0 }
}
