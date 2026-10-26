// Экран плеера: поверхность ExoPlayer, оверлей контролов с автоскрытием и блокировкой экрана,
// боковая панель, плашка «нужна подписка» и эффекты экрана (экран не гаснет, пауза за кадром).
// Снимок воспроизведения, перемотка и переходы — у модели; контролы — в PlayerOverlay, панель —
// в PlayerSidePanel.

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
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.KeepScreenOn
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerScreenModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private const val AUTO_HIDE_DELAY_MS = 4500L

/**
 * Плеер. Другая серия — это НАВИГАЦИЯ на новый экран плеера, а не подмена потока: прогресс модель
 * пишет в трек, выбранный при старте, и переход делает она сама.
 */
@Composable
fun PlayerScreen(modifier: Modifier = Modifier, screenModel: PlayerScreenModel = koinViewModel()) {
    val state by screenModel.collectAsState()
    val onEvent: (PlayerEvent) -> Unit = screenModel::dispatch
    val appError by screenModel.collectErrorAsState()
    PlayerEffects(screenModel = screenModel, playing = state.playback.playing)

    // Видны ли контролы и заблокирован ли экран — дело вёрстки: модель о касаниях не знает.
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    val scrubbing = state.scrubTargetMs != null
    // Контролы прячутся сами, но не пока тянут ползунок и не при открытой панели.
    LaunchedEffect(controlsVisible, scrubbing, state.panelOpen) {
        if (controlsVisible && !scrubbing && !state.panelOpen) {
            delay(AUTO_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { controlsVisible = !controlsVisible },
            ),
    ) {
        PlayerSurface(screenModel.player)
        // Загрузка тайтла или буферизация потока; под модалкой ошибки не крутим.
        if ((state.loading || state.playback.buffering) && appError == null) {
            CircularProgressIndicator(color = FilmaxTheme.palette.accent, modifier = Modifier.align(Alignment.Center))
        }
        // Плашка подписки — вне оверлея: без неё kino.pub не отдаст поток, и объяснение должно
        // быть видно всегда, а не только пока контролы на экране.
        if (state.subscriptionRequired && appError == null) SubscriptionBanner(Modifier.align(Alignment.TopCenter))
        AnimatedVisibility(
            // При открытой панели контролы прячутся: панель сама управляет, а сквозь неё они мешают читать.
            visible = controlsVisible && !state.loading && !state.panelOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (locked) {
                LockedOverlay(onUnlock = { locked = false })
            } else {
                PlayerOverlay(state = state, onEvent = onEvent, onLock = { locked = true })
            }
        }
        PlayerSidePanel(state = state, onEvent = onEvent)
        appError?.let { error ->
            FilmaxErrorModal(
                error = error,
                onDismiss = screenModel::dismissError,
                // «Повторить» перезапускает загрузку и воспроизведение (retry сбрасывает ошибку и
                // зовёт onFetchData).
                onPrimary = screenModel::retry,
                onSecondary = { onEvent(PlayerEvent.Exit) },
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
            color = FilmaxColors.TextSecondary,
        )
    }
}

private val BannerFill = Color.Black.copy(alpha = 0.7f)
