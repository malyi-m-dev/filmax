package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.TvRemote
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.Job

/**
 * Связь телефона с запомненным телевизором, как её видит телефонный граф: пока Filmax на экране
 * телефона, пульт сам возвращает связь — после обрыва, перезапуска ТВ или самого телефона. Свернули —
 * повторы останавливаются: в фоне пульт не нужен, а стучаться к выключенному ТВ — это батарея.
 * Своего состояния у модели нет — что со связью, показывает пульт.
 */
class RemoteKeeperScreenModel(
    private val remote: TvRemote,
) : BaseScreenModel<Unit, Nothing, RemoteKeeperEvent>(Unit) {

    private var keeping: Job? = null

    override fun onFetchData() = Unit

    override fun onEvent(event: RemoteKeeperEvent) {
        when (event) {
            RemoteKeeperEvent.Shown -> if (keeping?.isActive != true) {
                keeping = screenModelScope { _ -> remote.stayConnected() }
            }
            RemoteKeeperEvent.Hidden -> {
                keeping?.cancel()
                keeping = null
            }
        }
    }
}
