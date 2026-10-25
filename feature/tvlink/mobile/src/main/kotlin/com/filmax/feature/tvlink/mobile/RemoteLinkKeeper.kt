package com.filmax.feature.tvlink.mobile

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleStartEffect
import com.filmax.feature.tvlink.common.RemoteKeeperEvent
import com.filmax.feature.tvlink.common.RemoteKeeperScreenModel
import org.koin.androidx.compose.koinViewModel

/**
 * Связь с запомненным телевизором поверх телефонного графа: пока Filmax на экране и [enabled] (вход
 * выполнен), пульт сам подключается к ТВ, с которым телефон уже знаком, — выбирать его снова не нужно.
 * Ничего не рисует: что со связью, показывает пульт.
 */
@Composable
fun RemoteLinkKeeper(
    enabled: Boolean,
    screenModel: RemoteKeeperScreenModel = koinViewModel(),
) {
    LifecycleStartEffect(enabled, screenModel) {
        if (enabled) screenModel.dispatch(RemoteKeeperEvent.Shown)
        onStopOrDispose { screenModel.dispatch(RemoteKeeperEvent.Hidden) }
    }
}
