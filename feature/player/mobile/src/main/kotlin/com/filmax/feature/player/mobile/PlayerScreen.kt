// Экран плеера: поверхность ExoPlayer, оверлей контролов с автоскрытием и блокировкой экрана,
// боковая панель, плашка «нужна подписка» и эффекты воспроизведения (прогресс, экран не гаснет,
// пауза за кадром). Контролы — в PlayerOverlay, панель — в PlayerSidePanel.

package com.filmax.feature.player.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeTile
import com.filmax.core.designsystem.glassFill
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.KeepScreenOn
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerScreenModel
import com.filmax.feature.player.common.PlayerSideEffect
import com.filmax.feature.player.common.PlayerState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val AUTO_HIDE_DELAY_MS = 4500L

/**
 * Плеер. [itemId] — из маршрута: экрану он нужен, чтобы попросить другую серию — это НАВИГАЦИЯ на
 * новый экран плеера, а не подмена потока: прогресс модель пишет в трек, выбранный при старте.
 */
@Composable
fun PlayerScreen(
    itemId: Int,
    modifier: Modifier = Modifier,
    screenModel: PlayerScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect -> navigator.onPlayerEffect(itemId, effect) }
    val appError by screenModel.collectErrorAsState()
    // Другая серия — замена текущего экрана: перещёлкивание серий не копит стек.
    val playEpisode: (Int, Int) -> Unit = { season, videoId ->
        navigator.replace(Destination.Player(itemId = itemId, videoId = videoId, season = season))
    }
    val playNext = state.nextTrack?.let { next -> { playEpisode(next.seasonNumber, next.number) } }
    val playback = rememberPlayback(
        player = screenModel.player,
        onEnded = { playNext?.invoke() },
        onTick = { screenModel.dispatch(PlayerEvent.SaveProgress(it)) },
    )
    PlayerEffects(screenModel = screenModel, playing = playback.playing)
    val controls = rememberControls(scrubbing = playback.scrubMs != null, panelOpen = state.panelOpen)
    val actions = overlayActions(
        screenModel,
        playback,
        onBack = navigator::back,
        onLock = controls::lock,
        onNext = playNext
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = controls::toggle
            ),
    ) {
        PlayerSurface(screenModel.player)
        // Загрузка тайтла или буферизация потока; под модалкой ошибки не крутим.
        if ((state.loading || playback.buffering) && appError == null) {
            CircularProgressIndicator(color = FilmaxTheme.palette.accent, modifier = Modifier.align(Alignment.Center))
        }
        // Плашка подписки — вне оверлея: без неё kino.pub не отдаст поток, и объяснение должно
        // быть видно всегда, а не только пока контролы на экране.
        if (state.subscriptionRequired && appError == null) SubscriptionBanner(Modifier.align(Alignment.TopCenter))
        Controls(state = state, playback = playback, controls = controls, actions = actions)
        PlayerSidePanel(
            state = state,
            actions = PanelActions(onEvent = screenModel::dispatch, onPlayEpisode = playEpisode)
        )
        appError?.let { error ->
            FilmaxErrorModal(
                error = error,
                onDismiss = screenModel::dismissError,
                // «Повторить» перезапускает загрузку и воспроизведение (retry сбрасывает ошибку и
                // зовёт onFetchData).
                onPrimary = screenModel::retry,
                onSecondary = navigator::back,
            )
        }
    }
}

/**
 * Эффекты экрана: свернули приложение — пауза (иначе звук играл бы за пределами экрана, пока жив
 * ScreenModel); пока идёт воспроизведение — экран не гаснет; плеер — альбомный и без системных панелей.
 */
@Composable
private fun PlayerEffects(screenModel: PlayerScreenModel, playing: Boolean) {
    LifecycleStartEffect(screenModel) {
        screenModel.dispatch(PlayerEvent.ScreenShown)
        onStopOrDispose { screenModel.dispatch(PlayerEvent.ScreenHidden) }
    }
    KeepScreenOn(enabled = playing)
    PlayerFullscreenEffects()
}

