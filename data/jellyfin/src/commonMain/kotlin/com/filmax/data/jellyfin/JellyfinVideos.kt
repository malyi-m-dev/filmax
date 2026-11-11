package com.filmax.data.jellyfin

import com.filmax.data.jellyfin.mapper.JellyfinMapper
import com.filmax.data.jellyfin.remote.JellyfinApi
import com.filmax.data.jellyfin.remote.dto.ItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Видео, которое играет плеер: GUID фильма или серии на сервере. */
internal data class VideoRef(val guid: String, val durationSeconds: Int)

/** Тайтл с сериями — как его отдают `Items/{id}` и `Shows/{id}/Episodes`. */
internal data class TitleVideos(val item: ItemDto, val episodes: List<ItemDto>)

/**
 * Где на сервере лежит видео, которое плеер называет парой «сезон + номер». Маршрут плеера и
 * прогресс адресуют серию так же, как у kino.pub (`PlayerRoute(itemId, videoId, season)`), а
 * серверу нужен GUID серии. Пары запоминаются, когда грузятся детали тайтла; не нашлось (плеер
 * открыли с телефона, минуя детали) — тайтл грузится заново.
 */
internal class JellyfinVideos(private val api: JellyfinApi, private val ids: JellyfinIds) {

    private val known = MutableStateFlow<Map<VideoKey, VideoRef>>(emptyMap())

    suspend fun title(guid: String): TitleVideos {
        val item = api.item(guid)
        val episodic = JellyfinMapper.itemType(item.type).episodic
        val episodes = if (episodic) api.episodes(guid).items.orEmpty() else emptyList()
        remember(TitleVideos(item, episodes))
        return TitleVideos(item, episodes)
    }

    suspend fun resolve(itemId: Int, season: Int, number: Int): VideoRef {
        val key = VideoKey(itemId, season.coerceAtLeast(0), number)
        known.value[key]?.let { return it }
        title(ids.requireGuid(itemId))
        return known.value[key] ?: throw NoSuchElementException("Видео $season×$number не найдено (404)")
    }

    private fun remember(title: TitleVideos) {
        val itemId = ids.intFor(title.item.id.orEmpty())
        val refs = if (title.episodes.isEmpty()) {
            mapOf(VideoKey(itemId, 0, 1) to ref(title.item))
        } else {
            title.episodes.mapNotNull { episode ->
                val season = episode.parentIndexNumber ?: return@mapNotNull null
                val number = episode.indexNumber ?: return@mapNotNull null
                VideoKey(itemId, season, number) to ref(episode)
            }.toMap()
        }
        known.update { it + refs }
    }

    private fun ref(dto: ItemDto) = VideoRef(
        guid = dto.id.orEmpty(),
        durationSeconds = dto.runTimeTicks?.let(JellyfinMapper::ticksToSeconds)?.toInt() ?: 0,
    )

    private data class VideoKey(val itemId: Int, val season: Int, val number: Int)
}
