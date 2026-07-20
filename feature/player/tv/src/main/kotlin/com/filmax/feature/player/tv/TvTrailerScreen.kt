package com.filmax.feature.player.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.ui.components.KeepScreenOn

/**
 * TV-экран трейлера — одноразовый плеер по готовому HLS-URL.
 *
 * [url] — временный .m3u8 с истекающим токеном в query, поэтому плеер намеренно простой: он не
 * переживает пересоздание (по протухшему токену воспроизведение не восстановить — для трейлера
 * это допустимо). Отдельного ScreenModel не заводим: играем URL как есть, со штатным контроллером
 * Media3 (на TV он управляется D-pad пульта).
 */
@Composable
fun TvTrailerScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // remember(url) пересоздаёт плеер только при смене трейлера; ключ URL связывает жизненный
    // цикл ExoPlayer с конкретным HLS-адресом.
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // На TV «Назад» — системная кнопка пульта: перехватываем её, чтобы выйти из трейлера.
    BackHandler { onBack() }

    // Трейлер короткий — держим экран, пока он на экране, без слежения за паузой.
    KeepScreenOn()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        TvTrailerSurface(player = exoPlayer, modifier = Modifier.fillMaxSize())
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = TvMetrics.SafeHorizontal, vertical = 28.dp),
            )
        }
    }
}

/**
 * Видеоповерхность со штатным контроллером Media3. На TV контроллер D-pad-управляемый, поэтому
 * PlayerView обязан держать фокус — иначе пульт до кнопок «пауза/перемотка» не достучится.
 */
@Composable
private fun TvTrailerSurface(player: ExoPlayer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Один PlayerView на жизнь плеера: пересоздаём его вместе с ExoPlayer (ключ [player]).
    val playerView = remember(player) {
        PlayerView(context).apply {
            this.player = player
            useController = true
        }
    }
    // Фокус запрашиваем после присоединения к окну (post): до этого PlayerView его не примет.
    LaunchedEffect(playerView) { playerView.post { playerView.requestFocus() } }
    AndroidView(factory = { playerView }, modifier = modifier)
}
