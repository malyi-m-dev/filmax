package com.filmax.feature.player.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.error.AppError
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.ui.components.KeepScreenOn
import com.filmax.feature.player.common.PlaybackSpeeds
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerScreenModel
import com.filmax.feature.player.common.PlayerState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/**
 * TV-Плеер: видеоповерхность ExoPlayer и оверлей под пульт поверх неё.
 *
 * [onPlayEpisode] задаёт граф навигации: другая серия — это новый экран плеера с новым
 * [PlayerScreenModel], а не подмена MediaItem (иначе прогресс писался бы в предыдущую серию).
 * null — панели серий и «Следующей серии» не будет.
 */
@Composable
fun TvPlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayEpisode: ((season: Int, videoId: Int) -> Unit)? = null,
    screenModel: PlayerScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val ui = remember(screenModel.player) { TvPlayerUiState(screenModel.player) }

    // Панель серий есть только у сериала и только когда граф дал навигацию по сериям.
    val episodesPanel = remember(state.item, state.track, onPlayEpisode) {
        episodesPanelData(state.item?.tracklist.orEmpty(), state.track, onPlayEpisode)
    }
    val menu = playerMenu(
        state = state,
        episodesPanel = episodesPanel,
        onPlayEpisode = onPlayEpisode,
        dispatch = screenModel::dispatch,
    )

    PlayerEffects(ui = ui, screenModel = screenModel, menu = menu)
    // Ушли с плеера не «Назад», а HOME/лаунчером — пауза: ScreenModel (и ExoPlayer в нём) жив,
    // и без этого звук продолжал играть за пределами приложения (жалоба пользователя).
    LifecycleStartEffect(screenModel.player) {
        onStopOrDispose { screenModel.player.pause() }
    }
    BackHandler { if (!ui.back()) onBack() }

    PlayerContent(ui = ui, state = state, menu = menu, error = appError, modifier = modifier)
}

/** Ряд настроек кадра: пункт показываем только там, где реально есть из чего выбрать. */
private fun playerMenu(
    state: PlayerState,
    episodesPanel: EpisodesPanelData?,
    onPlayEpisode: ((season: Int, videoId: Int) -> Unit)?,
    dispatch: (PlayerEvent) -> Unit,
): PlayerActions = PlayerActions(
    items = buildList {
        if (state.qualities.size > 1) add(SettingsAction.Quality)
        if (state.audioTracks.size > 1) add(SettingsAction.Audio)
        if (state.subtitles.size > 1) add(SettingsAction.Subtitle)
        // Скорость доступна всегда — набор фиксированный, выбирать есть из чего.
        add(SettingsAction.Speed)
        if (episodesPanel != null) add(SettingsAction.Episodes)
        if (state.nextTrack != null && onPlayEpisode != null) add(SettingsAction.NextEpisode)
    },
    options = { action -> action.options(state) },
    selected = { action -> action.selected(state) },
    onSelect = { action, label -> action.toEvent(label)?.let(dispatch) },
    onNextEpisode = {
        state.nextTrack?.let { next -> onPlayEpisode?.invoke(next.seasonNumber, next.number) }
    },
    episodes = episodesPanel,
)

/**
 * Данные панели серий из плейлиста: сезоны отсортированы, стартовый курсор — играющая серия.
 * null — фильм (один трек) или граф не дал [onPlayEpisode].
 */
private fun episodesPanelData(
    tracks: List<MediaTrack>,
    track: MediaTrack?,
    onPlayEpisode: ((season: Int, videoId: Int) -> Unit)?,
): EpisodesPanelData? {
    if (tracks.size < 2 || onPlayEpisode == null) return null
    val seasons = tracks
        .groupBy { it.seasonNumber }
        .toSortedMap()
        .map { (number, episodes) -> number to episodes.sortedBy { it.number } }
    val seasonIndex = seasons.indexOfFirst { it.first == track?.seasonNumber }.coerceAtLeast(0)
    val episodeIndex = seasons.getOrNull(seasonIndex)?.second.orEmpty()
        .indexOfFirst { it.id == track?.id }
        .coerceAtLeast(0)
    return EpisodesPanelData(
        seasons = seasons,
        currentTrackId = track?.id,
        currentSeasonIndex = seasonIndex,
        currentEpisodeIndex = episodeIndex,
        onPlayEpisode = onPlayEpisode,
    )
}

