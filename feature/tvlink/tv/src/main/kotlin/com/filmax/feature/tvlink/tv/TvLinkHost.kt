package com.filmax.feature.tvlink.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.TvMessageCard
import com.filmax.feature.tvlink.common.TvLinkHostEvent
import com.filmax.feature.tvlink.common.TvLinkHostScreenModel
import com.filmax.feature.tvlink.common.TvLinkHostSideEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Приёмник телефонов поверх ТВ-графа. Пока Filmax на экране и [enabled] (вход выполнен), ТВ виден в
 * сети. Тайтл с телефона открывает плеер (это делает модель), подключившийся телефон объявляется плашкой.
 *
 * Свёрнутый Filmax телефоны не принимает: открыть плеер поверх лаунчера Android всё равно не даст,
 * и телефон «подключался» бы к приложению, которого не видно.
 */
@Composable
fun TvLinkHost(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    screenModel: TvLinkHostScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    screenModel.collectSideEffect { effect ->
        when (effect) {
            is TvLinkHostSideEffect.OpenPlayer -> navigator.open(playerOf(effect.play))
            is TvLinkHostSideEffect.ReplacePlayer -> navigator.replace(playerOf(effect.play))
        }
    }
    LifecycleStartEffect(enabled, screenModel) {
        if (enabled) screenModel.dispatch(TvLinkHostEvent.Shown)
        onStopOrDispose { screenModel.dispatch(TvLinkHostEvent.Hidden) }
    }

    val state by screenModel.collectAsState()
    AnimatedVisibility(
        visible = state.joinedPhone != null,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        TvMessageCard(title = "Пульт подключён", message = "«${state.lastJoinedPhone}» управляет этим телевизором")
    }
}

/** Плеер на тайтл с телефона — с той серии и секунды, что телефон передал. */
private fun playerOf(play: RemoteCommand.Play) =
    Destination.Player(play.itemId, play.videoId, play.season, play.startSeconds)
