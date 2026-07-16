package com.filmax.data.watching

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.safeRequest
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watching.model.Notification
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.data.watching.remote.WatchingApi
import com.filmax.data.watching.remote.dto.HistoryEntryDto

/** `watching/{type}`: единственный трек «досмотрено» — статус 1, как и в `items/{id}`. */
private const val WATCH_STATUS_IN_PROGRESS = 0

private fun HistoryEntryDto.toDomain(): WatchHistory {
    // Длительность берём у самой серии; у фильма media.duration тоже заполнен, а item.duration —
    // это средняя длительность по тайтлу, годная только как запасной вариант.
    val duration = media?.duration?.takeIf { it > 0 }
        ?: item.duration?.average?.takeIf { it > 0 }?.toInt()
    return WatchHistory(
        itemId = item.id,
        title = item.title,
        posterSmall = item.posters?.small,
        posterWide = item.posters?.wide,
        episodeThumbnail = media?.thumbnail,
        progress = WatchProgress(
            status = WATCH_STATUS_IN_PROGRESS,
            timeSeconds = time,
            durationSeconds = duration,
            // `number`, а не id: тем же числом kino.pub принимает прогресс в marktime и
            // им же плеер выбирает дорожку.
            videoId = media?.number,
            season = media?.snumber?.takeIf { it > 0 },
        ),
    )
}

internal class WatchingRepositoryImpl(
    private val api: WatchingApi,
) : WatchingRepository {

    /**
     * История с прогрессом — из `api/v1/history`, а не из `watching/{type}`.
     *
     * `watching/{type}` возвращает только id/title/posters, без объекта `watching`: доля просмотра
     * там всегда выходила нулевой, и «Продолжить» физически не мог наполниться. `api/v1/history`
     * отдаёт `time` по каждому видео, саму серию (`media`) с её кадром и длительностью — и уже
     * отсортирован сервером по свежести.
     *
     * Одна страница: на экран «Продолжить» и так попадает верхушка списка, а `perpage` = 20.
     *
     * `distinctBy(itemId)` обязателен. История ведётся ПО СЕРИЯМ: один сериал приходит несколькими
     * записями (s1e1, s1e2, …), а зрителю нужен один тайтл — тот, на котором он остановился.
     * Сервер отдаёт список от свежего к старому, поэтому первая запись тайтла и есть последняя
     * серия. Без дедупликации ряд получал дублирующиеся ключи и Compose падал с
     * «Key … was already used».
     */
    override suspend fun getHistory(type: String): RequestResult<List<WatchHistory>> = safeRequest {
        api.getHistoryList().history
            .map { entry -> entry.toDomain() }
            .distinctBy { it.itemId }
    }

    override suspend fun saveProgress(itemId: Int, videoId: Int, timeSeconds: Int): RequestResult<Unit> =
        safeRequest { api.saveProgress(itemId, videoId, timeSeconds) }

    override suspend fun saveProgressSerial(
        itemId: Int,
        season: Int,
        videoId: Int,
        timeSeconds: Int,
    ): RequestResult<Unit> =
        safeRequest { api.saveProgressSerial(itemId, season, videoId, timeSeconds) }

    override suspend fun toggleWatched(itemId: Int): RequestResult<Unit> =
        safeRequest { api.toggleWatched(itemId) }

    override suspend fun toggleWatchlist(itemId: Int): RequestResult<Boolean> =
        safeRequest { api.toggleWatchlist(itemId)["watching"] == 1 }

    override suspend fun clearHistory(itemId: Int): RequestResult<Unit> =
        safeRequest { api.clearItemHistory(itemId) }

    override suspend fun getNotifications(): RequestResult<List<Notification>> = safeRequest {
        api.getNotifications().notifications?.map { dto ->
            Notification(
                id = dto.id,
                title = dto.title,
                text = dto.text,
                createdAt = dto.createdAt?.toLong()?.times(MILLIS_IN_SECOND),
                read = dto.read,
                itemId = dto.itemId,
            )
        } ?: emptyList()
    }

    override suspend fun markNotificationRead(id: Int): RequestResult<Unit> =
        safeRequest { api.markNotificationRead(id) }

    override suspend fun markAllNotificationsRead(): RequestResult<Unit> =
        safeRequest { api.markAllNotificationsRead() }

    private companion object {
        // kino.pub отдаёт временные метки в секундах — переводим в миллисекунды.
        const val MILLIS_IN_SECOND = 1000
    }
}
