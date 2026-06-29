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
    }
}
