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
import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.catalog.model.audioLanguageName
import com.filmax.core.domain.catalog.model.subtitleLanguageName
import com.filmax.core.domain.common.ErrorReporting
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.error.RequestFailure
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.TvLinkReceiver
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.player.common.navigation.PlayerRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs

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
                if (!playNextStreamVariant()) {
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
                    Player.STATE_READY -> failFast = false
                    Player.STATE_ENDED -> onWatchFinished()
                }
            }
        })
        screenModelScope { _ -> remote.listen(::postSideEffect) }
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
                screenModelScope { _ -> updateState { it.copy(currentSpeed = event.speed) } }
            }
            PlayerEvent.OpenPanel -> openPanel()
            is PlayerEvent.OpenPanelAt -> openPanel(event.tab)
            PlayerEvent.ClosePanel -> closePanel()
            is PlayerEvent.SelectTab -> screenModelScope { _ -> updateState { it.copy(panelTab = event.tab) } }
            is PlayerEvent.ShowSeason -> screenModelScope { _ -> updateState { it.copy(panelSeason = event.season) } }
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
            videoId = track?.number ?: route.videoId,
            season = track?.seasonNumber ?: route.season,
            startSeconds = (player.currentPosition / MILLIS_IN_SECOND).toInt(),
        )
        screenModelScope { _ -> postSideEffect(PlayerSideEffect.ChooseTv(play)) }
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
        screenModelScope { _ ->
            updateState {
                val opening = tab?.takeIf { wanted -> wanted in it.panelTabs() } ?: it.openingTab()
                it.copy(panelOpen = true, panelTab = opening, panelSeason = it.track?.seasonNumber)
            }
        }
    }

    private fun closePanel() {
        if (!state.panelOpen) return
        if (resumeAfterPanel) player.play()
        screenModelScope { _ -> updateState { it.copy(panelOpen = false) } }
    }

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
                is RequestResult.Success -> {
                    val item = result.data
                    // Сериал: играем выбранный эпизод. `videoId` — это НОМЕР видео (`number` из
                    // API), а не id трека: тем же числом kino.pub принимает и отдаёт прогресс
                    // в watching/marktime. Номер уникален только внутри сезона, поэтому сезон
                    // обязателен в матчинге — без него S3E2 находил бы S1E2.
                    // Фильм/нет совпадения — первый трек.
                    val trackIndex = item.tracklist.indexOfFirst { it.matchesRoute(route) }.coerceAtLeast(0)
                    val track = item.tracklist.getOrNull(trackIndex)
                    selectedTrack = track
                    trackSubtitles = track?.subtitles.orEmpty()

                    val qualities = streamQualities(track)
                    // Предпочитаемое качество из настроек; «Авто»/нет совпадения — лучшее доступное,
                    // но не выше того, на чём декодер устройства уже падал.
                    val initial = QualityLadder.start(qualities, settings.quality, decoderLimit)

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
                        streamVariantIndex = 0
                        reportPlaybackStart(initial)
                        beginStream(initial.url, primaryWithBackup = initial.urls.size > 1)
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
        streamVariantIndex = 0
        // trackSelectionParameters (аудио/субтитры) живут на плеере и переживают смену MediaItem.
        beginStream(quality.url, primaryWithBackup = quality.urls.size > 1)
        player.seekTo(position)
        player.playWhenReady = wasPlaying
        screenModelScope { _ -> updateState { it.copy(currentQuality = quality.label, streamUrl = quality.url) } }
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
     * Досмотрено до конца: досмотр засчитывается, а окно оценки просится, только если дальше
     * ничего не играет — фильм или последняя доступная серия. Посреди сериала автопереход на
     * следующую серию окном не перебиваем.
     */
    private fun onWatchFinished() {
        screenModelScope { current ->
            val ask = feedbackPrompts.afterWatchFinished()
            if (ask && current.nextTrack == null) postSideEffect(PlayerSideEffect.AskFeedback)
        }
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

        /** Хост из URL — для хлебных крошек телеметрии (сам URL с подписью в логи не пишем). */
        fun urlHost(url: String): String = url.substringAfter("://").substringBefore("/")

        /** Трек маршрута: номер видео + сезон (у фильма сезона нет — совпадения по номеру достаточно). */
        fun MediaTrack.matchesRoute(route: PlayerRoute): Boolean =
            number == route.videoId && (route.season <= 0 || seasonNumber == route.season)

        /** `watchStatus` из API: 1 — трек досмотрен до конца. */
        const val WATCH_STATUS_FINISHED = 1

        /** Порог отправки прогресса: реже, чем тик плеера (1 с), но чаще, чем теряется место. */
        const val PROGRESS_STEP_SECONDS = 5

        /** Ключ отчёта Crashlytics: высота кадра, на которой декодер устройства не справился. */
        const val DECODER_LIMIT_KEY = "decoder_limit"

        fun langCode(display: String): String? = when (display.lowercase()) {
            "русский" -> "rus"
            "english" -> "eng"
            else -> null // «Оригинал» / неизвестно — пусть плеер выбирает сам
        }

        fun langDisplay(code: String?): String = subtitleLanguageName(code)

        fun audioDisplay(code: String?): String = audioLanguageName(code)

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
         * Вариант дорожки для меню: уникальная подпись ([audioLabel]) плюс те же метаданные по
         * частям — ТВ раскладывает их по строке варианта (язык и тип, студия, каналы).
         */
        fun audioOption(groupIndex: Int, group: Tracks.Group, apiAudios: List<AudioTrack>): AudioOption {
            val meta = apiAudios.firstOrNull { it.index == groupIndex + 1 }
            return AudioOption(
                label = audioLabel(groupIndex, group, apiAudios),
                groupIndex = groupIndex,
                languageCode = meta?.lang ?: group.getTrackFormat(0).language,
                voiceType = meta?.voiceType,
                voiceAuthor = meta?.voiceAuthor,
                channels = meta?.channels ?: group.getTrackFormat(0).channelCount.coerceAtLeast(0),
            )
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