/** Тики и таймеры плеера: прогресс/SaveProgress, автопереход, скраб, автоскрытие оверлея. */
@Composable
private fun PlayerEffects(ui: TvPlayerUiState, screenModel: PlayerScreenModel, menu: PlayerActions) {
    val player = screenModel.player

    // Эффекты живут с ключом player и переживают рекомпозиции, а menu пересобирается, когда
    // доезжает плейлист (при старте треков ещё нет и hasNextEpisode=false) — читаем всегда
    // АКТУАЛЬНЫЙ через rememberUpdatedState, иначе эффекты замкнут пустой первый экземпляр.
    val currentMenu by rememberUpdatedState(menu)

    DisposableEffect(player) {
        ui.isPlaying = player.isPlaying
        ui.isBuffering = player.playbackState == Player.STATE_BUFFERING
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                ui.isPlaying = isPlaying
            }

            // Серия дотекла до конца раньше тика — переходим сразу (если не отменяли «Назад»).
            override fun onPlaybackStateChanged(playbackState: Int) {
                ui.isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED && currentMenu.hasNextEpisode && !ui.autoNextDismissed) {
                    ui.autoNextVisible = false
                    currentMenu.onNextEpisode()
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Тик прогресса и сохранения позиции. Пока длительность неизвестна — не сохраняем: записали бы
    // нулевую позицию поверх реального прогресса. Во время скраббинга позицию ведёт пульт.
    LaunchedEffect(player) {
        while (true) {
            delay(PROGRESS_TICK_MS)
            val duration = player.duration.takeIf { it > 0 } ?: continue
            ui.durationMs = duration
            if (!ui.isScrubbing) ui.positionMs = player.currentPosition
            screenModel.dispatch(PlayerEvent.SaveProgress(player.currentPosition))

            // Автопереход: плашка появляется в конце серии, отсчёт дошёл до нуля — следующая.
            ui.updateAutoNext(
                remainingMs = duration - player.currentPosition,
                enabled = currentMenu.hasNextEpisode,
                playing = ui.isPlaying,
            )
            if (ui.autoNextVisible && ui.autoNextSeconds <= 0) {
                ui.autoNextVisible = false
                currentMenu.onNextEpisode()
            }
        }
    }

    LaunchedEffect(ui.isScrubbing, ui.scrubTargetMs) {
        if (ui.isScrubbing) {
            delay(SCRUB_COMMIT_TIMEOUT_MS)
            ui.commitScrub()
        }
    }

    // Индикатор шага — про последнее нажатие, а не про состояние: живёт мгновение и гаснет.
    LaunchedEffect(ui.seekTick) {
        delay(SEEK_LABEL_HOLD_MS)
        ui.seekLabel = null
    }

    LaunchedEffect(ui.interactionTick, ui.idleHidesOverlay) {
        if (ui.idleHidesOverlay) {
            delay(OVERLAY_AUTO_HIDE_MS)
            ui.visible = false
            ui.seekLabel = null
        }
    }
}

/**
 * Кадр и оверлей. Фокусируемый узел на экране ровно один — корневой Box: он и держит фокус,
 * и разбирает клавиши, поэтому пульт работает одинаково при видимом и скрытом оверлее.
 */
@Composable
private fun PlayerContent(
    ui: TvPlayerUiState,
    state: PlayerState,
    menu: PlayerActions,
    error: AppError?,
    modifier: Modifier = Modifier,
) {
    val keyFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { keyFocus.requestFocus() } }

    // Пока идёт воспроизведение, экран не гаснет; на паузе — обычный таймаут системы.
    KeepScreenOn(enabled = ui.isPlaying)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface)
            .focusRequester(keyFocus)
            .focusable()
            .onKeyEvent { event ->
                when {
                    event.type != KeyEventType.KeyDown -> false
                    // «Назад» не перехватываем: он весь в BackHandler, иначе система не увидит KeyUp.
                    event.key == Key.Back || event.key == Key.Escape -> false
                    else -> ui.onKey(event.key, menu)
                }
            },
    ) {
        VideoSurface(player = ui.player)

        // Спиннер и на буферизации, а не только на загрузке деталей: старт следующей серии,
        // перемотка и смена качества иначе выглядели бы как зависший чёрный экран.
        if ((state.loading || ui.isBuffering) && error == null) {
            CircularProgressIndicator(color = TvAccent, modifier = Modifier.align(Alignment.Center))
        }

        // Раньше ошибка не показывалась вовсе: сбой загрузки серии оставлял чёрный экран
        // без единого индикатора (жалоба «следующая серия не запустилась»).
        error?.let { PlayerErrorCard(error = it, modifier = Modifier.align(Alignment.Center)) }

        AnimatedVisibility(
            visible = ui.visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            PlayerOverlay(
                ui = ui,
                menu = menu,
                title = state.item?.title.orEmpty(),
                subtitle = playerSubtitle(state),
            )
        }

        // Плашка подписки — ВНЕ оверлея: без подписки поток не идёт, и объяснение должно быть
        // видно всегда, а не только пока оверлей на экране.
        if (state.subscriptionRequired && error == null) {
            SubscriptionCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = SubscriptionCardTop),
            )
        }

        // Плашка автоперехода — тоже ВНЕ оверлея: в конце серии он обычно скрыт, а отсчёт
        // должен быть виден всегда.
        val next = state.nextTrack
        if (ui.autoNextVisible && next != null) {
            AutoNextCard(
                label = "Дальше: ${next.number}. ${next.title.ifBlank { "Серия ${next.number}" }}",
                seconds = ui.autoNextSeconds,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = TvMetrics.SafeHorizontal, bottom = AutoNextCardBottom),
            )
        }
    }
}

