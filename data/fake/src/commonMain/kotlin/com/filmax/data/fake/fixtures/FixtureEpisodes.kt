package com.filmax.data.fake.fixtures

private const val SECONDS_IN_MINUTE = 60

// Шаги «разброса» длительностей и названий: взаимно простые с размерами пулов и диапазонов, чтобы
// соседние серии и сезоны не выглядели копиями друг друга.
private const val SEASON_DURATION_STEP = 3
private const val EPISODE_DURATION_STEP = 5
private const val SECONDS_JITTER_STEP = 13
private const val SEASON_TITLE_SHIFT = 7

/** Сезон с названиями серий по порядку; длительность каждой серии — своя, в пределах [minutes]. */
internal fun season(number: Int, titles: List<String>, minutes: IntRange): SeasonSpec = SeasonSpec(
    number = number,
    episodes = titles.mapIndexed { index, title ->
        val episode = index + 1
        EpisodeSpec(number = episode, title = title, durationSeconds = episodeSeconds(number, episode, minutes))
    },
)

/**
 * Сезоны сериала из пула названий: [episodeCounts] — серий в каждом сезоне по порядку. Каждый
 * сезон берёт названия со своим сдвигом (и [seed] сериала), поэтому сериалы с общим пулом не
 * повторяют друг друга, а внутри сезона названия уникальны, пока он не длиннее пула.
 */
internal fun pooledSeasons(
    episodeCounts: List<Int>,
    pool: List<String>,
    minutes: IntRange,
    seed: Int,
): List<SeasonSpec> = episodeCounts.mapIndexed { index, count ->
    val number = index + 1
    val offset = seed + number * SEASON_TITLE_SHIFT
    season(
        number = number,
        titles = List(count) { position -> pool[(offset + position) % pool.size] },
        minutes = minutes,
    )
}

/** Сезон без разметки: ни названий, ни кадров — так у kino.pub выглядит добрая половина сериалов. */
internal fun untitledSeason(number: Int, count: Int, minutes: IntRange): SeasonSpec = SeasonSpec(
    number = number,
    episodes = (1..count).map { episode -> untitledEpisode(number, episode, minutes) },
)

/** Дописывает в конец сезона [count] свежих серий, которые ещё не успели разметить. */
internal fun SeasonSpec.withUntitledTail(count: Int, minutes: IntRange): SeasonSpec {
    val first = episodes.size + 1
    return copy(episodes = episodes + (first until first + count).map { untitledEpisode(number, it, minutes) })
}

/**
 * Сезон, где каждая серия ровно по [seconds] — длине тестового потока. Только у таких серий
 * досмотр в плеере доходит до 90% длительности и честно ставит «просмотрено».
 */
internal fun streamLengthSeason(number: Int, titles: List<String>, seconds: Int): SeasonSpec = SeasonSpec(
    number = number,
    episodes = titles.mapIndexed { index, title ->
        EpisodeSpec(number = index + 1, title = title, durationSeconds = seconds)
    },
)

private fun untitledEpisode(season: Int, episode: Int, minutes: IntRange): EpisodeSpec = EpisodeSpec(
    number = episode,
    title = "",
    durationSeconds = episodeSeconds(season, episode, minutes),
    hasStill = false,
)

/** Длительность серии: у каждой своя, но одна и та же от запуска к запуску. */
private fun episodeSeconds(season: Int, episode: Int, minutes: IntRange): Int {
    val spread = minutes.last - minutes.first + 1
    val minute = minutes.first + (season * SEASON_DURATION_STEP + episode * EPISODE_DURATION_STEP) % spread
    return minute * SECONDS_IN_MINUTE + episode * SECONDS_JITTER_STEP % SECONDS_IN_MINUTE
}
