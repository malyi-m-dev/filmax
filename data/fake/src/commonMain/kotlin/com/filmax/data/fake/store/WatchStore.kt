package com.filmax.data.fake.store

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Статусы просмотра kino.pub (`watching.status`); «не начат» (-1) — значение трека по умолчанию. */
internal const val WATCH_STATUS_IN_PROGRESS = 0
internal const val WATCH_STATUS_FINISHED = 1

/** Сезон фильма: его единственное видео приходит в `videos` без сезона. */
internal const val MOVIE_SEASON = 0

/**
 * С какой доли длительности трек считается досмотренным. Плеер не сообщает «конец», только
 * позиции раз в несколько секунд, и последняя отметка почти никогда не равна длительности.
 */
private const val WATCHED_SHARE = 0.9

/** Ключ прогресса: номер серии уникален только внутри сезона, у фильма сезон — [MOVIE_SEASON]. */
internal data class TrackKey(val itemId: Int, val season: Int, val number: Int)

/** Прогресс трека в терминах kino.pub: секунды и статус. */
internal data class TrackProgress(val seconds: Int, val status: Int)

/**
 * Состояние просмотра. [recent] — по одной записи на тайтл, свежие первыми: ровно так выглядит
 * `api/v1/history` после distinctBy в настоящем репозитории. [seenAt] — когда тайтл смотрели
 * последний раз (`last_seen`), мс от эпохи, по id тайтла.
 */
internal data class WatchSnapshot(
    val progress: Map<TrackKey, TrackProgress>,
    val recent: List<TrackKey>,
    val seenAt: Map<Int, Long> = emptyMap(),
)

internal fun MediaTrack.keyIn(itemId: Int): TrackKey = TrackKey(itemId, seasonNumber, number)

/**
 * Прогресс и история на сессию. Состояние неизменяемое и меняется атомарно через StateFlow:
 * репозитории зовут из разных корутин (FavoritesRepositoryImpl, например, живёт на Default).
 */
internal class WatchStore(initial: WatchSnapshot, private val clock: () -> Long) {

    private val state = MutableStateFlow(initial)

    fun recent(): List<TrackKey> = state.value.recent

    fun progressOf(key: TrackKey): TrackProgress? = state.value.progress[key]

    fun seenAt(itemId: Int): Long? = state.value.seenAt[itemId]

    /** Серии тайтла с прогрессом поверх — так их отдаёт `items/{id}`. */
    fun withProgress(item: Item): List<MediaTrack> {
        val progress = state.value.progress
        return item.tracklist.map { track ->
            val saved = progress[track.keyIn(item.id)] ?: return@map track
            track.copy(watchedSeconds = saved.seconds, watchStatus = saved.status)
        }
    }

    /** Отметка из плеера (`watching/marktime`): тайтл встаёт первым в «недавнем». */
    fun save(key: TrackKey, seconds: Int, durationSeconds: Int) {
        val position = seconds.coerceAtLeast(0)
        val finished = durationSeconds > 0 && position >= durationSeconds * WATCHED_SHARE
        val status = if (finished) WATCH_STATUS_FINISHED else WATCH_STATUS_IN_PROGRESS
        state.update { snapshot ->
            snapshot.copy(
                progress = snapshot.progress + (key to TrackProgress(position, status)),
                recent = listOf(key) + snapshot.recent.filterNot { it.itemId == key.itemId },
                seenAt = snapshot.seenAt + (key.itemId to clock()),
            )
        }
    }

    /** `watching/toggle`: всё досмотрено — сбрасываем тайтл, иначе отмечаем досмотренным целиком. */
    fun toggleWatched(item: Item) {
        state.update { snapshot ->
            val keys = item.tracklist.map { it.keyIn(item.id) }
            val allWatched = keys.all { snapshot.progress[it]?.status == WATCH_STATUS_FINISHED }
            if (allWatched) snapshot.without(item.id) else snapshot.withAllWatched(item, clock())
        }
    }

    /** `history/clear-for-item`: тайтл пропадает из истории вместе с прогрессом. */
    fun forget(itemId: Int) {
        state.update { it.without(itemId) }
    }
}

private fun WatchSnapshot.without(itemId: Int): WatchSnapshot = copy(
    progress = progress.filterKeys { it.itemId != itemId },
    recent = recent.filterNot { it.itemId == itemId },
    seenAt = seenAt - itemId,
)

private fun WatchSnapshot.withAllWatched(item: Item, now: Long): WatchSnapshot {
    val watched = item.tracklist.associate { track ->
        track.keyIn(item.id) to TrackProgress(track.durationSeconds, WATCH_STATUS_FINISHED)
    }
    val last = item.tracklist.lastOrNull()?.keyIn(item.id) ?: return this
    return copy(
        progress = progress + watched,
        recent = listOf(last) + recent.filterNot { it.itemId == item.id },
        seenAt = seenAt + (item.id to now),
    )
}
