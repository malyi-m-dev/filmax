package com.filmax.feature.player.common

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.player.common.navigation.PlayerRoute
import kotlinx.coroutines.flow.first

class PlayerScreenModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
    private val playbackSettings: PlaybackSettingsRepository,
    context: Context,
) : BaseScreenModel<PlayerState, PlayerSideEffect, PlayerEvent>(PlayerState()) {

    private val route = savedStateHandle.toRoute<PlayerRoute>()

    // Шаг перемотки задан явно: дефолты Media3 (5 с назад / 15 с вперёд) не совпадают
    // с иконками Replay10/Forward10 на кнопках плеера.
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
        .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
        .build()

    /** Субтитры текущего трека — нужны, чтобы пересобрать MediaItem при смене качества. */
    private var trackSubtitles: List<SubtitleTrack> = emptyList()
    private var audioPreference: String = PlaybackSettings.AudioOriginal

    /** Выбранный трек/эпизод — нужен для сохранения прогресса (сериалы пишутся по сезону). */
    private var selectedTrack: MediaTrack? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                screenModelScope { showError(AppError.Playback) }
            }

            // Аудиодорожки известны только после разбора манифеста — читаем их здесь.
            override fun onTracksChanged(tracks: Tracks) = updateAudioTracks(tracks)
        })
        onFetchData()
    }

    override fun dispatch(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.SaveProgress -> saveProgress(event.positionMs)
            is PlayerEvent.SelectQuality -> selectQuality(event.label)
            is PlayerEvent.SelectAudio -> selectAudio(event.label)
            is PlayerEvent.SelectSubtitle -> selectSubtitle(event.label)
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            val settings = playbackSettings.settings.first()
            audioPreference = settings.audioLanguage
            when (val result = catalog.getItemDetails(route.itemId)) {
                is RequestResult.Success -> {
                    val item = result.data
                    // Сериал: играем выбранный эпизод (videoId из маршрута). Фильм/нет совпадения — первый трек.
                    val track = item.tracklist.firstOrNull { it.id == route.videoId }
                        ?: item.tracklist.firstOrNull()
                    selectedTrack = track
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
                        // Продолжаем с сохранённой позиции. Досмотренный до конца трек
                        // начинаем сначала — иначе пересмотр стартовал бы с титров.
                        track?.watchedSeconds
                            ?.takeIf { it > 0 && track.watchStatus != WATCH_STATUS_FINISHED }
                            ?.let { player.seekTo(it * MILLIS_IN_SECOND) }
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
        screenModelScope { _ -> updateState { it.copy(currentQuality = label, streamUrl = quality.url) } }
    }

    private fun selectSubtitle(label: String) {
        val option = state.subtitles.firstOrNull { it.label == label } ?: return
        applyTrackPreferences(option.lang)
        screenModelScope { _ -> updateState { it.copy(currentSubtitle = label) } }
    }

    private fun selectAudio(label: String) {
        val option = state.audioTracks.firstOrNull { it.label == label } ?: return
        val builder = player.trackSelectionParameters.buildUpon()
        option.lang?.let { builder.setPreferredAudioLanguage(it) }
        player.trackSelectionParameters = builder.build()
        screenModelScope { _ -> updateState { it.copy(currentAudio = label) } }
    }

    /** Снимает список аудиодорожек с плеера; селектор показываем только при выборе из нескольких. */
    private fun updateAudioTracks(tracks: Tracks) {
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val options = audioGroups
            .map { group -> group.getTrackFormat(0).language }
            .distinct()
            .map { code -> AudioOption(audioDisplay(code), code) }
        val selectedLang = audioGroups.firstOrNull { it.isSelected }?.getTrackFormat(0)?.language
        screenModelScope { _ ->
            updateState {
                it.copy(
                    audioTracks = if (options.size > 1) options else emptyList(),
                    currentAudio = audioDisplay(selectedLang),
                )
            }
        }
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
        val track = selectedTrack ?: return
        val seconds = (positionMs / 1000).toInt()
        screenModelScope {
            // Сериалы прогресс пишут по сезону+эпизоду, фильмы — по одному видео.
            if (track.seasonNumber > 0) {
                watching.saveProgressSerial(item.id, track.seasonNumber, track.id, seconds)
            } else {
                watching.saveProgress(item.id, track.id, seconds)
            }
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    private companion object {
        const val SEEK_INCREMENT_MS = 10_000L
        const val MILLIS_IN_SECOND = 1000L

        /** `watchStatus` из API: 1 — трек досмотрен до конца. */
        const val WATCH_STATUS_FINISHED = 1

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

        fun audioDisplay(code: String?): String = when (code?.lowercase()) {
            "rus", "ru" -> "Русский"
            "eng", "en" -> "English"
            "ukr", "uk" -> "Українська"
            null, "" -> "Оригинал"
            else -> code
        }
    }
}
