package com.filmax.feature.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.player.navigation.PlayerRoute
import kotlinx.coroutines.flow.first

class PlayerScreenModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
    private val playbackSettings: PlaybackSettingsRepository,
    context: Context,
) : BaseScreenModel<PlayerState, PlayerSideEffect, PlayerEvent>(PlayerState()) {

    private val route = savedStateHandle.toRoute<PlayerRoute>()

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    /** Субтитры текущего трека — нужны, чтобы пересобрать MediaItem при смене качества. */
    private var trackSubtitles: List<SubtitleTrack> = emptyList()
    private var audioPreference: String = PlaybackSettings.AudioOriginal

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                screenModelScope { showError(AppError.Playback) }
            }
        })
        onFetchData()
    }

    override fun dispatch(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.SaveProgress -> saveProgress(event.positionMs)
            is PlayerEvent.SelectQuality -> selectQuality(event.label)
            is PlayerEvent.SelectSubtitle -> selectSubtitle(event.label)
        }
    }

    override fun onFetchData() {
        screenModelScope {
            val settings = playbackSettings.settings.first()
            audioPreference = settings.audioLanguage
            when (val result = catalog.getItemDetails(route.itemId)) {
                is RequestResult.Success -> {
                    val item = result.data
                    val track = item.tracklist.firstOrNull()
                    trackSubtitles = track?.subtitles.orEmpty()

                    // Доступные качества — из файлов трека (метка + лучшая ссылка).
                    val qualities = track?.files.orEmpty().mapNotNull { file ->
                        (file.hls4 ?: file.hls ?: file.http)?.let { StreamQuality(file.quality, it) }
                    }
                    // Предпочитаемое качество из настроек; «Авто»/нет совпадения — лучшее доступное.
                    val initial = qualities.firstOrNull { it.label == settings.quality }
                        ?: qualities.firstOrNull()

                    // Субтитры: «Выкл» + языки из трека.
                    val subtitleOptions = buildList {
                        add(SubtitleOption(PlaybackSettings.SubtitleOff, null))
                        trackSubtitles.forEach { add(SubtitleOption(langDisplay(it.lang), it.lang)) }
                    }
                    val selectedSubtitle = if (settings.subtitleLanguage == PlaybackSettings.SubtitleOff) {
                        subtitleOptions.first()
                    } else {
                        subtitleOptions.firstOrNull { it.label == settings.subtitleLanguage }
                            ?: subtitleOptions.first()
                    }

                    updateState {
                        it.copy(
                            loading = false,
                            item = item,
                            streamUrl = initial?.url,
                            qualities = qualities,
                            currentQuality = initial?.label,
                            subtitles = subtitleOptions,
                            currentSubtitle = selectedSubtitle.label,
                        )
                    }

                    if (initial != null) {
                        player.setMediaItem(buildMediaItem(initial.url))
                        player.prepare()
                        applyTrackPreferences(selectedSubtitle.lang)
                        player.playWhenReady = true
                    }
                }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    showError(result)
                }
            }
        }
    }

    private fun selectQuality(label: String) {
        val quality = state.qualities.firstOrNull { it.label == label } ?: return
        if (label == state.currentQuality) return
        val position = player.currentPosition
        val wasPlaying = player.playWhenReady
        // trackSelectionParameters (аудио/субтитры) живут на плеере и переживают смену MediaItem.
        player.setMediaItem(buildMediaItem(quality.url))
        player.prepare()
        player.seekTo(position)
        player.playWhenReady = wasPlaying
        screenModelScope { updateState { it.copy(currentQuality = label, streamUrl = quality.url) } }
    }

    private fun selectSubtitle(label: String) {
        val option = state.subtitles.firstOrNull { it.label == label } ?: return
        applyTrackPreferences(option.lang)
        screenModelScope { updateState { it.copy(currentSubtitle = label) } }
    }

    /** Собирает MediaItem с вложенными конфигурациями субтитров текущего трека. */
    private fun buildMediaItem(url: String): MediaItem {
        val subtitleConfigs = trackSubtitles.map { subtitle ->
            val mime = if (subtitle.url.endsWith(".vtt", ignoreCase = true)) {
                MimeTypes.TEXT_VTT
            } else {
                MimeTypes.APPLICATION_SUBRIP
            }
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.url))
                .setLanguage(subtitle.lang)
                .setMimeType(mime)
                .build()
        }
        return MediaItem.Builder()
            .setUri(url)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()
    }

    /** Применяет предпочтения дорожек к плееру: язык аудио и язык/выключение субтитров. */
    private fun applyTrackPreferences(subtitleLang: String?) {
        val builder = player.trackSelectionParameters.buildUpon()
        langCode(audioPreference)?.let { builder.setPreferredAudioLanguage(it) }
        if (subtitleLang == null) {
            builder.setPreferredTextLanguage(null)
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            builder.setPreferredTextLanguage(subtitleLang)
        }
        player.trackSelectionParameters = builder.build()
    }

    private fun saveProgress(positionMs: Long) {
        val item = state.item ?: return
        val videoId = item.tracklist.firstOrNull()?.id ?: return
        screenModelScope {
            watching.saveProgress(item.id, videoId, (positionMs / 1000).toInt())
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    private companion object {
        fun langCode(display: String): String? = when (display.lowercase()) {
            "русский" -> "rus"
            "english" -> "eng"
            else -> null // «Оригинал» / неизвестно — пусть плеер выбирает сам
        }

        fun langDisplay(code: String?): String = when (code?.lowercase()) {
            "rus", "ru" -> "Русский"
            "eng", "en" -> "English"
            "ukr", "uk" -> "Українська"
            null, "" -> "Субтитры"
            else -> code
        }
    }
}
