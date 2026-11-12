// Чистые правила трека плеера: какой трек играет маршрут, с какой секунды, какими потоками и на
// каком языке аудио. Вынесены из PlayerScreenModel — им не нужны ни плеер, ни состояние экрана.
package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.feature.player.common.navigation.PlayerRoute

/** Трек маршрута: серия — по сезону и номеру, видео фильма — по номеру. */
internal fun MediaTrack.matchesRoute(route: PlayerRoute): Boolean = when (val video = route.video) {
    is VideoRef.Episode -> seasonNumber == video.season && number == video.number
    is VideoRef.Movie -> number == video.number
    null -> false
}

/**
 * С какой секунды начать. Позицию, переданную с телефона, берём как есть: он только что
 * показывал этот кадр. Иначе — сохранённый прогресс, но досмотренный до конца трек начинаем
 * сначала, иначе пересмотр стартовал бы с титров. null — с начала.
 */
internal fun startSeconds(route: PlayerRoute, track: MediaTrack?): Int? =
    route.startSeconds.takeIf { it >= 0 }
        ?: track?.watchedSeconds?.takeIf { it > 0 && track.watchStatus != WATCH_STATUS_FINISHED }

/**
 * Доступные качества — из файлов трека; все варианты доставки в порядке предпочтения,
 * чтобы плееру было куда фолбэчить при недоступном CDN. После hls4 — mp4, а не hls: оба на
 * cdntogo.net, но в hls kino.pub вшивает одну озвучку, а в mp4 лежат все, и при заблокированном
 * api.srvkp.com зритель сохраняет выбор озвучки.
 */
internal fun streamQualities(track: MediaTrack?): List<StreamQuality> =
    track?.files.orEmpty().mapNotNull { file ->
        listOfNotNull(file.hls4, file.http, file.hls)
            .takeIf { it.isNotEmpty() }
            ?.let { StreamQuality(file.quality, it) }
    }

/** Код языка для выбора дорожки плеером; «Оригинал» и неизвестный — null, плеер выберет сам. */
internal fun langCode(display: String): String? = when (display.lowercase()) {
    "русский" -> "rus"
    "english" -> "eng"
    else -> null
}
