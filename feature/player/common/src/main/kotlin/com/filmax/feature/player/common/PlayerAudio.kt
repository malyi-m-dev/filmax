// Аудиодорожки плеера: подписи и ключи озвучек из метаданных kino.pub и групп Media3, выбор трека
// по маршруту и коды языков. Вынесено из PlayerScreenModel — это чистые функции без состояния.
package com.filmax.feature.player.common

import androidx.media3.common.Tracks
import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.audioLanguageName
import com.filmax.feature.player.common.navigation.PlayerRoute

/** Трек маршрута: номер видео + сезон (у фильма сезона нет — совпадения по номеру достаточно). */
internal fun matchesRoute(track: MediaTrack, route: PlayerRoute): Boolean =
    track.number == route.videoId && (route.season <= 0 || track.seasonNumber == route.season)

/** Код языка для предпочтения Media3; «Оригинал» и неизвестное — null: пусть плеер выбирает сам. */
internal fun languageCodeOf(display: String): String? = when (display.lowercase()) {
    "русский" -> "rus"
    "english" -> "eng"
    else -> null
}

/**
 * Подпись дорожки: «2. Русский · Многоголосый · BaibaKo» — как в оригинальном клиенте
 * kino.pub. Метаданные берём из `audios[]` ответа API, сопоставляя с группой Media3 по
 * порядку (`audios[].index` 1-based = порядок дорожек в HLS-манифесте): сам манифест
 * kino.pub кладёт в NAME только код языка, и по нему озвучки неотличимы. Номер в начале
 * гарантирует уникальность подписи, даже если у двух озвучек совпали студия и тип.
 */
internal fun audioLabel(groupIndex: Int, group: Tracks.Group, apiAudios: List<AudioTrack>): String {
    val meta = apiAudios.firstOrNull { it.index == groupIndex + 1 }
    val language = meta?.lang ?: group.getTrackFormat(0).language
    val parts = buildList {
        add(audioLanguageName(language))
        meta?.voiceType?.let { add(it) }
        meta?.voiceAuthor?.let { add(it) }
    }.distinct()
    return "${groupIndex + 1}. ${parts.joinToString(" · ")}"
}

/**
 * Вариант дорожки для меню: уникальная подпись ([audioLabel]) плюс те же метаданные по частям —
 * ТВ раскладывает их по строке варианта (язык и тип, студия, каналы).
 */
internal fun audioOption(groupIndex: Int, group: Tracks.Group, apiAudios: List<AudioTrack>): AudioOption {
    val meta = apiAudios.firstOrNull { it.index == groupIndex + 1 }
    return AudioOption(
        label = audioLabel(groupIndex, group, apiAudios),
        groupIndex = groupIndex,
        languageCode = meta?.lang ?: group.getTrackFormat(0).language,
        voiceType = meta?.voiceType,
        voiceAuthor = meta?.voiceAuthor,
        channels = meta?.channels ?: group.getTrackFormat(0).channelCount.coerceAtLeast(0),
    )
}

/**
 * Ключ озвучки для памяти на тайтл: `язык|тип|студия` из метаданных API. Позиционный индекс не
 * годится — у разных серий порядок дорожек может отличаться, а связка язык+тип+студия
 * идентифицирует именно озвучку.
 */
internal fun voiceKey(groupIndex: Int, group: Tracks.Group, apiAudios: List<AudioTrack>): String {
    val meta = apiAudios.firstOrNull { it.index == groupIndex + 1 }
    val language = meta?.lang ?: group.getTrackFormat(0).language
    return listOf(language.orEmpty(), meta?.voiceType.orEmpty(), meta?.voiceAuthor.orEmpty()).joinToString("|")
}

/** Хост из URL — для хлебных крошек телеметрии (сам URL с подписью в логи не пишем). */
internal fun urlHost(url: String): String = url.substringAfter("://").substringBefore("/")
