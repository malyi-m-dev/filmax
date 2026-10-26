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
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.catalog.model.subtitleLanguageName
import com.filmax.core.domain.common.ErrorReporting
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.error.RequestFailure
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.TvLinkReceiver
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.player.common.navigation.PlayerRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/**
 * Плеер: загрузка тайтла, выбор дорожек и качества, фолбэк CDN-вариантов, прогресс, перемотка,
 * карточка «Далее» и переходы (другая серия, окно оценки, выбор ТВ, выход). Контракт целен —
 * одна связная машина воспроизведения, дробление раздало бы половину полей в каждый кусок.
 */
class PlayerScreenModel(
    savedStateHandle: SavedStateHandle,
    repositories: PlayerRepositories,
    private val playbackSettings: PlaybackSettingsRepository,
    private val feedbackPrompts: FeedbackPrompts,
    tvLink: TvLinkReceiver,
    private val navigator: Navigator,
    context: Context,
) : BaseScreenModel<PlayerState, PlayerSideEffect, PlayerEvent>(PlayerState()) {

    private val catalog = repositories.catalog
    private val watching = repositories.watching
    private val userRepository = repositories.user

    private val route = savedStateHandle.toRoute<PlayerRoute>()

    /**
     * Основной вариант доставки ещё не заиграл, а за ним есть запасной: ошибка загрузки не
     * повторяется, и плеер сразу уходит на следующий CDN (см. [streamSourceFactory]). Читается
     * потоком загрузки Media3.
     */
    @Volatile
    private var failFast = false

    /** Сторож старта основного варианта (см. [watchStart]). */
    private var startWatch: Job? = null

    /** Тик раз в секунду: снимок, прогресс, отсчёт «Далее». Идёт, пока у потока известна длительность. */
    private var ticker: Job? = null

    /** Подтверждение перемотки пультом — после паузы в нажатиях. */
    private var scrubCommit: Job? = null

    // Шаг перемотки задан явно: дефолты Media3 (5 с назад / 15 с вперёд) не совпадают
    // с иконками Replay10/Forward10 на кнопках плеера.
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(streamSourceFactory(context) { failFast })
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

    /** Играло ли видео, когда открыли нижнюю панель: закрытие продолжит, только если играло. */
    private var resumeAfterPanel = false

    /** Потолок декодера устройства (см. [PlaybackSettingsRepository.decoderLimit]). */
    private var decoderLimit: Int? = null

    /** Экран на виду (см. [PlayerEvent.ScreenShown]): только тогда загрузка запускает видео. */
    private var screenShown = true

    /** Когда зажали стрелку перемотки — от этого считается разгон. */
    private var holdStartedAtMs = 0L

    /** Пульт телефона, если ТВ к нему подключён (см. [PlayerRemoteLink]). */
    private val remote = PlayerRemoteLink(tvLink, player) { state }

    init {
        player.addListener(playerListener())
        screenModelScope { _ -> remote.listen(onPlayEpisode = ::playEpisode, onStop = navigator::back) }
        onFetchData()
    }

    private fun playerListener() = object : Player.Listener {
        // Пульт узнаёт о паузе, буферизации и перемотке сразу, а не со следующим тиком прогресса.
        override fun onEvents(player: Player, events: Player.Events) {
            if (REMOTE_EVENTS.any(events::contains)) remote.publish()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) = onPlayingChanged(isPlaying)

        override fun onPlayerError(error: PlaybackException) = onPlaybackError(error)

        // Аудиодорожки известны только после разбора манифеста — читаем их здесь.
        override fun onTracksChanged(tracks: Tracks) = updateAudioTracks(tracks)

        // Кадр выше потолка всё-таки показан — зритель выбрал его сам, и он идёт (скажем,
        // 4K в HEVC там, где 4K H.264 падал): потолок был ложным, снимаем.
        override fun onRenderedFirstFrame() = liftDecoderLimitIfExceeded()

        override fun onPlaybackStateChanged(playbackState: Int) = onStateChanged(playbackState)
    }

    override fun onEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.TogglePlay -> onTogglePlay()
            is PlayerEvent.SeekBy -> onSeekBy(event.deltaMs)
            is PlayerEvent.Scrub -> onScrub(event.positionMs)
            PlayerEvent.ScrubFinished -> commitScrub()
            is PlayerEvent.SeekStep -> onSeekStep(event)
            is PlayerEvent.SelectQuality -> onSelectQuality(event.label)
            is PlayerEvent.SelectAudio -> onSelectAudio(event.label)
            is PlayerEvent.SelectSubtitle -> onSelectSubtitle(event.label)
            is PlayerEvent.SetSpeed -> onSetSpeed(event.speed)
            PlayerEvent.OpenPanel -> onOpenPanel()
            is PlayerEvent.OpenPanelAt -> onOpenPanel(event.tab)
            PlayerEvent.ClosePanel -> onClosePanel()
            is PlayerEvent.SelectTab -> onSelectTab(event.tab)
            is PlayerEvent.ShowSeason -> onShowSeason(event.season)
            is PlayerEvent.PlayEpisode -> playEpisode(event.season, event.videoId)
            PlayerEvent.PlayNext -> onPlayNext()
            PlayerEvent.DismissAutoNext -> onDismissAutoNext()
            PlayerEvent.ScreenShown -> onScreenShown()
            PlayerEvent.ScreenHidden -> onScreenHidden()
            PlayerEvent.ContinueOnTv -> onContinueOnTv()
            PlayerEvent.Exit -> navigator.back()
        }
    }

    // ── Транспорт ────────────────────────────────────────────────────────────────

    /** Незакоммиченную перемотку не теряем: сначала на выбранную позицию, потом переключаем. */
    private fun onTogglePlay() {
        commitScrub()
        if (player.isPlaying) player.pause() else player.play()
    }

    private fun onSeekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0)
        player.seekTo(target)
        screenModelScope { _ -> updateState { it.copy(playback = it.playback.copy(positionMs = target)) } }
    }

    private fun onScrub(positionMs: Long) {
        screenModelScope { _ -> updateState { it.copy(scrubTargetMs = positionMs) } }
    }

    /** Шаг перемотки пультом: разгон от начала удержания, подтверждение — паузой в нажатиях. */
    private fun onSeekStep(step: PlayerEvent.SeekStep) {
        // Пока длительность неизвестна, ограничено только началом — как у перемотки с пульта телефона.
        val end = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        if (step.repeatCount == 0) holdStartedAtMs = step.eventTimeMs
        val stepMs = seekStepSeconds(step.eventTimeMs - holdStartedAtMs) * MILLIS_IN_SECOND
        val from = state.scrubTargetMs ?: player.currentPosition
        val target = (from + step.direction * stepMs).coerceIn(0L, end)
        screenModelScope { _ -> updateState { it.copy(scrubTargetMs = target) } }
        scrubCommit?.cancel()
        scrubCommit = screenModelScope { _ ->
            delay(SCRUB_COMMIT_TIMEOUT_MS)
            commitScrub()
        }
    }

    /** Подтверждение перемотки: плеер уходит на выбранную позицию, позиция на экране — тоже. */
    private fun commitScrub() {
        scrubCommit?.cancel()
        val target = state.scrubTargetMs ?: return
        player.seekTo(target)
        screenModelScope { _ ->
            updateState { it.copy(scrubTargetMs = null, playback = it.playback.copy(positionMs = target)) }
        }
    }

    private fun onSetSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        screenModelScope { _ -> updateState { it.copy(currentSpeed = speed) } }
    }

    private fun onPlayingChanged(isPlaying: Boolean) {
        screenModelScope { _ -> updateState { it.copy(playback = it.playback.copy(playing = isPlaying)) } }
    }

    private fun onStateChanged(playbackState: Int) {
        val buffering = playbackState == Player.STATE_BUFFERING
        screenModelScope { _ -> updateState { it.copy(playback = it.playback.copy(buffering = buffering)) } }
        when (playbackState) {
            // Поток заиграл — дальше сбои загрузки повторяются как обычно, и начинается тик прогресса.
            Player.STATE_READY -> {
                failFast = false
                startTicker()
            }
            Player.STATE_ENDED -> onWatchFinished()
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = screenModelScope { _ ->
            while (true) {
                delay(PROGRESS_TICK_MS)
                onTick()
            }
        }
    }

    /**
     * Тик раз в секунду: снимок плеера, прогресс на сервер, снимок пульту и отсчёт «Далее».
     * Пока длительность неизвестна, ничего не пишем: ноль лёг бы поверх настоящего прогресса.
     */
    private suspend fun onTick() {
        val duration = player.duration.takeIf { it > 0 } ?: return
        val current = state
        val position = player.currentPosition
        val snapshot = PlaybackSnapshot(
            playing = player.isPlaying,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = position,
            durationMs = duration,
            bufferedMs = player.bufferedPosition,
        )
        val autoNext = current.autoNext.tick(
            remainingMs = duration - position,
            enabled = current.nextTrack != null,
            playing = player.isPlaying,
            scrubbing = current.scrubTargetMs != null,
        )
        updateState { it.copy(playback = snapshot, autoNext = autoNext) }
        saveProgress(position)
        remote.publish()
        if (autoNext.expired) onPlayNext()
    }

    private fun onPlayNext() {
        val next = state.nextTrack ?: return
        screenModelScope { _ -> updateState { it.copy(autoNext = it.autoNext.copy(visible = false)) } }
        playEpisode(next)
    }

    private fun onDismissAutoNext() {
        screenModelScope { _ ->
            updateState { it.copy(autoNext = it.autoNext.copy(visible = false, dismissed = true)) }
        }
    }

    /**
     * Серия доиграла. Есть следующая и зритель не отменил карточку «Далее» — сразу она, иначе плеер
     * застывал бы на титрах. Дальше ничего не играет (фильм или последняя серия) — досмотр
     * засчитывается и, если пора, плеер уступает место окну оценки: «Назад» из него ведёт в детали.
     */
    private fun onWatchFinished() {
        screenModelScope { current ->
            val next = current.nextTrack
            when {
                next != null && !current.autoNext.dismissed -> playEpisode(next)
                next == null && feedbackPrompts.afterWatchFinished() ->
                    navigator.replace(Destination.Feedback(FeedbackTrigger.AfterWatch.name))
            }
        }
    }

    private fun playEpisode(track: MediaTrack) = playEpisode(track.seasonNumber, track.number)

    /** Другая серия — замена экрана плеера: прогресс модель пишет в трек, выбранный при старте. */
    private fun playEpisode(season: Int, videoId: Int) {
        navigator.replace(Destination.Player(itemId = route.itemId, videoId = videoId, season = season))
    }

    private fun onScreenShown() {
        screenShown = true
        remote.publish()
    }

    private fun onScreenHidden() {
        screenShown = false
        player.pause()
        remote.withdraw()
    }

    /** Эта серия с текущей позиции — на телевизор; плеер телефона встанет на паузу, уходя с экрана. */
    private fun onContinueOnTv() {
        val item = state.item ?: return
        val track = selectedTrack
        val play = RemoteCommand.Play(
            itemId = item.id,
            videoId = track?.number ?: route.videoId,
            season = track?.seasonNumber ?: route.season,
            startSeconds = (player.currentPosition / MILLIS_IN_SECOND).toInt(),
        )
        navigator.open(Destination.TvPicker(play.itemId, play.videoId, play.season, play.startSeconds))
    }

    // ── Панель ───────────────────────────────────────────────────────────────────

    /**
     * Панель открывается поверх остановленного кадра: выбирать дорожку или серию, пока фильм идёт
     * дальше, — значит пропустить кусок. Было ли воспроизведение, запоминаем — закрытие его вернёт.
     * [tab] — вкладка, на которой открыть; её нет среди доступных — открывается как обычно.
     */
    private fun onOpenPanel(tab: PlayerTab? = null) {
        if (state.panelOpen) return
        commitScrub()
        resumeAfterPanel = player.playWhenReady
        player.pause()
        screenModelScope { current ->
            val opening = tab?.takeIf { wanted -> wanted in current.panelTabs() } ?: current.openingTab()
            val season = current.track?.seasonNumber
            updateState { it.copy(panelOpen = true, panelTab = opening, panelSeason = season) }
        }
    }

    private fun onClosePanel() {
        if (!state.panelOpen) return
        if (resumeAfterPanel) player.play()
        screenModelScope { _ -> updateState { it.copy(panelOpen = false) } }
    }

    private fun onSelectTab(tab: PlayerTab) {
        screenModelScope { _ -> updateState { it.copy(panelTab = tab) } }
    }

    private fun onShowSeason(season: Int) {
        screenModelScope { _ -> updateState { it.copy(panelSeason = season) } }
    }

    // ── Загрузка ─────────────────────────────────────────────────────────────────

    /**
     * Подписка проверяется параллельно с загрузкой и не блокирует старт: без неё поток всё равно
     * не пойдёт, а плашка объяснит почему. Ошибка профиля (офлайн и т.п.) плашку не показывает.
     */
    private fun checkSubscription() {
        screenModelScope { _ ->
            val profile = userRepository.getProfile()
            if (profile is RequestResult.Success && profile.data.subscription?.active != true) {
                updateState { it.copy(subscriptionRequired = true) }
            }
        }
    }

    override fun onFetchData() {
        checkSubscription()
        screenModelScope { _ ->
            val settings = playbackSettings.settings.first()
            audioPreference = settings.audioLanguage
            savedVoiceKey = playbackSettings.voiceKeyFor(route.itemId)
            // Потолок Full HD и ниже мог остаться от 2.2.0, где его записывал занятый декодер, —
            // такой не действует.
            decoderLimit = playbackSettings.decoderLimit()?.let(QualityLadder::limitOf)
            when (val result = catalog.getItemDetails(route.itemId)) {
                is RequestResult.Success -> onItemLoaded(result.data, settings)
                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    showError(result)
                }
            }
        }
    }

    /**
     * Сериал: играем выбранный эпизод. `videoId` — это НОМЕР видео (`number` из API), а не id
     * трека: тем же числом kino.pub принимает и отдаёт прогресс в watching/marktime. Номер
     * уникален только внутри сезона, поэтому сезон обязателен в матчинге — без него S3E2 находил
     * бы S1E2. Фильм/нет совпадения — первый трек.
     */
    private suspend fun onItemLoaded(item: Item, settings: PlaybackSettings) {
        val trackIndex = item.tracklist.indexOfFirst { matchesRoute(it, route) }.coerceAtLeast(0)
        val track = item.tracklist.getOrNull(trackIndex)
        selectedTrack = track
        trackSubtitles = track?.subtitles.orEmpty()

        val qualities = streamQualities(track)
        // Предпочитаемое качество из настроек; «Авто»/нет совпадения — лучшее доступное,
        // но не выше того, на чём декодер устройства уже падал.
        val initial = QualityLadder.start(qualities, settings.quality, decoderLimit)
        val subtitleOptions = subtitleOptions()
        val selectedSubtitle = subtitleOptions.firstOrNull { it.label == settings.subtitleLanguage }
            ?: subtitleOptions.first()
        val nextTrack = item.tracklist.getOrNull(trackIndex + 1)
        val previousTrack = item.tracklist.getOrNull(trackIndex - 1)
        updateState {
            it.copy(
                loading = false,
                item = item,
                track = track,
                nextTrack = nextTrack,
                previousTrack = previousTrack,
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
            beginStream(initial.url, primaryWithBackup = initial.urls.size > 1)
            applyTrackPreferences(selectedSubtitle.lang)
            startSeconds(track)?.let { player.seekTo(it * MILLIS_IN_SECOND) }
            player.playWhenReady = screenShown
        }
    }

    /** Субтитры: «Выкл» + языки из трека. */
    private fun subtitleOptions(): List<SubtitleOption> = buildList {
        add(SubtitleOption(PlaybackSettings.SubtitleOff, null))
        trackSubtitles.forEach { add(SubtitleOption(subtitleLanguageName(it.lang), it.lang)) }
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

    // ── Качество и варианты доставки ─────────────────────────────────────────────

    private fun onSelectQuality(label: String) {
        val quality = state.qualities.firstOrNull { it.label == label } ?: return
        if (label == state.currentQuality) return
        ErrorReporting.reporter.log("player: quality $label host=${urlHost(quality.url)}")
        switchQuality(quality)
    }

    /** Смена качества с той же позиции и с тем же состоянием паузы. */
    private fun switchQuality(quality: StreamQuality) {
        val position = player.currentPosition
        val wasPlaying = player.playWhenReady
        streamVariantIndex = 0
        // trackSelectionParameters (аудио/субтитры) живут на плеере и переживают смену MediaItem.
        beginStream(quality.url, primaryWithBackup = quality.urls.size > 1)
        player.seekTo(position)
        player.playWhenReady = wasPlaying
        screenModelScope { _ -> updateState { it.copy(currentQuality = quality.label, streamUrl = quality.url) } }
    }

    /**
     * Ошибка плеера. Декодер не тянет кадр (4K H.264 на слабой приставке) — ни другой CDN, ни
     * повтор не помогут: сразу качество ниже. Сбой на Full HD и ниже — не предел железа (см.
     * QualityLadder.limitOf): его разбирает повтор. Ошибка источника часто значит «CDN этого
     * варианта недоступен» (DPI/SNI-блокировка srvkp.com): прежде чем показывать модалку, пробуем
     * следующий вариант доставки.
     */
    private fun onPlaybackError(error: PlaybackException) {
        val failedHeight = videoDecoderFailureHeight(error)?.let(QualityLadder::limitOf)
        if (failedHeight != null && stepDownBelow(failedHeight)) return
        // Ошибки плеера не проходят через safeRequest — репортим сами, иначе телеметрия не увидит
        // именно тот класс сбоев, на который жалуются («серия не запустилась»).
        ErrorReporting.reporter.report(RequestFailure.of(AppError.Playback, error))
        if (!playNextStreamVariant()) {
            screenModelScope { _ -> showError(AppError.Playback) }
        }
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
        screenModelScope { _ -> playbackSettings.setDecoderLimit(decoderLimit) }
        switchQuality(lower)
        return true
    }

    private fun liftDecoderLimitIfExceeded() {
        val limit = decoderLimit
        val height = state.currentQuality?.let(QualityLadder::heightOf)
        if (limit == null || height == null || height < limit) return
        ErrorReporting.reporter.log("player: ${height}p plays, decoder limit ${limit}p lifted")
        decoderLimit = null
        screenModelScope { _ -> playbackSettings.setDecoderLimit(null) }
    }

    /** Хлебная крошка старта: при ошибке в отчёте видно тайтл, качество и CDN-хост. */
    private fun reportPlaybackStart(initial: StreamQuality) {
        ErrorReporting.reporter.log(
            "player: start item=${route.itemId} quality=${initial.label} host=${urlHost(initial.url)}",
        )
    }

    /**
     * Переключает поток на следующий вариант доставки текущего качества (hls4 → http → hls).
     * Варианты ведут на разные CDN-хосты, и недоступность одного (SNI-блокировка srvkp.com)
     * не значит, что тайтл не посмотреть. false — варианты кончились, ошибку показывает вызывающий.
     */
    private fun playNextStreamVariant(): Boolean {
        val quality = state.qualities.firstOrNull { it.label == state.currentQuality }
        val nextUrl = quality?.urls?.getOrNull(streamVariantIndex + 1) ?: return false
        streamVariantIndex++
        ErrorReporting.reporter.log("player: variant fallback #$streamVariantIndex host=${urlHost(nextUrl)}")
        val position = player.currentPosition
        // Состояние воспроизведения переносим как есть: сбой CDN — не повод запускать видео
        // у того, кто стоял на паузе.
        val wasPlaying = player.playWhenReady
        beginStream(nextUrl, primaryWithBackup = false)
        if (position > 0) player.seekTo(position)
        player.playWhenReady = wasPlaying
        screenModelScope { _ -> updateState { it.copy(streamUrl = nextUrl) } }
        return true
    }

    /**
     * Ставит в плеер вариант доставки [url]. [primaryWithBackup] — это основной вариант, и за ним
     * есть запасной: пока поток не заиграл, сбой загрузки не повторяется ([failFast]), а молчание
     * обрывает сторож старта. Запасные живут на другом CDN и грузятся как обычно, с повторами.
     */
    private fun beginStream(url: String, primaryWithBackup: Boolean) {
        failFast = primaryWithBackup
        player.setMediaItem(buildMediaItem(url))
        player.prepare()
        startWatch?.cancel()
        if (primaryWithBackup) startWatch = screenModelScope { _ -> watchStart(url) }
    }

    /**
     * Сторож старта: основной вариант, который за [START_TIMEOUT_MS] не отдал ни секунды видео,
     * сменяется запасным, не дожидаясь ошибки, — заблокированный хост молчит до неё полминуты.
     * Медленный, но живой CDN что-то уже отдал, и его не трогаем.
     */
    private suspend fun watchStart(url: String) {
        delay(START_TIMEOUT_MS)
        if (!failFast || player.totalBufferedDuration > 0) return
        ErrorReporting.reporter.log("player: no data in ${START_TIMEOUT_MS}ms host=${urlHost(url)}")
        playNextStreamVariant()
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

    // ── Дорожки ──────────────────────────────────────────────────────────────────

    private fun onSelectSubtitle(label: String) {
        val option = state.subtitles.firstOrNull { it.label == label } ?: return
        applyTrackPreferences(option.lang)
        screenModelScope { _ -> updateState { it.copy(currentSubtitle = label) } }
    }

    private fun onSelectAudio(label: String) {
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
        val options = audioGroups.mapIndexed { index, group -> audioOption(index, group, apiAudios) }

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
        val audioTracks = if (options.size > 1) options else emptyList()
        val currentAudio = options.getOrNull(selectedIndex)?.label ?: options.firstOrNull()?.label.orEmpty()
        screenModelScope { _ -> updateState { it.copy(audioTracks = audioTracks, currentAudio = currentAudio) } }
    }

    /** Применяет предпочтения дорожек к плееру: язык аудио и язык/выключение субтитров. */
    private fun applyTrackPreferences(subtitleLang: String?) {
        val builder = player.trackSelectionParameters.buildUpon()
        languageCodeOf(audioPreference)?.let { builder.setPreferredAudioLanguage(it) }
        if (subtitleLang == null) {
            builder.setPreferredTextLanguage(null)
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            builder.setPreferredTextLanguage(subtitleLang)
        }
        player.trackSelectionParameters = builder.build()
    }

    // ── Прогресс ─────────────────────────────────────────────────────────────────

    /**
     * Пишет прогресс на сервер. `video` — это НОМЕР видео (`MediaTrack.number`), а не id трека:
     * kino.pub в `watching/marktime` ждёт именно номер, и тем же числом отдаёт прогресс обратно
     * в `items/{id}`. С id прогресс уходил «в никуда» — история оставалась пустой.
     *
     * Троттлинг по позиции: пока не отъехали от последней отправки дальше [PROGRESS_STEP_SECONDS],
     * не дёргаем сервер — тик идёт раз в секунду, а это на порядок чаще, чем нужно.
     */
    private fun saveProgress(positionMs: Long) {
        val item = state.item
        val track = selectedTrack
        if (item == null || track == null) return
        val seconds = (positionMs / MILLIS_IN_SECOND).toInt()
        val sent = lastSentSeconds
        if (sent != null && abs(seconds - sent) < PROGRESS_STEP_SECONDS) return
        lastSentSeconds = seconds
        screenModelScope { _ ->
            // Сериалы прогресс пишут по сезону+эпизоду, фильмы — по одному видео.
            if (track.seasonNumber > 0) {
                watching.saveProgressSerial(item.id, track.seasonNumber, track.number, seconds)
            } else {
                watching.saveProgress(item.id, track.number, seconds)
            }
        }
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

        /** Порог отправки прогресса: реже, чем тик плеера (1 с), но чаще, чем теряется место. */
        const val PROGRESS_STEP_SECONDS = 5

        /** Ключ отчёта Crashlytics: высота кадра, на которой декодер устройства не справился. */
        const val DECODER_LIMIT_KEY = "decoder_limit"
    }
}
