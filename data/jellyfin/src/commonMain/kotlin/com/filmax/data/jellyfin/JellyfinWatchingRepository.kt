package com.filmax.data.jellyfin

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.data.jellyfin.mapper.JellyfinMapper
import com.filmax.data.jellyfin.mapper.history
import com.filmax.data.jellyfin.remote.ItemsQuery
import com.filmax.data.jellyfin.remote.dto.PlaybackProgressBody
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Просмотр на домашнем сервере. Прогресс уходит отчётами о воспроизведении — так же, как от
 * официальных клиентов: сервер сам решает, где точка «продолжить», когда серия досмотрена и что
 * попадёт в «Продолжить просмотр» на других устройствах. Писать позицию напрямую в данные
 * пользователя значило бы повторять эти правила здесь и расходиться с ними.
 */
// Реализация всего контракта WatchingRepository — столько же методов.
@Suppress("TooManyFunctions")
internal class JellyfinWatchingRepository(
    private val context: JellyfinContext,
    private val videos: JellyfinVideos,
) : WatchingRepository {

    private val api = context.api

    /** Видео, о старте которого сервер уже знает: старт ставит «смотрели только что» и счётчик. */
    private val started = MutableStateFlow<String?>(null)

    override suspend fun getHistory(type: String): RequestResult<List<WatchHistory>> = context.request { mapper ->
        val query = ItemsQuery(
            includeItemTypes = historyTypes(type),
            sortBy = listOf(DATE_PLAYED),
            limit = HISTORY_LIMIT,
        )
        api.items(query).items.orEmpty()
            // Сортировка по дате просмотра ставит несмотренное в хвост — в историю оно не входит.
            .filter { it.userData?.lastPlayedDate != null || JellyfinMapper.watchedSeconds(it.userData) > 0 }
            .mapNotNull { mapper.history(it, context.ids) }
            .distinctBy { it.itemId }
    }

    override suspend fun saveProgress(itemId: Int, videoId: Int, timeSeconds: Int): RequestResult<Unit> =
        report(itemId, season = 0, number = videoId, timeSeconds = timeSeconds)

    override suspend fun saveProgressSerial(
        itemId: Int,
        season: Int,
        videoId: Int,
        timeSeconds: Int,
    ): RequestResult<Unit> = report(itemId, season, videoId, timeSeconds)

    override suspend fun toggleWatched(itemId: Int): RequestResult<Unit> = context.request { _ ->
        val guid = context.ids.requireGuid(itemId)
        api.setPlayed(guid, played = api.userData(guid).played != true)
    }

    /**
     * «Убрать из истории» — снять отметку просмотра: сервер стирает с ней и дату просмотра, и
     * точку «продолжить». У сериала снимается со всех серий.
     */
    override suspend fun clearHistory(itemId: Int): RequestResult<Unit> = context.request { _ ->
        api.setPlayed(context.ids.requireGuid(itemId), played = false)
    }

    private suspend fun report(itemId: Int, season: Int, number: Int, timeSeconds: Int): RequestResult<Unit> =
        context.request { _ ->
            val video = videos.resolve(itemId, season, number)
            val body = PlaybackProgressBody(
                itemId = video.guid,
                positionTicks = JellyfinMapper.secondsToTicks(timeSeconds),
            )
            if (started.value == video.guid) {
                api.reportPlaybackProgress(body)
            } else {
                api.reportPlaybackStart(body)
                started.value = video.guid
            }
        }

    private fun historyTypes(type: String): List<String> = when (type) {
        "movie" -> listOf(TYPE_MOVIE)
        "serial" -> listOf(JellyfinMapper.TYPE_EPISODE)
        else -> listOf(TYPE_MOVIE, JellyfinMapper.TYPE_EPISODE)
    }

    private companion object {
        const val DATE_PLAYED = "DatePlayed"

        /** Видео, из которых собирается история: серии одного сериала схлопываются в одну запись. */
        const val HISTORY_LIMIT = 100
    }
}
