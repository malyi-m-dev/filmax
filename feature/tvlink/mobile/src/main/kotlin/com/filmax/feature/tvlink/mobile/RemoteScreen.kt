// Пульт телевизора на телефоне: каркас экрана и состояния связи. Что играет и кнопки управления —
// в RemotePlayback.

package com.filmax.feature.tvlink.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConnectedTv
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.radialWash
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonStyle
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxOverline
import com.filmax.feature.tvlink.common.RemoteEvent
import com.filmax.feature.tvlink.common.RemoteScreenModel
import com.filmax.feature.tvlink.common.RemoteState
import org.koin.androidx.compose.koinViewModel

/** Пульт: что играет на подключённом телевизоре и управление им; без связи — как её вернуть. */
@Composable
fun RemoteScreen(modifier: Modifier = Modifier, screenModel: RemoteScreenModel = koinViewModel()) {
    val state by screenModel.collectAsState()
    val onEvent: (RemoteEvent) -> Unit = screenModel::dispatch
    Column(
        modifier
            .fillMaxSize()
            .background(FilmaxTheme.palette.background)
            .radialWash(FilmaxTheme.palette.wash, centerX = 0.5f, centerY = 0f, radiusX = 0.9f, radiusY = 0.35f)
            .systemBarsPadding()
            .padding(horizontal = FilmaxMetrics.ScreenPadding, vertical = 12.dp),
    ) {
        RemoteTopBar(state = state, onEvent = onEvent)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            RemoteBody(state = state, onEvent = onEvent)
        }
    }
}

/** Связь есть и что-то играет — управление; иначе — почему пульт молчит и как это поправить. */
@Composable
private fun RemoteBody(state: RemoteState, onEvent: (RemoteEvent) -> Unit) {
    val playback = state.playback
    when {
        playback != null -> RemotePlayback(
            playback = playback,
            caption = state.playbackCaption,
            positionMs = state.positionMs,
            onEvent = onEvent,
            modifier = Modifier.fillMaxHeight(),
        )
        state.session is RemoteSession.Connected -> NothingPlaying()
        state.session is RemoteSession.Connecting -> Connecting(state.deviceName.orEmpty())
        state.session is RemoteSession.Lost -> LinkLost(state, onEvent)
        else -> NotConnected(onEvent)
    }
}

/**
 * Шапка: назад, имя телевизора и — пока телевизор выбран — «Забыть телевизор». Иконкой здесь, а не
 * кнопкой внизу: на маленьком телефоне низ занят управлением, и оно важнее. Видна и без связи:
 * запомненный ТВ, который больше не нужен, иначе подключался бы сам.
 */
@Composable
private fun RemoteTopBar(state: RemoteState, onEvent: (RemoteEvent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        FilmaxIconButton(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            onClick = { onEvent(RemoteEvent.Back) },
        )
        Column(Modifier.weight(1f)) {
            FilmaxOverline("Пульт")
            Text(
                state.deviceName ?: "Телевизор не выбран",
                style = FilmaxType.playerTitle,
                color = FilmaxColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.deviceName != null) {
            FilmaxIconButton(
                Icons.Filled.LinkOff,
                contentDescription = "Забыть телевизор",
                onClick = { onEvent(RemoteEvent.Forget) },
            )
        }
    }
}

@Composable
private fun NothingPlaying() {
    FilmaxEmptyState(
        icon = Icons.Filled.ConnectedTv,
        title = "На телевизоре ничего не играет",
        subtitle = "Откройте фильм на телефоне и нажмите «На ТВ» — он начнётся на телевизоре",
    )
}

@Composable
private fun Connecting(deviceName: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = FilmaxTheme.palette.accent)
        Text(
            "Подключаемся к «$deviceName»…",
            style = FilmaxType.body,
            color = FilmaxColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Связь пропала: почему — из состояния; запомненный ТВ подключится сам, как только ответит. */
@Composable
private fun LinkLost(state: RemoteState, onEvent: (RemoteEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilmaxEmptyState(
            icon = Icons.Filled.TvOff,
            title = "Нет связи с телевизором",
            subtitle = state.failureMessage,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.reconnectsItself) {
            Text(
                "Подключимся сами, как только Filmax на телевизоре снова будет на экране",
                style = FilmaxType.meta,
                color = FilmaxColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        FilmaxButton(
            text = "Подключиться снова",
            onClick = { onEvent(RemoteEvent.Reconnect) },
            modifier = Modifier.fillMaxWidth(),
        )
        FilmaxButton(
            text = "Выбрать другой телевизор",
            onClick = { onEvent(RemoteEvent.ChooseTv) },
            style = FilmaxButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NotConnected(onEvent: (RemoteEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilmaxEmptyState(
            icon = Icons.Filled.Tv,
            title = "Телевизор не подключён",
            subtitle = "Откройте Filmax на телевизоре в той же Wi-Fi сети — и управляйте им отсюда",
            modifier = Modifier.fillMaxWidth(),
        )
        FilmaxButton(
            text = "Выбрать телевизор",
            onClick = { onEvent(RemoteEvent.ChooseTv) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
