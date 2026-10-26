package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.TvDevice

/** Не получилось подключиться к телевизору [deviceName] — и почему; [message] — то же словами. */
data class TvPickerFailure(val deviceName: String, val reason: LinkFailure) {
    val message: String = linkFailureMessage(reason, deviceName)
}

data class TvPickerState(
    /** Свои телевизоры в сети — того же аккаунта kino.pub. */
    val devices: List<TvDevice> = emptyList(),
    /** Поиск идёт дольше, чем обычно находится ТВ, — пора подсказать, почему его может не быть. */
    val searchTakesLong: Boolean = false,
    /** Телевизор, к которому подключается телефон; пока он есть, второй выбор не принимается. */
    val connectingTo: String? = null,
    /** Телевизор, с которым связь уже есть: выбрать его — значит сразу отдать ему тайтл. */
    val connectedTo: String? = null,
    val failure: TvPickerFailure? = null,
    /** Открыт ввод адреса вручную — для сетей, где роутер режет поиск. */
    val manualOpen: Boolean = false,
    val address: String = "",
    val addressInvalid: Boolean = false,
    /** Окно открыто ради тайтла (а не ради пульта): заголовок и кнопки говорят «Смотреть на ТВ». */
    val withTitle: Boolean = false,
    /** Заголовок окна — по тому, ради чего оно открыто. */
    val title: String = pickerTitle(withTitle),
)

/** «Смотреть на телевизоре», когда есть что запустить, иначе — «Подключить телевизор». */
fun pickerTitle(withTitle: Boolean): String = if (withTitle) "Смотреть на телевизоре" else "Подключить телевизор"

sealed interface TvPickerEvent {
    data class Choose(val deviceId: String) : TvPickerEvent
    data object OpenManual : TvPickerEvent
    data class EditAddress(val text: String) : TvPickerEvent
    data object ConnectByAddress : TvPickerEvent
    data object Dismiss : TvPickerEvent
}

/** Переходы окна выбора ТВ; исполняет их навигатором экран. */
sealed interface TvPickerSideEffect {
    /** Связь есть — окно уступает место пульту. */
    data object OpenRemote : TvPickerSideEffect
    data object Close : TvPickerSideEffect
}
