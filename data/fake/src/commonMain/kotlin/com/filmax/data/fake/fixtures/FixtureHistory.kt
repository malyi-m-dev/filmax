package com.filmax.data.fake.fixtures

import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.store.FakeCatalog
import com.filmax.data.fake.store.MOVIE_SEASON
import com.filmax.data.fake.store.TrackKey
import com.filmax.data.fake.store.TrackProgress
import com.filmax.data.fake.store.WATCH_STATUS_FINISHED
import com.filmax.data.fake.store.WATCH_STATUS_IN_PROGRESS
import com.filmax.data.fake.store.WatchSnapshot
import com.filmax.data.fake.store.keyIn

/** Начатый трек и позиция в нём. */
private data class StartedTrack(val key: TrackKey, val seconds: Int)

/**
 * Досмотрено всё до этой серии включительно, по порядку сезонов. У «Северного ветра» начатой
 * серии нет — «Продолжить» ведёт на следующую после досмотренной (S2E5), а не на начатую.
 */
private val WATCHED_THROUGH: List<TrackKey> = listOf(
    TrackKey(itemId = FakeIds.LONG_SERIES, season = 4, number = 6),
    TrackKey(itemId = FakeIds.MEDIUM_SERIES, season = 2, number = 4),
    TrackKey(itemId = FakeIds.SHORT_SERIES, season = 1, number = 2),
    TrackKey(itemId = FakeIds.ANIME_SERIES, season = 1, number = 5),
    TrackKey(itemId = FakeIds.DOCUMENTARY_SERIES, season = 1, number = 1),
    TrackKey(itemId = FakeIds.WATCHED_MOVIE, season = MOVIE_SEASON, number = 1),
)

/**
 * Начатые треки. Позиции — внутри первых десяти минут: тестовые потоки длятся 10–12 минут, и
 * позиция дальше конца перемотала бы плеер сразу на финал.
 */
private val STARTED: List<StartedTrack> = listOf(
    StartedTrack(TrackKey(itemId = FakeIds.LONG_SERIES, season = 4, number = 7), seconds = 480),
    StartedTrack(TrackKey(itemId = FakeIds.MOVIE_IN_PROGRESS, season = MOVIE_SEASON, number = 1), seconds = 420),
    StartedTrack(TrackKey(itemId = FakeIds.SHORT_SERIES, season = 1, number = 3), seconds = 210),
    StartedTrack(TrackKey(itemId = FakeIds.ANIME_SERIES, season = 1, number = 6), seconds = 300),
    StartedTrack(TrackKey(itemId = FakeIds.DOCUMENTARY_SERIES, season = 1, number = 2), seconds = 150),
)

/**
 * История — по треку на тайтл, свежие первыми. Первые пять уходят в «Продолжить» на главной:
 * сериал на середине серии, фильм, веб-сериал, аниме и сериал, где последняя серия досмотрена.
 */
private val RECENT: List<TrackKey> = listOf(
    TrackKey(itemId = FakeIds.LONG_SERIES, season = 4, number = 7),
    TrackKey(itemId = FakeIds.MOVIE_IN_PROGRESS, season = MOVIE_SEASON, number = 1),
    TrackKey(itemId = FakeIds.SHORT_SERIES, season = 1, number = 3),
    TrackKey(itemId = FakeIds.ANIME_SERIES, season = 1, number = 6),
    TrackKey(itemId = FakeIds.MEDIUM_SERIES, season = 2, number = 4),
    TrackKey(itemId = FakeIds.DOCUMENTARY_SERIES, season = 1, number = 2),
    TrackKey(itemId = FakeIds.WATCHED_MOVIE, season = MOVIE_SEASON, number = 1),
)

/** Прогресс и история на старт сессии; длительности досмотренных серий берутся из каталога. */
internal fun initialWatchSnapshot(catalog: FakeCatalog): WatchSnapshot {
    val watched = WATCHED_THROUGH.flatMap { last -> watchedUpTo(catalog, last) }
    val started = STARTED.map { it.key to TrackProgress(it.seconds, WATCH_STATUS_IN_PROGRESS) }
    return WatchSnapshot(progress = (watched + started).toMap(), recent = RECENT)
}

/** Все треки тайтла до [last] включительно — досмотрены до конца. */
private fun watchedUpTo(catalog: FakeCatalog, last: TrackKey): List<Pair<TrackKey, TrackProgress>> {
    val tracks = catalog.entry(last.itemId)?.item?.tracklist.orEmpty()
    val lastIndex = tracks.indexOfFirst { it.seasonNumber == last.season && it.number == last.number }
    return tracks.take(lastIndex + 1).map { track ->
        track.keyIn(last.itemId) to TrackProgress(track.durationSeconds, WATCH_STATUS_FINISHED)
    }
}