/** Видеоповерхность ExoPlayer; контролы отключены — весь UI поверх кадра рисует Compose. */
@Composable
private fun VideoSurface(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).also { view ->
                view.player = player
                view.useController = false
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}

/** Подстрока шапки: «Сезон 2 · Серия 5» у сериала, «год · качество» у фильма. */
private fun playerSubtitle(state: PlayerState): String {
    val track = state.track ?: return ""
    return when {
        track.seasonNumber > 0 -> "Сезон ${track.seasonNumber} · Серия ${track.number}"
        state.item?.tracklist.orEmpty().size > 1 -> "Серия ${track.number}"
        else -> listOfNotNull(state.item?.year?.takeIf { it > 0 }?.toString(), state.currentQuality)
            .joinToString(" · ")
    }
}

private fun SettingsAction.options(state: PlayerState): List<String> = when (this) {
    SettingsAction.Quality -> state.qualities.map { it.label }
    SettingsAction.Audio -> state.audioTracks.map { it.label }
    SettingsAction.Subtitle -> state.subtitles.map { it.label }
    SettingsAction.Speed -> PlaybackSpeeds.labels
    SettingsAction.Episodes, SettingsAction.NextEpisode -> emptyList()
}

private fun SettingsAction.selected(state: PlayerState): String = when (this) {
    SettingsAction.Quality -> state.currentQuality.orEmpty()
    SettingsAction.Audio -> state.currentAudio
    SettingsAction.Subtitle -> state.currentSubtitle
    SettingsAction.Speed -> PlaybackSpeeds.labelFor(state.currentSpeed)
    SettingsAction.Episodes, SettingsAction.NextEpisode -> ""
}

private fun SettingsAction.toEvent(label: String): PlayerEvent? = when (this) {
    SettingsAction.Quality -> PlayerEvent.SelectQuality(label)
    SettingsAction.Audio -> PlayerEvent.SelectAudio(label)
    SettingsAction.Subtitle -> PlayerEvent.SelectSubtitle(label)
    SettingsAction.Speed -> PlaybackSpeeds.valueFor(label)?.let { PlayerEvent.SetSpeed(it) }
    SettingsAction.Episodes, SettingsAction.NextEpisode -> null
}

private val AutoNextCardBottom = 120.dp

/** Отступ плашки подписки от верха кадра — ниже строки шапки, выше центра с поповерами. */
private val SubscriptionCardTop = 96.dp
