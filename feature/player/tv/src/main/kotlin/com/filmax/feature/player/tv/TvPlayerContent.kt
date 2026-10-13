// Кадр плеера и слои над ним: контролы транспорта, нижняя панель, карточка «Далее» и состояния
// (загрузка, ошибка, нет подписки). Разбор клавиш пульта в транспорте — здесь же.
package com.filmax.feature.player.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.error.AppError
import com.filmax.core.tv.designsystem.FxEasing
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.LocalReducedMotion
import com.filmax.core.tv.designsystem.TvNextUpCard
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.KeepScreenOn
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerState

private val SubscriptionCardTop = 96.dp
private val NextUpTop = 220.dp

/** «Далее» въезжает слева на 28dp за 650 мс — медленнее панелей: это предложение, а не ответ на клавишу. */
private val NextUpSlide = 28.dp
private const val NEXT_UP_ENTER_MS = 650

private fun nextUpEnter(fromX: Int): EnterTransition {
    val spec = tween<IntOffset>(NEXT_UP_ENTER_MS, easing = FxEasing.Standard)
    return fadeIn(tween(NEXT_UP_ENTER_MS, easing = FxEasing.Standard)) + slideInHorizontally(spec) { fromX }
}
private val NextUpEnd = 40.dp

/** Ключ кнопки паузы в памяти фокуса экрана: на неё фокус встаёт при входе. */
private const val PAUSE_KEY = "pause"

/** Что показывает плеер: состояние модели, транспорт поверх кадра и ошибка воспроизведения. */
@Immutable
internal data class PlayerScene(val state: PlayerState, val transport: TvTransportState, val error: AppError?)

/**
 * Кадр и слои над ним. internal — UI-тест пульта собирает плеер из состояния, без Koin и
 * ScreenModel, а вместо видео передаёт пустой [videoSurface].
 *
 * Фокус в транспорте стоит на кнопке паузы и никуда не ходит: стрелки — это перемотка и панель,
 * их перехватывает корень раньше, чем фокус поищет соседа. Пока открыта панель, корень клавиши не
 * трогает — по панели ходит обычный фокус.
 */
@Composable
internal fun PlayerContent(
    scene: PlayerScene,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    videoSurface: @Composable () -> Unit,
) {
    val (state, transport, error) = scene
    val focus = rememberTvScreenFocus(startAt = PAUSE_KEY)
    val pauseFocus = remember { FocusRequester() }
    ReturnFocusOnPanelClose(state.panelOpen, pauseFocus, onClosed = transport::touch)
    PlayerBackHandler(state, transport, actions)
    // Пока идёт воспроизведение, экран не гаснет; на паузе — обычный таймаут системы.
    KeepScreenOn(enabled = transport.isPlaying)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                !state.panelOpen && onTransportKey(event, state.nextTrack, transport, actions)
            }
            .then(focus.containerModifier),
    ) {
        videoSurface()
        PlayerStatus(state, transport, error)
        TransportOverlay(
            state = state,
            transport = transport,
            visible = transport.visible && !state.panelOpen,
            pauseModifier = focus.item(PAUSE_KEY).focusRequester(pauseFocus),
        )
        PanelOverlay(state, transport, actions)
        NextUpOverlay(state.nextTrack, transport)
    }
}

/**
 * «Назад» закрывает ровно один слой: панель → карточку «Далее» → выходит из плеера. Скрытые
 * контролы слоем не считаются — прятать за ними выход значило бы лишнее нажатие.
 */
@Composable
private fun PlayerBackHandler(state: PlayerState, transport: TvTransportState, actions: PlayerActions) {
    BackHandler {
        when {
            state.panelOpen -> actions.dispatch(PlayerEvent.ClosePanel)
            transport.dismissAutoNext() -> Unit
            else -> actions.onExit()
        }
    }
}

/**
 * Клавиша в транспорте. Решение — [transportCommand]; здесь только исполнение. Потребляем и
 * нажатие, и отпускание: иначе отпускание OK дошло бы до кнопки паузы вторым кликом. Удержание
 * повторяет только перемотку: зажатый OK не должен дёргать паузу десять раз.
 */
