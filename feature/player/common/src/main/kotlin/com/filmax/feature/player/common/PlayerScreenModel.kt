package com.filmax.feature.player.common

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.common.ErrorReporting
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.player.common.navigation.PlayerRoute
import kotlinx.coroutines.flow.first
import kotlin.math.abs

// Контракт плеера целен: загрузка, выбор дорожек/качества, фолбэк CDN-вариантов и прогресс —
// одна связная машина воспроизведения, дробление раздало бы половину полей в каждый кусок.
@Suppress("TooManyFunctions")
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

    /** Аудиогруппы последнего onTracksChanged — по ним selectAudio делает точечный override. */
    private var audioGroups: List<Tracks.Group> = emptyList()

    /**
     * Озвучка, выбранная для этого тайтла (язык|тип|студия). Читается при загрузке и
     * применяется к КАЖДОМУ onTracksChanged: так следующая серия сериала стартует с той же
     * студией, а смена качества не сбрасывает выбор. Обновляется при ручном выборе дорожки.
     */
    private var savedVoiceKey: String? = null

    /** Позиция последней отправки прогресса — база для троттлинга в [saveProgress]. */
    private var lastSentSeconds: Int? = null

    /** Индекс текущего варианта доставки в [StreamQuality.urls]; сбрасывается сменой качества. */
    private var streamVariantIndex = 0

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Ошибки плеера не проходят через safeRequest — репортим сами, иначе телеметрия
                // не увидит именно тот класс сбоев, на который жалуются («серия не запустилась»).
                ErrorReporting.reporter.report(error)
                // Ошибка источника часто значит «CDN этого варианта недоступен» (DPI/SNI-блокировка
                // srvkp.com): прежде чем показывать модалку, пробуем следующий вариант доставки.
                if (!playNextStreamVariant()) {
                    screenModelScope { showError(AppError.Playback) }
                }
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
            // Скорость сессионная и простая: меняем на плеере и в state прямо тут. Отдельный
            // метод перевёл бы класс за порог TooManyFunctions detekt — незачем.
            is PlayerEvent.SetSpeed -> {
                player.setPlaybackSpeed(event.speed)
                screenModelScope { _ -> updateState { it.copy(currentSpeed = event.speed) } }
            }
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            val settings = playbackSettings.settings.first()
            audioPreference = settings.audioLanguage
            savedVoiceKey = playbackSettings.voiceKeyFor(route.itemId)
            when (val result = catalog.getItemDetails(route.itemId)) {
                is RequestResult.Success -> {
                    val item = result.data
                    // Сериал: играем выбранный эпизод. `videoId` — это НОМЕР видео (`number` из
                    // API), а не id трека: тем же числом kino.pub принимает и отдаёт прогресс
                    // в watching/marktime. Номер уникален только внутри сезона, поэтому сезон
                    // обязателен в матчинге — без него S3E2 находил бы S1E2.
                    // Фильм/нет совпадения — первый трек.
                    val track = item.tracklist.firstOrNull { it.matchesRoute(route) }
                        ?: item.tracklist.firstOrNull()
                    selectedTrack = track
                    trackSubtitles = track?.subtitles.orEmpty()

                    // Доступные качества — из файлов трека; все варианты доставки в порядке
                    // предпочтения, чтобы плееру было куда фолбэчить при недоступном CDN.
                    val qualities = track?.files.orEmpty().mapNotNull { file ->
                        listOfNotNull(file.hls4, file.hls, file.http)
                            .takeIf { it.isNotEmpty() }
                            ?.let { StreamQuality(file.quality, it) }
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
                        streamVariantIndex = 0
                        reportPlaybackStart(initial)
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
        streamVariantIndex = 0
        ErrorReporting.reporter.log("player: quality $label host=${urlHost(quality.url)}")
        // trackSelectionParameters (аудио/субтитры) живут на плеере и переживают смену MediaItem.
        player.setMediaItem(buildMediaItem(quality.url))
        player.prepare()
        player.seekTo(position)
        player.playWhenReady = wasPlaying
        screenModelScope { _ -> updateState { it.copy(currentQuality = label, streamUrl = quality.url) } }
    }

    /**
     * Переключает поток на следующий вариант доставки текущего качества (hls4 → hls → http).
     * Варианты ведут на разные CDN-хосты, и недоступность одного (SNI-блокировка srvkp.com)
     * не значит, что тайтл не посмотреть. false — варианты кончились, ошибку показывает вызывающий.
     */
    /** Хлебная крошка старта: при ошибке в отчёте видно тайтл, качество и CDN-хост. */
    private fun reportPlaybackStart(initial: StreamQuality) {
        ErrorReporting.reporter.log(
            "player: start item=${route.itemId} quality=${initial.label} host=${urlHost(initial.url)}",
        )
    }

    private fun playNextStreamVariant(): Boolean {
        val quality = state.qualities.firstOrNull { it.label == state.currentQuality }
        val nextUrl = quality?.urls?.getOrNull(streamVariantIndex + 1) ?: return false
        streamVariantIndex++
        ErrorReporting.reporter.log("player: variant fallback #$streamVariantIndex host=${urlHost(nextUrl)}")
        val position = player.currentPosition
        // Состояние воспроизведения переносим как есть: сбой CDN — не повод запускать видео
        // у того, кто стоял на паузе.
        val wasPlaying = player.playWhenReady
        player.setMediaItem(buildMediaItem(nextUrl))
        player.prepare()
        if (position > 0) player.seekTo(position)
        player.playWhenReady = wasPlaying
        screenModelScope { _ -> updateState { it.copy(streamUrl = nextUrl) } }
        return true
    }

    private fun selectSubtitle(label: String) {
        val option = state.subtitles.firstOrNull { it.label == label } ?: return
        applyTrackPreferences(option.lang)
        screenModelScope { _ -> updateState { it.copy(currentSubtitle = label) } }
    }

    private fun selectAudio(label: String) {
        val option = state.audioTracks.firstOrNull { it.label == label } ?: return
        val group = audioGroups.getOrNull(option.groupIndex) ?: return
        // Точечный override на конкретную группу: предпочитаемый ЯЗЫК не различил бы несколько
        // русских озвучек разных студий.
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
        // Запоминаем озвучку на весь тайтл: следующие серии стартуют с этой же студии.
        val key = voiceKey(option.groupIndex, group, selectedTrack?.audios.orEmpty())
        savedVoiceKey = key
        screenModelScope { _ ->
            playbackSettings.setVoiceKey(route.itemId, key)
            updateState { it.copy(currentAudio = label) }
        }
    }

    /**
     * Снимает список аудиодорожек с плеера — ВСЕ группы, а не уникальные языки: у тайтла
     * обычно несколько озвучек одного языка (дубляж, многоголоски разных студий, оригинал),
     * и оригинальный клиент kino.pub показывает их полным списком. Подписи — из `audios[]`
     * ответа API (язык · тип · студия); селектор показываем только при выборе из нескольких.
     */
    private fun updateAudioTracks(tracks: Tracks) {
        audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val apiAudios = selectedTrack?.audios.orEmpty()
        val options = audioGroups.mapIndexed { index, group ->
            AudioOption(label = audioLabel(index, group, apiAudios), groupIndex = index)
        }

        // Запомненная озвучка тайтла: находим группу с тем же ключом и ставим override —
        // следующая серия стартует с той же студии, а смена качества не сбрасывает выбор.
        // Не нашлась (у серии другой набор озвучек) — остаёмся на выборе плеера.
        val savedIndex = savedVoiceKey?.let { key ->
            audioGroups.indices.firstOrNull { voiceKey(it, audioGroups[it], apiAudios) == key }
        }
        if (savedIndex != null && !audioGroups[savedIndex].isSelected) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setOverrideForType(TrackSelectionOverride(audioGroups[savedIndex].mediaTrackGroup, 0))
                .build()
        }

        val selectedIndex = savedIndex ?: audioGroups.indexOfFirst { it.isSelected }
        screenModelScope { _ ->
            updateState {
                it.copy(
                    audioTracks = if (options.size > 1) options else emptyList(),
                    currentAudio = options.getOrNull(selectedIndex)?.label
                        ?: options.firstOrNull()?.label.orEmpty(),
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

    /**
     * Пишет прогресс на сервер. `video` — это НОМЕР видео (`MediaTrack.number`), а не id трека:
     * kino.pub в `watching/marktime` ждёт именно номер, и тем же числом отдаёт прогресс обратно
     * в `items/{id}`. С id прогресс уходил «в никуда» — история оставалась пустой.
     *
     * Троттлинг по позиции: пока не отъехали от последней отправки дальше [PROGRESS_STEP_SECONDS],
     * не дёргаем сервер — тик плеера идёт раз в секунду, а это на порядок чаще, чем нужно.
     */
    private fun saveProgress(positionMs: Long) {
        val item = state.item
        val track = selectedTrack
        if (item == null || track == null) return
        val seconds = (positionMs / MILLIS_IN_SECOND).toInt()
        val sent = lastSentSeconds
        if (sent != null && abs(seconds - sent) < PROGRESS_STEP_SECONDS) return
        lastSentSeconds = seconds
        screenModelScope {
            // Сериалы прогресс пишут по сезону+эпизоду, фильмы — по одному видео.
            if (track.seasonNumber > 0) {
                watching.saveProgressSerial(item.id, track.seasonNumber, track.number, seconds)
            } else {
                watching.saveProgress(item.id, track.number, seconds)
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

        /** Хост из URL — для хлебных крошек телеметрии (сам URL с подписью в логи не пишем). */
        fun urlHost(url: String): String = url.substringAfter("://").substringBefore("/")

        /** Трек маршрута: номер видео + сезон (у фильма сезона нет — совпадения по номеру достаточно). */
        fun MediaTrack.matchesRoute(route: PlayerRoute): Boolean =
            number == route.videoId && (route.season <= 0 || seasonNumber == route.season)

        /** `watchStatus` из API: 1 — трек досмотрен до конца. */
        const val WATCH_STATUS_FINISHED = 1

        /** Порог отправки прогресса: реже, чем тик плеера (1 с), но чаще, чем теряется место. */
        const val PROGRESS_STEP_SECONDS = 5

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

        /**
         * Подпись дорожки: «2. Русский · Многоголосый · BaibaKo» — как в оригинальном клиенте
         * kino.pub. Метаданные берём из `audios[]` ответа API, сопоставляя с группой Media3 по
         * порядку (`audios[].index` 1-based = порядок дорожек в HLS-манифесте): сам манифест
         * kino.pub кладёт в NAME только код языка, и по нему озвучки неотличимы. Номер в начале
         * гарантирует уникальность подписи, даже если у двух озвучек совпали студия и тип.
         */
        fun audioLabel(groupIndex: Int, group: Tracks.Group, apiAudios: List<AudioTrack>): String {
            val meta = apiAudios.firstOrNull { it.index == groupIndex + 1 }
            val language = meta?.lang ?: group.getTrackFormat(0).language
            val parts = buildList {
                add(audioDisplay(language))
                meta?.voiceType?.let { add(it) }
                meta?.voiceAuthor?.let { add(it) }
            }.distinct()
            return "${groupIndex + 1}. ${parts.joinToString(" · ")}"
        }

        /**
         * Ключ озвучки для памяти на тайтл: `язык|тип|студия` из метаданных API. Позиционный
         * индекс не годится — у разных серий порядок дорожек может отличаться, а связка
         * язык+тип+студия идентифицирует именно озвучку.
         */
        fun voiceKey(groupIndex: Int, group: Tracks.Group, apiAudios: List<AudioTrack>): String {
            val meta = apiAudios.firstOrNull { it.index == groupIndex + 1 }
            val language = meta?.lang ?: group.getTrackFormat(0).language
            return listOf(language.orEmpty(), meta?.voiceType.orEmpty(), meta?.voiceAuthor.orEmpty())
                .joinToString("|")
        }
    }
}
