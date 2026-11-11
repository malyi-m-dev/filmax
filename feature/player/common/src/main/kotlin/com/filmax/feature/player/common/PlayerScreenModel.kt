package com.filmax.feature.player.common

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.core.domain.catalog.model.ref
import com.filmax.core.domain.common.ErrorReporting
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.error.RequestFailure
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.source.capabilities
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.TvLinkReceiver
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.player.common.navigation.PlayerRoute
import kotlinx.coroutines.flow.first

// Контракт плеера целен: загрузка, выбор дорожек/качества, фолбэк CDN-вариантов и прогресс —
// одна связная машина воспроизведения, дробление раздало бы половину полей в каждый кусок.
@Suppress("TooManyFunctions")
class PlayerScreenModel(
    savedStateHandle: SavedStateHandle,
    repositories: PlayerRepositories,
    private val playbackSettings: PlaybackSettingsRepository,
    private val feedbackPrompts: FeedbackPrompts,
    tvLink: TvLinkReceiver,
    context: Context,
) : BaseScreenModel<PlayerState, PlayerSideEffect, PlayerEvent>(PlayerState()) {

    private val catalog = repositories.catalog
    private val watching = repositories.watching
    private val userRepository = repositories.user
    private val streams = repositories.streams
    private val sources = repositories.sources

    private val route = savedStateHandle.toRoute<PlayerRoute>()

    // Шаг перемотки задан явно: дефолты Media3 (5 с назад / 15 с вперёд) не совпадают
    // с иконками Replay10/Forward10 на кнопках плеера. Повторы загрузки решает [fallback]:
    // лямбда читается потоком загрузки, когда он уже создан.
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(streamSourceFactory(context) { fallback.failFast })
        .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
        .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
        .build()

    /** Субтитры текущего трека — нужны, чтобы пересобрать MediaItem при смене качества. */
    private var trackSubtitles: List<SubtitleTrack> = emptyList()
    private var audioPreference: String = PlaybackSettings.AudioOriginal

    /** Варианты доставки текущего качества и переход между CDN. */
    private val fallback = StreamFallback(
        player = player,
        mediaItemOf = { url -> mediaItemOf(url, trackSubtitles) },
        launch = { block -> screenModelScope { block() } },
        onSwitched = { url -> screenModelScope { updateState { it.copy(streamUrl = url) } } },
    )

    /** Озвучки: меню, выбор и память на тайтл. */
    private val audio = PlayerAudio(player)

    private val progress = ProgressThrottle()

    /** Выбранный трек/эпизод — нужен для сохранения прогресса (сериалы пишутся по сезону). */
    private var selectedTrack: MediaTrack? = null

    /** Играло ли видео, когда открыли нижнюю панель: закрытие продолжит, только если играло. */
    private var resumeAfterPanel = false

    /** Потолок декодера устройства (см. [PlaybackSettingsRepository.decoderLimit]). */
    private var decoderLimit: Int? = null

    /** Экран на виду (см. [PlayerEvent.ScreenShown]): только тогда загрузка запускает видео. */
    private var screenShown = true

    /** Пульт телефона, если ТВ к нему подключён (см. [PlayerRemoteLink]). */
    private val remote = PlayerRemoteLink(tvLink, player) { state }

    init {
        player.addListener(object : Player.Listener {
            // Пульт узнаёт о паузе, буферизации и перемотке сразу, а не со следующим тиком прогресса.
            override fun onEvents(player: Player, events: Player.Events) {
                if (REMOTE_EVENTS.any(events::contains)) remote.publish()
            }

            override fun onPlayerError(error: PlaybackException) {
                // Декодер не тянет кадр (4K H.264 на слабой приставке) — ни другой CDN, ни
                // повтор не помогут: сразу качество ниже. Раньше плеер перебирал CDN-варианты
                // того же 4K и падал сериями по 5–11 ошибок, а потом показывал модалку. Сбой на
                // Full HD и ниже — не предел железа (см. QualityLadder.limitOf): его разбирает повтор.
                val failedHeight = error.videoDecoderFailureHeight()?.let(QualityLadder::limitOf)
                if (failedHeight != null && stepDownBelow(failedHeight)) return
                // Ошибки плеера не проходят через safeRequest — репортим сами, иначе телеметрия
                // не увидит именно тот класс сбоев, на который жалуются («серия не запустилась»).
                // Обёртка RequestFailure даёт читаемый заголовок issue (см. reportRequestFailure).
                ErrorReporting.reporter.report(RequestFailure.of(AppError.Playback, error))
                // Ошибка источника часто значит «CDN этого варианта недоступен» (DPI/SNI-блокировка
                // srvkp.com): прежде чем показывать модалку, пробуем следующий вариант доставки.
                if (!fallback.next()) {
                    screenModelScope { showError(AppError.Playback) }
                }
            }

            // Аудиодорожки известны только после разбора манифеста — читаем их здесь.
            override fun onTracksChanged(tracks: Tracks) = updateAudioTracks(tracks)

            // Кадр выше потолка всё-таки показан — зритель выбрал его сам, и он идёт (скажем,
            // 4K в HEVC там, где 4K H.264 падал): потолок был ложным, снимаем.
            override fun onRenderedFirstFrame() = liftDecoderLimitIfExceeded()

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    // Поток заиграл — дальше сбои загрузки повторяются как обычно.
                    Player.STATE_READY -> fallback.onReady()
                    Player.STATE_ENDED -> onWatchFinished()
                }
            }
        })
        screenModelScope { remote.listen(::postSideEffect) }
        onFetchData()
    }

    override fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.SaveProgress -> {
                saveProgress(event.positionMs)
                remote.publish()
            }
            is PlayerEvent.SelectQuality -> selectQuality(event.label)
            is PlayerEvent.SelectAudio -> selectAudio(event.label)
            is PlayerEvent.SelectSubtitle -> selectSubtitle(event.label)
            // Скорость сессионная и простая: меняем на плеере и в state прямо тут. Отдельный
            // метод перевёл бы класс за порог TooManyFunctions detekt — незачем.
            is PlayerEvent.SetSpeed -> {
                player.setPlaybackSpeed(event.speed)
                screenModelScope { updateState { it.copy(currentSpeed = event.speed) } }
            }
            PlayerEvent.OpenPanel -> openPanel()
            is PlayerEvent.OpenPanelAt -> openPanel(event.tab)
            PlayerEvent.ClosePanel -> closePanel()
            is PlayerEvent.SelectTab -> screenModelScope { updateState { it.copy(panelTab = event.tab) } }
            is PlayerEvent.ShowSeason -> screenModelScope { updateState { it.copy(panelSeason = event.season) } }
            PlayerEvent.ScreenShown -> {
                screenShown = true
                remote.publish()
            }
            PlayerEvent.ScreenHidden -> {
                screenShown = false
                player.pause()
                remote.withdraw()
            }
            PlayerEvent.ContinueOnTv -> continueOnTv()
        }
    }

    /** Эта серия с текущей позиции — на телевизор; плеер телефона встанет на паузу, уходя с экрана. */
    private fun continueOnTv() {
        val item = state.item ?: return
        val track = selectedTrack
        val play = RemoteCommand.Play(
            itemId = item.id,
            video = track?.ref ?: route.video,
            startSeconds = (player.currentPosition / MILLIS_IN_SECOND).toInt(),
        )
        screenModelScope { postSideEffect(PlayerSideEffect.ChooseTv(play)) }
    }

    /**
     * Панель открывается поверх остановленного кадра: выбирать дорожку или серию, пока фильм идёт
     * дальше, — значит пропустить кусок. Было ли воспроизведение, запоминаем — закрытие его вернёт.
     */
    /** [tab] — вкладка, на которой открыть; её нет среди доступных — открывается как обычно. */
    private fun openPanel(tab: PlayerTab? = null) {
        if (state.panelOpen) return
        resumeAfterPanel = player.playWhenReady
        player.pause()
        screenModelScope {
            updateState {
                val opening = tab?.takeIf { wanted -> wanted in it.panelTabs() } ?: it.openingTab()
                it.copy(panelOpen = true, panelTab = opening, panelSeason = it.track?.seasonNumber)
            }
        }
    }

    private fun closePanel() {
        if (!state.panelOpen) return
        if (resumeAfterPanel) player.play()
        screenModelScope { updateState { it.copy(panelOpen = false) } }
    }

    /**
     * Подписка проверяется параллельно с загрузкой и не блокирует старт: без неё поток всё равно
     * не пойдёт, а плашка объяснит почему. Ошибка профиля (офлайн и т.п.) плашку не показывает.
     * Источнику без подписки (домашний сервер) проверять нечего.
     */
    private fun checkSubscription() {
        if (!sources.source.value.capabilities.subscription) return
        screenModelScope {
            val profile = userRepository.getProfile()
            if (profile is RequestResult.Success && profile.data.subscription?.active != true) {
                updateState { it.copy(subscriptionRequired = true) }
            }
        }
    }

    override fun onFetchData() {
        checkSubscription()
        screenModelScope {
            val settings = playbackSettings.settings.first()
            audioPreference = settings.audioLanguage
            audio.savedVoiceKey = playbackSettings.voiceKeyFor(route.itemId)
            // Потолок Full HD и ниже мог остаться от 2.2.0, где его записывал занятый декодер, —
            // такой не действует.
            decoderLimit = playbackSettings.decoderLimit()?.let(QualityLadder::limitOf)
            when (val result = catalog.getItemDetails(route.itemId)) {
                is RequestResult.Success -> {
                    val item = result.data
                    // Сериал: играем выбранный эпизод. `videoId` — это НОМЕР видео (`number` из
                    // API), а не id трека: тем же числом kino.pub принимает и отдаёт прогресс
                    // в watching/marktime. Номер уникален только внутри сезона, поэтому сезон
                    // обязателен в матчинге — без него S3E2 находил бы S1E2.
                    // Фильм/нет совпадения — первый трек.
                    val trackIndex = item.tracklist.indexOfFirst { it.matchesRoute(route) }.coerceAtLeast(0)
                    val listed = item.tracklist.getOrNull(trackIndex)
                    val track = listed?.let { playable(item.id, it) ?: return@screenModelScope }
                    selectedTrack = track
                    trackSubtitles = track?.subtitles.orEmpty()

                    val qualities = streamQualities(track)
                    // Предпочитаемое качество из настроек; «Авто»/нет совпадения — лучшее доступное,
                    // но не выше того, на чём декодер устройства уже падал.
                    val initial = QualityLadder.start(qualities, settings.quality, decoderLimit)

                    val subtitleOptions = subtitleOptions(trackSubtitles)
                    val selectedSubtitle = subtitleOptions.preferred(settings.subtitleLanguage)

                    updateState {
                        it.copy(
                            loading = false,
                            item = item,
                            track = track,
                            nextTrack = item.tracklist.getOrNull(trackIndex + 1),
                            previousTrack = item.tracklist.getOrNull(trackIndex - 1),
                            streamUrl = initial?.url,
                            qualities = qualities,
                            currentQuality = initial?.label,
                            subtitles = subtitleOptions,
                            currentSubtitle = selectedSubtitle.label,
                        )
                    }

                    if (initial != null) {
                        reportPlaybackStart(initial)
                        fallback.start(initial)
                        applyTrackPreferences(selectedSubtitle.lang)
                        startSeconds(track)?.let { player.seekTo(it * MILLIS_IN_SECOND) }
                        player.playWhenReady = screenShown
                    }
                }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    showError(result)
                }
            }
        }
    }

    /**
     * Трек с потоками под этот запуск (см. [com.filmax.core.domain.playback.StreamRepository]).
     * null — источник видео не отдал: ошибка уже на экране.
     */
    private suspend fun playable(itemId: Int, track: MediaTrack): MediaTrack? =
        when (val result = streams.playable(itemId, track)) {
            is RequestResult.Success -> result.data
            is RequestResult.Error -> {
                updateState { it.copy(loading = false, error = result.message) }
                showError(result)
                null
            }
        }

    /**
     * С какой секунды начать. Позицию, переданную с телефона, берём как есть: он только что
     * показывал этот кадр. Иначе — сохранённый прогресс, но досмотренный до конца трек начинаем
     * сначала, иначе пересмотр стартовал бы с титров. null — с начала.
     */
    private fun startSeconds(track: MediaTrack?): Int? =
        route.startSeconds.takeIf { it >= 0 }
            ?: track?.watchedSeconds?.takeIf { it > 0 && track.watchStatus != WATCH_STATUS_FINISHED }

    /**
     * Доступные качества — из файлов трека; все варианты доставки в порядке предпочтения,
     * чтобы плееру было куда фолбэчить при недоступном CDN. После hls4 — mp4, а не hls: оба на
     * cdntogo.net, но в hls kino.pub вшивает одну озвучку, а в mp4 лежат все, и при заблокированном
     * api.srvkp.com зритель сохраняет выбор озвучки.
     */
    private fun streamQualities(track: MediaTrack?): List<StreamQuality> =
        track?.files.orEmpty().mapNotNull { file ->
            listOfNotNull(file.hls4, file.http, file.hls)
                .takeIf { it.isNotEmpty() }
                ?.let { StreamQuality(file.quality, it) }
        }

    private fun selectQuality(label: String) {
        val quality = state.qualities.firstOrNull { it.label == label } ?: return
        if (label == state.currentQuality) return
        ErrorReporting.reporter.log("player: quality $label host=${urlHost(quality.url)}")
        switchQuality(quality)
    }

    /** Смена качества с той же позиции и с тем же состоянием паузы. */
    private fun switchQuality(quality: StreamQuality) {
        val position = player.currentPosition
        val wasPlaying = player.playWhenReady
        // trackSelectionParameters (аудио/субтитры) живут на плеере и переживают смену MediaItem.
        fallback.start(quality)
        player.seekTo(position)
        player.playWhenReady = wasPlaying
        screenModelScope { updateState { it.copy(currentQuality = quality.label, streamUrl = quality.url) } }
    }

    /**
     * Декодер не справился с кадром высотой [failedHeight]: потолок запоминается для устройства, и
     * поток уходит на лучшее качество ниже. false — ниже ничего нет, ошибку разбирает вызывающий.
     * Это не баг приложения, а предел железа, поэтому в телеметрию — крошкой и ключом, не событием.
     */
    private fun stepDownBelow(failedHeight: Int): Boolean {
        val lower = QualityLadder.below(state.qualities, failedHeight) ?: return false
        ErrorReporting.reporter.log("player: decoder can't play ${failedHeight}p, step down to ${lower.label}")
        ErrorReporting.reporter.setKey(DECODER_LIMIT_KEY, failedHeight.toString())
        decoderLimit = minOf(failedHeight, decoderLimit ?: failedHeight)
        screenModelScope { playbackSettings.setDecoderLimit(decoderLimit) }
        switchQuality(lower)
        return true
    }

    private fun liftDecoderLimitIfExceeded() {
        val limit = decoderLimit
        val height = state.currentQuality?.let(QualityLadder::heightOf)
        if (limit == null || height == null || height < limit) return
        ErrorReporting.reporter.log("player: ${height}p plays, decoder limit ${limit}p lifted")
        decoderLimit = null
        screenModelScope { playbackSettings.setDecoderLimit(null) }
    }

    /** Хлебная крошка старта: при ошибке в отчёте видно тайтл, качество и CDN-хост. */
    private fun reportPlaybackStart(initial: StreamQuality) {
        ErrorReporting.reporter.log(
            "player: start item=${route.itemId} quality=${initial.label} host=${urlHost(initial.url)}",
        )
    }

    /**
     * Досмотрено до конца: досмотр засчитывается, а окно оценки просится, только если дальше
     * ничего не играет — фильм или последняя доступная серия. Посреди сериала автопереход на
     * следующую серию окном не перебиваем.
     */
    private fun onWatchFinished() {
        screenModelScope {
            val current = state
            val ask = feedbackPrompts.afterWatchFinished()
            if (ask && current.nextTrack == null) postSideEffect(PlayerSideEffect.AskFeedback)
        }
    }

    private fun selectSubtitle(label: String) {
        val option = state.subtitles.firstOrNull { it.label == label } ?: return
        applyTrackPreferences(option.lang)
        screenModelScope { updateState { it.copy(currentSubtitle = label) } }
    }

    private fun selectAudio(label: String) {
        val option = state.audioTracks.firstOrNull { it.label == label } ?: return
        val key = audio.select(option, selectedTrack?.audios.orEmpty()) ?: return
        screenModelScope {
            playbackSettings.setVoiceKey(route.itemId, key)
            updateState { it.copy(currentAudio = label) }
        }
    }

    /** Аудиодорожки известны только после разбора манифеста — меню собирается отсюда. */
    private fun updateAudioTracks(tracks: Tracks) {
        val choice = audio.onTracksChanged(tracks, selectedTrack?.audios.orEmpty())
        screenModelScope { updateState { it.copy(audioTracks = choice.menu, currentAudio = choice.current) } }
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
     * Отправку прореживает [ProgressThrottle]: тик плеера идёт раз в секунду.
     */
    private fun saveProgress(positionMs: Long) {
        val item = state.item
        val track = selectedTrack
        if (item == null || track == null) return
        val seconds = (positionMs / MILLIS_IN_SECOND).toInt()
        if (!progress.shouldSend(seconds)) return
        screenModelScope { watching.saveProgress(item.id, track.ref, seconds) }
    }

    override fun onCleared() {
        remote.withdraw()
        player.release()
        super.onCleared()
    }

    private companion object {
        const val SEEK_INCREMENT_MS = 10_000L
        const val MILLIS_IN_SECOND = 1000L

        /** События плеера, о которых пульт узнаёт сразу: пауза, буферизация, перемотка. */
        val REMOTE_EVENTS = intArrayOf(
            Player.EVENT_IS_PLAYING_CHANGED,
            Player.EVENT_PLAYBACK_STATE_CHANGED,
            Player.EVENT_POSITION_DISCONTINUITY,
        )

        /** Трек маршрута: серия — по сезону и номеру, видео фильма — по номеру. */
        fun MediaTrack.matchesRoute(route: PlayerRoute): Boolean = when (val video = route.video) {
            is VideoRef.Episode -> seasonNumber == video.season && number == video.number
            is VideoRef.Movie -> number == video.number
            null -> false
        }

        /** `watchStatus` из API: 1 — трек досмотрен до конца. */
        const val WATCH_STATUS_FINISHED = 1

        /** Ключ отчёта Crashlytics: высота кадра, на которой декодер устройства не справился. */
        const val DECODER_LIMIT_KEY = "decoder_limit"

        fun langCode(display: String): String? = when (display.lowercase()) {
            "русский" -> "rus"
            "english" -> "eng"
            else -> null // «Оригинал» / неизвестно — пусть плеер выбирает сам
        }
    }
}
