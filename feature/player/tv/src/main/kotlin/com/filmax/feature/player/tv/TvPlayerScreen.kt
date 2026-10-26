package com.filmax.feature.player.tv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerScreenModel
import org.koin.androidx.compose.koinViewModel

/**
 * TV-плеер: видеоповерхность ExoPlayer, контролы поверх кадра с автоскрытием и нижняя панель
 * «Серии · Аудио · Субтитры · Качество · Скорость».
 *
 * Другая серия — это замена экрана плеера новым [PlayerScreenModel], а не подмена MediaItem
 * (иначе прогресс писался бы в предыдущую серию); переход, как и выход, делает модель.
 */
@Composable
fun TvPlayerScreen(modifier: Modifier = Modifier, screenModel: PlayerScreenModel = koinViewModel()) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()

    // Ушли с плеера не «Назад», а HOME/лаунчером — пауза: ScreenModel (и ExoPlayer в нём) жив,
    // и без этого звук продолжал играть за пределами приложения. Модель знает об этом и не
    // запустит видео, если загрузка закончится, пока экран скрыт.
    LifecycleStartEffect(screenModel) {
        screenModel.dispatch(PlayerEvent.ScreenShown)
        onStopOrDispose { screenModel.dispatch(PlayerEvent.ScreenHidden) }
    }

    PlayerContent(state, appError, screenModel::dispatch, modifier) { VideoSurface(screenModel.player) }
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