private fun onTransportKey(
    event: KeyEvent,
    next: MediaTrack?,
    transport: TvTransportState,
    actions: PlayerActions
): Boolean {
    val command = transportCommand(event.key, transport.autoNext.visible) ?: return false
    val native = event.nativeKeyEvent
    val fresh = native.repeatCount == 0 || command.repeats
    if (event.type == KeyEventType.KeyDown && fresh) {
        when (command) {
            TransportCommand.SeekBack -> transport.seek(-1, native.repeatCount, native.eventTime)
            TransportCommand.SeekForward -> transport.seek(1, native.repeatCount, native.eventTime)
            TransportCommand.TogglePlay -> transport.togglePlay()
            TransportCommand.Peek -> transport.touch()
            TransportCommand.OpenPanel -> {
                transport.commitScrub()
                actions.dispatch(PlayerEvent.OpenPanel)
            }
            TransportCommand.PlayNext -> {
                transport.autoNext = transport.autoNext.copy(visible = false)
                actions.playNext(next)
            }
        }
    }
    return true
}

/**
 * Панель закрылась — фокус обратно на паузу (без него пульт перестал бы отвечать), а контролы —
 * на экран ([onClosed]): за панелью они могли успеть уйти, и зритель вернулся бы к голому кадру.
 */
@Composable
private fun ReturnFocusOnPanelClose(panelOpen: Boolean, pauseFocus: FocusRequester, onClosed: () -> Unit) {
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(panelOpen) {
        if (panelOpen) {
            wasOpen = true
        } else if (wasOpen) {
            wasOpen = false
            pauseFocus.requestFocus()
            onClosed()
        }
    }
}

/**
 * Загрузка, ошибка и подписка. Спиннер — и на буферизации: старт серии, перемотка и смена качества
 * иначе выглядели бы зависшим чёрным кадром. Плашка подписки — вне контролов: без подписки поток
 * не идёт, и объяснение видно всегда.
 */
@Composable
private fun BoxScope.PlayerStatus(state: PlayerState, transport: TvTransportState, error: AppError?) {
    if ((state.loading || transport.isBuffering) && error == null) {
        CircularProgressIndicator(color = LocalFxColors.current.accent, modifier = Modifier.align(Alignment.Center))
    }
    error?.let { PlayerErrorCard(error = it, modifier = Modifier.align(Alignment.Center)) }
    if (state.subscriptionRequired && error == null) {
        SubscriptionCard(Modifier.align(Alignment.TopCenter).padding(top = SubscriptionCardTop))
    }
}

/**
 * Карточка «Далее» — вне контролов: в конце серии они обычно скрыты, а отсчёт должен быть виден.
 * Полоса отсчёта плавно едет между тиками секунды, а не прыгает.
 */
@Composable
private fun BoxScope.NextUpOverlay(next: MediaTrack?, transport: TvTransportState) {
    val autoNext = transport.autoNext
    val reduced = LocalReducedMotion.current
    val slideFrom = with(LocalDensity.current) { -NextUpSlide.roundToPx() }
    AnimatedVisibility(
        visible = autoNext.visible && next != null,
        // Карточка въезжает слева на 28dp, как в макете; при сниженной анимации — только проявляется.
        enter = if (reduced) fadeIn(FxMotion.fade()) else nextUpEnter(slideFrom),
        exit = fadeOut(FxMotion.panelOut()),
        modifier = Modifier.align(Alignment.TopEnd).padding(top = NextUpTop, end = NextUpEnd),
    ) {
        val elapsed by animateFloatAsState(
            autoNext.elapsed,
            tween(PROGRESS_TICK_MS.toInt(), easing = LinearEasing),
            label = "nextUpCountdown",
        )
        next?.let { track ->
            TvNextUpCard(
                overline = "Далее · ${autoNext.secondsLeft} с",
                title = nextUpTitle(track),
                imageUrl = track.thumbnail,
                countdown = { elapsed },
            )
        }
    }
}
