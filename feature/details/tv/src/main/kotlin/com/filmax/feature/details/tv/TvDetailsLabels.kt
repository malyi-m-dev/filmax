// Строка серии на ТВ — модель компонента дизайн-системы (TvEpisodeRowModel), поэтому живёт здесь, а не
// в модели экрана. Остальные подписи деталей (надзаголовок, оценки, мета, кнопка «Смотреть», сводка
// сезона) — готовые поля состояния в details.common.
package com.filmax.feature.details.tv

import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.tv.designsystem.TvEpisodeRowModel
import com.filmax.feature.details.common.WATCH_STATUS_FINISHED
import com.filmax.feature.details.common.WATCH_STATUS_IN_PROGRESS
import com.filmax.feature.details.common.episodeProgress
import com.filmax.feature.details.common.episodeRowMeta
import com.filmax.feature.details.common.numberedEpisodeTitle

/** Модель строки серии: «7. Тайный совет», мета о том, где в серии зритель, прогресс. */
internal fun episodeRow(episode: MediaTrack, resumeId: Int?): TvEpisodeRowModel {
    val watched = episode.watchStatus == WATCH_STATUS_FINISHED
    val resume = episode.id == resumeId && !watched
    return TvEpisodeRowModel(
        title = numberedEpisodeTitle(episode),
        meta = episodeRowMeta(episode, resume),
        imageUrl = episode.thumbnail,
        progress = episodeProgress(episode).takeIf { episode.watchStatus == WATCH_STATUS_IN_PROGRESS || watched },
        watched = watched,
        resume = resume,
    )
}
