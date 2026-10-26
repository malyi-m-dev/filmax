package com.filmax.feature.player.common

import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlaybackException

/**
 * Лестница качеств: что включить на старте и куда спуститься, когда декодер устройства не
 * справился с кадром. Разрешения и кодека kino.pub не отдаёт — только подпись «2160p», «1080p»…,
 * поэтому высота кадра берётся из неё.
 *
 * Потолок ([limit]) — высота, на которой декодер ЭТОГО устройства уже падал: слабые ТВ-приставки
 * (MIBOX4, BRAVIA BF1) не тянут 4K H.264, и выше потолка плеер сам больше не стартует.
 */
internal object QualityLadder {

    /** Высота кадра по подписи: «2160p» → 2160, «4K» → 2160; null — подпись не разобрать. */
    fun heightOf(label: String): Int? {
        if (label.contains("4K", ignoreCase = true)) return UHD_HEIGHT
        return HEIGHT.find(label)?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Стартовое качество: предпочтение из настроек, если оно ниже потолка; иначе лучшее ниже
     * потолка. Ниже потолка ничего нет — лучшее вообще: попытка лучше пустого экрана.
     */
    fun start(qualities: List<StreamQuality>, preferred: String, limit: Int?): StreamQuality? {
        val playable = bestFirst(qualities).filter { fits(it, limit) }.ifEmpty { bestFirst(qualities) }
        return playable.firstOrNull { it.label == preferred } ?: playable.firstOrNull()
    }

    /**
     * Потолок по высоте, на которой упал декодер; null — это не предел железа. Потолком считается
     * только то, что выше Full HD: 1080p тянет любой ТВ на Android, и сбой на нём значит, что
     * декодер занят (его ещё держал другой плеер), а не слаб. Запомненный, такой сбой навсегда
     * оставил бы устройство на 720p — так и вышло на MiTV, где второй плеер открывался за кадром.
     */
    fun limitOf(failedHeight: Int): Int? = failedHeight.takeIf { it > FULL_HD_HEIGHT }

    /** Лучшее качество ниже [height], на которой декодер упал; null — спускаться некуда. */
    fun below(qualities: List<StreamQuality>, height: Int): StreamQuality? =
        bestFirst(qualities).firstOrNull { quality -> heightOf(quality.label)?.let { it < height } == true }

    /** Качество не выше потолка; без потолка или с неизвестной высотой — подходит. */
    private fun fits(quality: StreamQuality, limit: Int?): Boolean {
        val height = heightOf(quality.label) ?: return true
        return limit == null || height < limit
    }

    /** От лучшего к худшему; подписи без высоты — в конце, в исходном порядке. */
    private fun bestFirst(qualities: List<StreamQuality>): List<StreamQuality> =
        qualities.sortedByDescending { heightOf(it.label) ?: 0 }

    private val HEIGHT = Regex("""(\d{3,4})\s*p""", RegexOption.IGNORE_CASE)
    private const val UHD_HEIGHT = 2160
    private const val FULL_HD_HEIGHT = 1080
}

/**
 * Высота кадра, с которой не справился видеодекодер; null — ошибка не декодера видео (сеть, CDN,
 * аудио), её разбирает обычный фолбэк вариантов доставки.
 *
 * Ошибки декодера — коды 4001–4005: не инициализировался, не нашёлся, упал на кадре, формат выше
 * его возможностей или не поддержан. Их лечит только качество ниже. Так падали MIBOX4, BRAVIA BF1
 * и TCL на 4K H.264 (avc1.640033, 3840×2160): «MediaCodecVideoRenderer error».
 */
internal fun videoDecoderFailureHeight(error: PlaybackException): Int? =
    (error as? ExoPlaybackException)
        ?.takeIf { it.errorCode in DECODER_ERRORS }
        ?.rendererFormat
        ?.takeIf { MimeTypes.isVideo(it.sampleMimeType) }
        ?.height
        ?.takeIf { it > 0 }

private val DECODER_ERRORS =
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED..PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
