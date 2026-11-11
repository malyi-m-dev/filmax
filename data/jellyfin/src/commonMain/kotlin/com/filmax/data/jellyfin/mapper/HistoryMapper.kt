package com.filmax.data.jellyfin.mapper

import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.data.jellyfin.JellyfinIds
import com.filmax.data.jellyfin.remote.dto.ItemDto

/**
 * Запись истории из просмотренного видео. История у сервера — по видео, а «Моё» и «Продолжить
 * просмотр» показывают тайтлы: серия превращается в свой сериал с прогрессом этой серии.
 */
internal fun JellyfinMapper.history(dto: ItemDto, ids: JellyfinIds): WatchHistory? {
    val episode = dto.type == JellyfinMapper.TYPE_EPISODE
    val titleGuid = (if (episode) dto.seriesId else dto.id) ?: return null
    val primaryTag = dto.imageTags?.get(JellyfinMapper.PRIMARY)
    return WatchHistory(
        itemId = ids.intFor(titleGuid),
        title = (if (episode) dto.seriesName else dto.name).orEmpty(),
        posterSmall = if (episode) {
            images.primary(dto.seriesId, dto.seriesPrimaryImageTag, JellyfinImages.POSTER_SMALL)
        } else {
            images.primary(dto.id, primaryTag, JellyfinImages.POSTER_SMALL)
        },
        progress = WatchProgress(
            status = JellyfinMapper.watchStatus(dto.userData),
            timeSeconds = JellyfinMapper.watchedSeconds(dto.userData),
            durationSeconds = dto.runTimeTicks?.let(JellyfinMapper::ticksToSeconds)?.toInt(),
            videoId = if (episode) dto.indexNumber else 1,
            season = if (episode) dto.parentIndexNumber else null,
        ),
        posterWide = backdrop(dto),
        episodeThumbnail = if (episode) images.primary(dto.id, primaryTag, JellyfinImages.EPISODE_STILL) else null,
        lastSeenAt = IsoDates.toEpochMillis(dto.userData?.lastPlayedDate),
    )
}
