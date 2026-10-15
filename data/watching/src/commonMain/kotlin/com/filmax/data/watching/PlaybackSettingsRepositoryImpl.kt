package com.filmax.data.watching

import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Настройки воспроизведения на multiplatform-settings; реактивность — через [MutableStateFlow]. */
internal class PlaybackSettingsRepositoryImpl(
    private val storage: Settings,
) : PlaybackSettingsRepository {

    private val state = MutableStateFlow(load())

    override val settings: Flow<PlaybackSettings> = state.asStateFlow()

    override suspend fun setQuality(quality: String) = update { it.copy(quality = quality) }

    override suspend fun setAudioLanguage(language: String) = update { it.copy(audioLanguage = language) }

    override suspend fun setSubtitleLanguage(language: String) = update { it.copy(subtitleLanguage = language) }

    // Озвучка на тайтл — точечные ключи мимо state: это не глобальная настройка, а память
    // «какую дорожку слушали в этом сериале», и подписки на неё не нужны.
    override suspend fun voiceKeyFor(itemId: Int): String? =
        storage.getStringOrNull(KEY_VOICE_PREFIX + itemId)

    override suspend fun setVoiceKey(itemId: Int, key: String) {
        storage.putString(KEY_VOICE_PREFIX + itemId, key)
    }

    override suspend fun decoderLimit(): Int? = storage.getIntOrNull(KEY_DECODER_LIMIT)

    override suspend fun setDecoderLimit(height: Int?) {
        if (height == null) storage.remove(KEY_DECODER_LIMIT) else storage.putInt(KEY_DECODER_LIMIT, height)
    }

    private fun update(transform: (PlaybackSettings) -> PlaybackSettings) {
        val updated = transform(state.value)
        storage.putString(KEY_QUALITY, updated.quality)
        storage.putString(KEY_AUDIO, updated.audioLanguage)
        storage.putString(KEY_SUBTITLES, updated.subtitleLanguage)
        state.value = updated
    }

    private fun load() = PlaybackSettings(
        quality = storage.getStringOrNull(KEY_QUALITY) ?: PlaybackSettings.QualityAuto,
        audioLanguage = storage.getStringOrNull(KEY_AUDIO) ?: PlaybackSettings.AudioOriginal,
        subtitleLanguage = storage.getStringOrNull(KEY_SUBTITLES) ?: PlaybackSettings.SubtitleOff,
    )

    private companion object {
        const val KEY_QUALITY = "playback_quality"
        const val KEY_AUDIO = "playback_audio"
        const val KEY_SUBTITLES = "playback_subtitles"
        const val KEY_VOICE_PREFIX = "playback_voice_"
        const val KEY_DECODER_LIMIT = "playback_decoder_limit"
    }
}
