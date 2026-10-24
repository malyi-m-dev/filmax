package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.PlaybackStatus

/** Почему не вышло, и что с этим сделать, — человеческим языком. */
fun LinkFailure.message(deviceName: String): String = when (this) {
    LinkFailure.Unreachable ->
        "«$deviceName» не отвечает. Откройте Filmax на телевизоре — он должен быть в той же Wi-Fi сети."
    LinkFailure.OtherAccount ->
        "На «$deviceName» вход в другой аккаунт kino.pub. Управлять можно только телевизором своего аккаунта."
    LinkFailure.Dropped -> "Связь с «$deviceName» прервалась: телевизор выключили, свернули Filmax или пропал Wi-Fi."
}

/** «Сезон 4 · Серия 7 · Перевал»; null у фильма. */
fun PlaybackStatus.episodeLabel(): String? {
    if (season <= 0) return null
    return listOfNotNull("Сезон $season", "Серия $episode", episodeTitle).joinToString(" · ")
}
