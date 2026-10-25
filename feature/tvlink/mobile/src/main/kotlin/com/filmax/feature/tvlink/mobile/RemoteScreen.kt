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
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonStyle
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxOverline
import com.filmax.feature.tvlink.common.RemoteEvent
import com.filmax.feature.tvlink.common.RemoteScreenModel
import com.filmax.feature.tvlink.common.RemoteSideEffect
import com.filmax.feature.tvlink.common.message
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Пульт: что играет на подключённом телевизоре и управление им; без связи — как её вернуть. */
@Composable
fun RemoteScreen(
    modifier: Modifier = Modifier,
    screenModel: RemoteScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            RemoteSideEffect.OpenPicker -> navigator.open(Destination.TvPicker())
            RemoteSideEffect.Close -> navigator.back()
        }
    }
    val dispatch = screenModel::dispatch
    Column(
        modifier
            .fillMaxSize()
            .background(FilmaxTheme.palette.background)
            .radialWash(FilmaxTheme.palette.wash, centerX = 0.5f, centerY = 0f, radiusX = 0.9f, radiusY = 0.35f)
            .systemBarsPadding()
            .padding(horizontal = FilmaxMetrics.ScreenPadding, vertical = 12.dp),
    ) {
        RemoteTopBar(
            session = state.session,
            onBack = navigator::back,
            onForget = { dispatch(RemoteEvent.Forget) },
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val session = state.session) {
                is RemoteSession.Connected ->
                    session.playback
                        ?.let { RemotePlayback(it, state.positionMs, dispatch, Modifier.fillMaxHeight()) }
                        ?: NothingPlaying()
                is RemoteSession.Connecting -> Connecting(session.device.name)
                is RemoteSession.Lost -> LinkLost(session, reconnectsItself = state.reconnectsItself, dispatch)
                RemoteSession.Idle -> NotConnected(dispatch)
            }
        }
    }
}

/**
 * Шапка: назад, имя телевизора и — пока телевизор выбран — «Забыть телевизор». Иконкой здесь, а не
 * кнопкой внизу: на маленьком телефоне низ занят управлением, и оно важнее. Видна и без связи:
 * запомненный ТВ, который больше не нужен, иначе подключался бы сам.
 */
@Composable
private fun RemoteTopBar(session: RemoteSession, onBack: () -> Unit, onForget: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        FilmaxIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", onClick = onBack)
        Column(Modifier.weight(1f)) {
            FilmaxOverline("Пульт")
            Text(
                session.deviceName() ?: "Телевизор не выбран",
                style = FilmaxType.playerTitle,
                color = FilmaxColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (session !is RemoteSession.Idle) {
            FilmaxIconButton(Icons.Filled.LinkOff, contentDescription = "Забыть телевизор", onClick = onForget)
        }
    }
}

private fun RemoteSession.deviceName(): String? = when (this) {
    is RemoteSession.Connected -> device.name
    is RemoteSession.Connecting -> device.name
    is RemoteSession.Lost -> device.name
    RemoteSession.Idle -> null
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
            textAlign = TextAlign.Center
        )
    }
}

/** [reconnectsItself] — ТВ запомнен: телефон подключится сам, как только тот ответит. */
@Composable
private fun LinkLost(session: RemoteSession.Lost, reconnectsItself: Boolean, dispatch: (RemoteEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilmaxEmptyState(
            icon = Icons.Filled.TvOff,
            title = "Нет связи с телевизором",
            subtitle = session.failure.message(session.device.name),
            modifier = Modifier.fillMaxWidth(),
        )
        if (reconnectsItself) {
            Text(
                "Подключимся сами, как только Filmax на телевизоре снова будет на экране",
                style = FilmaxType.meta,
                color = FilmaxColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        FilmaxButton(text = "Подключиться снова", onClick = {
            dispatch(RemoteEvent.Reconnect)
        }, modifier = Modifier.fillMaxWidth())
        FilmaxButton(
            text = "Выбрать другой телевизор",
            onClick = { dispatch(RemoteEvent.ChooseTv) },
            style = FilmaxButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NotConnected(dispatch: (RemoteEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilmaxEmptyState(
            icon = Icons.Filled.Tv,
            title = "Телевизор не подключён",
            subtitle = "Откройте Filmax на телевизоре в той же Wi-Fi сети — и управляйте им отсюда",
            modifier = Modifier.fillMaxWidth(),
        )
        FilmaxButton(text = "Выбрать телевизор", onClick = {
            dispatch(RemoteEvent.ChooseTv)
        }, modifier = Modifier.fillMaxWidth())
    }
}
