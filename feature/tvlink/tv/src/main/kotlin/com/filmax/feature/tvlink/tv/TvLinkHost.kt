package com.filmax.feature.tvlink.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
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
 * сети. Тайтл с телефона открывает плеер, подключившийся телефон объявляется плашкой.
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
            is TvLinkHostSideEffect.OpenPlayer -> {
                val destination = with(effect.play) { Destination.Player(itemId, videoId, season, startSeconds) }
                if (effect.replacePlayer) navigator.replace(destination) else navigator.open(destination)
            }
        }
    }
    LifecycleStartEffect(enabled, screenModel) {
        if (enabled) screenModel.dispatch(TvLinkHostEvent.Shown)
        onStopOrDispose { screenModel.dispatch(TvLinkHostEvent.Hidden) }
    }

    val state by screenModel.collectAsState()
    // Имя держим и после того, как плашка начала гаснуть: иначе она исчезала бы пустой.
    val lastPhone = remember { mutableStateOf("") }
    val phone = state.joinedPhone ?: lastPhone.value
    SideEffect { state.joinedPhone?.let { lastPhone.value = it } }
    AnimatedVisibility(
        visible = state.joinedPhone != null,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        TvMessageCard(title = "Пульт подключён", message = "«$phone» управляет этим телевизором")
    }
}
