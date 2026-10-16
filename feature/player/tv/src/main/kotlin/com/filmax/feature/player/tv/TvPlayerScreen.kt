package com.filmax.feature.player.tv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerScreenModel
import com.filmax.feature.player.common.PlayerSideEffect
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * TV-плеер: видеоповерхность ExoPlayer, контролы поверх кадра с автоскрытием и нижняя панель
 * «Серии · Аудио · Субтитры · Качество · Скорость».
 *
 * Другая серия — это замена экрана через [Navigator]: новый экран плеера с новым
 * [PlayerScreenModel], а не подмена MediaItem (иначе прогресс писался бы в предыдущую серию).
 */
@Composable
fun TvPlayerScreen(
    itemId: Int,
    modifier: Modifier = Modifier,
    screenModel: PlayerScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            // Плеер уступает место окну: «Назад» из него ведёт в детали тайтла, а не в титры.
            PlayerSideEffect.AskFeedback -> navigator.replace(Destination.Feedback(FeedbackTrigger.AfterWatch.name))
        }
    }
    val appError by screenModel.collectErrorAsState()
    val player = screenModel.player
    val transport = remember(player) { TvTransportState(player) }
    val actions = remember(itemId, navigator, screenModel) { playerActions(itemId, navigator, screenModel) }

    PlayerEffects(transport, screenModel, next = state.nextTrack, actions = actions)
    // Ушли с плеера не «Назад», а HOME/лаунчером — пауза: ScreenModel (и ExoPlayer в нём) жив,
    // и без этого звук продолжал играть за пределами приложения.
    LifecycleStartEffect(player) {
        onStopOrDispose { player.pause() }
    }

    PlayerContent(PlayerScene(state, transport, appError), actions, modifier) { VideoSurface(player) }
}

/** Действия плеера: события модели, переход на другую серию и выход. */
internal data class PlayerActions(
    val dispatch: (PlayerEvent) -> Unit,
    /** Серия того же тайтла — заменой экрана; сезон обязателен: номер серии уникален только в нём. */
    val onPlayEpisode: (season: Int, videoId: Int) -> Unit,
    val onExit: () -> Unit,
) {
    fun playNext(next: MediaTrack?) {
        next?.let { onPlayEpisode(it.seasonNumber, it.number) }
    }
}

private fun playerActions(itemId: Int, navigator: Navigator, screenModel: PlayerScreenModel) = PlayerActions(
    dispatch = screenModel::dispatch,
    onPlayEpisode = { season, videoId ->
        navigator.replace(Destination.Player(itemId = itemId, videoId = videoId, season = season))
    },
    onExit = navigator::back,
)

/** Тики и таймеры плеера: зеркала состояния, прогресс и SaveProgress, «Далее», перемотка, автоскрытие. */
@Composable
private fun PlayerEffects(
    transport: TvTransportState,
    screenModel: PlayerScreenModel,
    next: MediaTrack?,
    actions: PlayerActions,
) {
    // Эффекты живут с ключом player, а следующая серия доезжает вместе с плейлистом позже —
    // читаем всегда актуальную, иначе эффекты замкнули бы пустое значение первого кадра.
    val currentNext by rememberUpdatedState(next)
    val playNext = {
        transport.autoNext = transport.autoNext.copy(visible = false)
        actions.playNext(currentNext)
    }
    PlayerListener(screenModel.player, transport) {
        if (currentNext != null && !transport.autoNext.dismissed) playNext()
    }
    LaunchedEffect(screenModel.player) {
        while (true) {
            delay(PROGRESS_TICK_MS)
            val ready = transport.syncWith(screenModel.player, nextEnabled = currentNext != null)
            if (!ready) continue
            screenModel.dispatch(PlayerEvent.SaveProgress(screenModel.player.currentPosition))
            if (transport.autoNext.expired) playNext()
        }
    }
    LaunchedEffect(transport.isScrubbing, transport.scrubTargetMs) {
        if (transport.isScrubbing) {
            delay(SCRUB_COMMIT_TIMEOUT_MS)
            transport.commitScrub()
        }
    }
    LaunchedEffect(transport.interactionTick, transport.idleHides) {
        if (transport.idleHides) {
            delay(OVERLAY_AUTO_HIDE_MS)
            transport.visible = false
        }
    }
}

/** Зеркала плеера в состоянии транспорта; серия дотекла до конца раньше тика — [onEnded]. */
@Composable
private fun PlayerListener(player: Player, transport: TvTransportState, onEnded: () -> Unit) {
    val currentOnEnded by rememberUpdatedState(onEnded)
    DisposableEffect(player) {
        transport.isPlaying = player.isPlaying
        transport.isBuffering = player.playbackState == Player.STATE_BUFFERING
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                transport.isPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                transport.isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) currentOnEnded()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
}

/**
 * Тик транспорта: длительность, позиция, загруженное и отсчёт «Далее». false — длительность ещё
 * неизвестна: позицию не сохраняем, иначе ноль лёг бы поверх настоящего прогресса.
 */
private fun TvTransportState.syncWith(player: Player, nextEnabled: Boolean): Boolean {
    val duration = player.duration.takeIf { it > 0 } ?: return false
    durationMs = duration
    bufferedMs = player.bufferedPosition
    // Во время перемотки позицию ведёт пульт.
    if (!isScrubbing) positionMs = player.currentPosition
    autoNext = autoNext.tick(
        remainingMs = duration - player.currentPosition,
        enabled = nextEnabled,
        playing = isPlaying,
        scrubbing = isScrubbing,
    )
    return true
}

/** Видеоповерхность ExoPlayer; контролы отключены — весь UI поверх кадра рисует Compose. */
@Composable
private fun VideoSurface(player: Player) {
    AndroidView(
        factory = { context ->
            PlayerView(context).also { view ->
                view.player = player
                view.useController = false
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