/** Видны ли контролы и заблокирован ли экран; тап по видео переключает видимость. */
@Stable
internal class ControlsUi {
    var visible by mutableStateOf(true)
        private set
    var locked by mutableStateOf(false)
        private set

    fun toggle() {
        visible = !visible
    }

    fun hide() {
        visible = false
    }

    fun lock() {
        locked = true
    }

    fun unlock() {
        locked = false
    }
}

/** Контролы прячутся сами, но не пока тянут ползунок и не при открытой панели. */
@Composable
private fun rememberControls(scrubbing: Boolean, panelOpen: Boolean): ControlsUi {
    val controls = remember { ControlsUi() }
    LaunchedEffect(controls.visible, scrubbing, panelOpen) {
        if (controls.visible && !scrubbing && !panelOpen) {
            delay(AUTO_HIDE_DELAY_MS)
            controls.hide()
        }
    }
    return controls
}

/** Что умеют контролы: транспорт — сам плеер, панель и «на ТВ» — события модели. */
private fun overlayActions(
    screenModel: PlayerScreenModel,
    playback: PlaybackUi,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onNext: (() -> Unit)?,
): OverlayActions {
    val player = screenModel.player
    return OverlayActions(
        onBack = onBack,
        onTogglePlay = { if (player.isPlaying) player.pause() else player.play() },
        onSeekBy = { delta -> player.seekTo((player.currentPosition + delta).coerceAtLeast(0)) },
        onScrub = { playback.scrubMs = it },
        onScrubFinished = {
            playback.scrubMs?.let { target ->
                player.seekTo(target)
                playback.positionMs = target
            }
            playback.scrubMs = null
        },
        onOpenTab = { screenModel.dispatch(PlayerEvent.OpenPanelAt(it)) },
        onPlayOnTv = { screenModel.dispatch(PlayerEvent.ContinueOnTv) },
        onLock = onLock,
        onNext = onNext,
    )
}

@Composable
private fun PlayerSurface(player: Player) {
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).also { view ->
                view.player = player
                view.useController = false
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/** Оверлей или «замок» — по состоянию экрана: что видно, что блокировано и что умеют контролы. */
@Composable
private fun Controls(state: PlayerState, playback: PlaybackUi, controls: ControlsUi, actions: OverlayActions) {
    AnimatedVisibility(
        // При открытой панели контролы прячутся: панель сама управляет, а сквозь неё они мешают читать.
        visible = controls.visible && !state.loading && !state.panelOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (controls.locked) {
            LockedOverlay(onUnlock = controls::unlock)
        } else {
            PlayerOverlay(state = state, playback = playback, actions = actions)
        }
    }
}

/** Плашка «нужна подписка»: без неё kino.pub не отдаст поток, и экран остался бы просто чёрным. */
@Composable
private fun SubscriptionBanner(modifier: Modifier = Modifier) {
    Column(
        modifier
            .windowInsetsPadding(OverlayTopInsets)
            .padding(top = 76.dp)
            .glassFill(ShapeTile, fill = BannerFill)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Нужна подписка", style = FilmaxType.itemTitle, color = FilmaxColors.Text)
        Text(
            "Просмотр доступен только с активной подпиской kino.pub",
            style = FilmaxType.meta,
            color = FilmaxColors.TextSecondary
        )
    }
}

/** Эффекты модели — это навигация: окно оценки, серия и выход по пульту, выбор телевизора. */
private fun Navigator.onPlayerEffect(itemId: Int, effect: PlayerSideEffect) {
    when (effect) {
        // Плеер уступает место окну: «Назад» из него ведёт в детали тайтла, а не в титры.
        PlayerSideEffect.AskFeedback -> replace(Destination.Feedback(FeedbackTrigger.AfterWatch.name))
        is PlayerSideEffect.PlayEpisode ->
            replace(Destination.Player(itemId = itemId, videoId = effect.videoId, season = effect.season))
        PlayerSideEffect.Exit -> back()
        is PlayerSideEffect.ChooseTv -> effect.play.let { play ->
            open(Destination.TvPicker(play.itemId, play.videoId, play.season, play.startSeconds))
        }
    }
}

private val BannerFill = Color.Black.copy(alpha = 0.7f)
