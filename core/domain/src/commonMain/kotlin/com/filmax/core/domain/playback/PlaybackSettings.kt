package com.filmax.core.domain.playback

import kotlinx.coroutines.flow.Flow

/**
 * Пользовательские предпочтения воспроизведения — выбираются в Профиле и
 * применяются на экране плеера (качество по умолчанию, язык аудио и субтитров).
 */
data class PlaybackSettings(
    val quality: String = QualityAuto,
    val audioLanguage: String = AudioOriginal,
    val subtitleLanguage: String = SubtitleOff,
) {
    companion object {
        const val QualityAuto = "Авто"
        const val AudioOriginal = "Оригинал"
        const val SubtitleOff = "Выкл"

        /** Предпочитаемое качество; «Авто» — лучшее из доступных у конкретного фильма. */
        val qualityOptions = listOf(QualityAuto, "2160p", "1080p", "720p", "480p", "360p")
        val audioOptions = listOf(AudioOriginal, "Русский", "English")
        val subtitleOptions = listOf(SubtitleOff, "Русский", "English")
    }
}

interface PlaybackSettingsRepository {
    val settings: Flow<PlaybackSettings>

    suspend fun setQuality(quality: String)

    suspend fun setAudioLanguage(language: String)

    suspend fun setSubtitleLanguage(language: String)

    /**
     * Озвучка, выбранная для конкретного тайтла: следующие серии сериала стартуют с неё же.
     * [key] — непрозрачный идентификатор дорожки (язык|тип|студия), собирает и разбирает его
     * плеер. null — для тайтла озвучку ещё не выбирали.
     */
    suspend fun voiceKeyFor(itemId: Int): String?

    suspend fun setVoiceKey(itemId: Int, key: String)
}
