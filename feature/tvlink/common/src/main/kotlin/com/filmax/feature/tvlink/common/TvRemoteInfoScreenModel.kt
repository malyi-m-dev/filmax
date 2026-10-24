package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.ReceiverState
import com.filmax.core.domain.tvlink.TvLinkReceiver
import com.filmax.core.presentation.BaseScreenModel

/**
 * Карточка «Пульт с телефона» в Профиле ТВ: адрес приёмника (его вводят на телефоне, когда поиск в
 * сети не находит ТВ) и подключённые телефоны. Только показывает — событий у карточки нет.
 */
class TvRemoteInfoScreenModel(
    private val receiver: TvLinkReceiver,
) : BaseScreenModel<ReceiverState, Nothing, Nothing>(ReceiverState()) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        screenModelScope { _ -> receiver.state.collect { received -> updateState { received } } }
    }

    override fun onEvent(event: Nothing) = Unit
}

/** Крупная строка карточки: кто управляет, иначе — адрес, по которому подключиться. */
fun ReceiverState.cardValue(): String = when {
    phones.isNotEmpty() -> phones.joinToString(", ")
    else -> address ?: "Нет сети"
}

/**
 * Пояснение под ней — что делать дальше. Коротко: карточка стоит под подпиской, и длинный текст на
 * экране 1080p уходил за нижний край.
 */
fun ReceiverState.cardCaption(): String = when {
    phones.isNotEmpty() -> listOfNotNull("Управляет телевизором", address?.let { "адрес $it" }).joinToString(" · ")
    address != null -> "Введите этот адрес на телефоне, если он не находит телевизор"
    else -> "Подключите телевизор к Wi-Fi или кабелю"
}
