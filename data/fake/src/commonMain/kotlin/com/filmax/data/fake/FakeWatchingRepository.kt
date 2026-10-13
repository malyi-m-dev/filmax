package com.filmax.data.fake

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watching.model.Notification
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.data.fake.store.MOVIE_SEASON
import com.filmax.data.fake.store.TrackKey
import com.filmax.data.fake.store.WATCH_STATUS_IN_PROGRESS

/**
 * Просмотр на сессию: отметки плеера, история, «Буду смотреть» как флаг и уведомления. Отметка
 * ищет трек по паре сезон+номер (номер серии уникален только внутри сезона), а досмотренным трек
 * становится с 90% длительности — так «Продолжить» в деталях сдвигается на следующую серию.
 */
class FakeWatchingRepository(private val backend: FakeBackend) : WatchingRepository {

    // Тип не фильтрует, как и у настоящего репозитория: `api/v1/history` общий для всего.
    override suspend fun getHistory(type: String): RequestResult<List<WatchHistory>> = backend.respond {
        success(watch.recent().mapNotNull { key -> historyEntry(key) })
    }

    override suspend fun saveProgress(itemId: Int, videoId: Int, timeSeconds: Int): RequestResult<Unit> =
        mark(TrackKey(itemId = itemId, season = MOVIE_SEASON, number = videoId), timeSeconds)

    override suspend fun saveProgressSerial(
        itemId: Int,
        season: Int,
        videoId: Int,
        timeSeconds: Int,
    ): RequestResult<Unit> =
        mark(TrackKey(itemId = itemId, season = season, number = videoId), timeSeconds)

    override suspend fun toggleWatched(itemId: Int): RequestResult<Unit> = backend.respond {
        val entry = catalog.entry(itemId) ?: return@respond notFound("item $itemId")
        watch.toggleWatched(entry.item)
        success(Unit)
    }

    override suspend fun toggleWatchlist(itemId: Int): RequestResult<Boolean> = backend.respond {
        if (catalog.entry(itemId) == null) notFound("item $itemId") else success(library.toggleWatchlist(itemId))
    }

    override suspend fun clearHistory(itemId: Int): RequestResult<Unit> = backend.respond {
        watch.forget(itemId)
        success(Unit)
    }

    override suspend fun getNotifications(): RequestResult<List<Notification>> =
        backend.respond { success(account.notifications()) }

    override suspend fun markNotificationRead(id: Int): RequestResult<Unit> = backend.respond {
        if (account.markRead(id)) success(Unit) else notFound("notification $id")
    }

    override suspend fun markAllNotificationsRead(): RequestResult<Unit> = backend.respond {
        account.markAllRead()
        success(Unit)
    }

    /** `watching/marktime`: позиция пишется только для трека, который реально есть у тайтла. */
    private suspend fun mark(key: TrackKey, seconds: Int): RequestResult<Unit> = backend.respond {
        val track = catalog.track(key) ?: return@respond notFound("video ${key.number} of item ${key.itemId}")
        watch.save(key, seconds, track.durationSeconds)
        success(Unit)
    }
}

/**
 * Запись истории так, как её собирает настоящий репозиторий из `api/v1/history`: статус всегда
 * «в процессе» (сервер его там не отдаёт, досмотренность видна по доле time/duration), номер
 * серии — `number`, сезон фильма — null.
 */
private fun FakeBackend.historyEntry(key: TrackKey): WatchHistory? {
    val item = catalog.entry(key.itemId)?.item
    val track = catalog.track(key)
    val progress = watch.progressOf(key)
    if (item == null || track == null || progress == null) return null
    return WatchHistory(
        itemId = item.id,
        title = item.title,
        posterSmall = item.posters.small,
        progress = WatchProgress(
            status = WATCH_STATUS_IN_PROGRESS,
            timeSeconds = progress.seconds,
            durationSeconds = track.durationSeconds,
            videoId = track.number,
            season = track.seasonNumber.takeIf { it > MOVIE_SEASON },
        ),
        posterWide = item.posters.wide,
        episodeThumbnail = track.thumbnail,
        lastSeenAt = watch.seenAt(item.id),
    )
}
