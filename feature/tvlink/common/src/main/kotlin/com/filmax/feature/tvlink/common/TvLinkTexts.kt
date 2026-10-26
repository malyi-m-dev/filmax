// Подписи связи телефона и ТВ — чистые функции, результат ложится в состояния пульта и выбора ТВ.

package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.ReceiverState

/** Почему не вышло, и что с этим сделать, — человеческим языком. */
fun linkFailureMessage(failure: LinkFailure, deviceName: String): String = when (failure) {
    LinkFailure.Unreachable ->
        "«$deviceName» не отвечает. Откройте Filmax на телевизоре — он должен быть в той же Wi-Fi сети."
    LinkFailure.OtherAccount ->
        "На «$deviceName» вход в другой аккаунт kino.pub. Управлять можно только телевизором своего аккаунта."
    LinkFailure.Dropped -> "Связь с «$deviceName» прервалась: телевизор выключили, свернули Filmax или пропал Wi-Fi."
}

/** «Сезон 4 · Серия 7 · Перевал»; null у фильма. */
fun episodeLabel(playback: PlaybackStatus): String? {
    if (playback.season <= 0) return null
    return listOfNotNull("Сезон ${playback.season}", "Серия ${playback.episode}", playback.episodeTitle)
        .joinToString(" · ")
}

/** Серия, а пока поток грузится — «Загружается»: иначе пауза и загрузка на пульте неотличимы. */
fun playbackCaption(playback: PlaybackStatus): String? =
    listOfNotNull(episodeLabel(playback), "Загружается".takeIf { playback.buffering })
        .joinToString(" · ")
        .ifEmpty { null }

/** Крупная строка карточки «Пульт с телефона»: кто управляет, иначе — адрес, по которому подключиться. */
fun remoteCardValue(received: ReceiverState): String = when {
    received.phones.isNotEmpty() -> received.phones.joinToString(", ")
    else -> received.address ?: "Нет сети"
}

/**
 * Пояснение под ней — что делать дальше. Коротко: карточка стоит под подпиской, и длинный текст на
 * экране 1080p уходил за нижний край.
 */
fun remoteCardCaption(received: ReceiverState): String = when {
    received.phones.isNotEmpty() ->
        listOfNotNull("Управляет телевизором", received.address?.let { "адрес $it" }).joinToString(" · ")
    received.address != null -> "Введите этот адрес на телефоне, если он не находит телевизор"
    else -> "Подключите телевизор к Wi-Fi или кабелю"
}
