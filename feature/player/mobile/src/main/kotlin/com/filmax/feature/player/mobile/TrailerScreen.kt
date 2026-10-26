package com.filmax.feature.player.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.HideSystemBars
import com.filmax.core.ui.components.KeepScreenOn
import com.filmax.feature.player.common.TrailerEvent
import com.filmax.feature.player.common.TrailerScreenModel
import com.filmax.feature.player.common.TrailerSideEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Экран трейлера — одноразовый плеер по готовому HLS-URL.
 *
 * Адрес — временный .m3u8 с истекающим токеном в query, поэтому плеер намеренно простой: он не
 * переживает пересоздание (по протухшему токену воспроизведение не восстановить — для трейлера
 * это допустимо). Играем URL как есть, со штатным контроллером Media3; модель лишь отдаёт адрес
 * и название из маршрута и закрывает экран.
 */
@Composable
fun TrailerScreen(
    modifier: Modifier = Modifier,
    screenModel: TrailerScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            TrailerSideEffect.Close -> navigator.back()
        }
    }
    val url = state.url
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

    // Трейлер короткий — держим экран, пока он на экране, без слежения за паузой.
    KeepScreenOn()
    // Трейлер — то же полноэкранное видео, что и плеер: системные панели прячем и здесь.
    HideSystemBars()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        TrailerTopBar(
            title = state.title,
            onBack = { screenModel.dispatch(TrailerEvent.Back) },
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun TrailerTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(OverlayTopInsets)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilmaxIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", onClick = onBack)
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = FilmaxType.playerTitle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
