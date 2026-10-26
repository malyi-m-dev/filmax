package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.ReceiverState
import com.filmax.core.domain.tvlink.TvLinkReceiver
import com.filmax.core.presentation.BaseScreenModel

/** Карточка «Пульт с телефона» словами: крупная строка и пояснение под ней. */
data class TvRemoteCardState(
    val value: String = remoteCardValue(ReceiverState()),
    val caption: String = remoteCardCaption(ReceiverState()),
)

/**
 * Карточка «Пульт с телефона» в Профиле ТВ: адрес приёмника (его вводят на телефоне, когда поиск в
 * сети не находит ТВ) и подключённые телефоны. Только показывает — событий у карточки нет.
 */
class TvRemoteInfoScreenModel(
    private val receiver: TvLinkReceiver,
) : BaseScreenModel<TvRemoteCardState, Nothing, Nothing>(TvRemoteCardState()) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            receiver.state.collect { received ->
                val card = TvRemoteCardState(remoteCardValue(received), remoteCardCaption(received))
                updateState { card }
            }
        }
    }

    override fun onEvent(event: Nothing) = Unit
}
