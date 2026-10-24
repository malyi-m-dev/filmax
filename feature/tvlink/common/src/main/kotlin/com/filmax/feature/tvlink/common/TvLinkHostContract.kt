package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.RemoteCommand

data class TvLinkHostState(
    /** Имя телефона, который только что подключился как пульт, — плашка на пару секунд. */
    val joinedPhone: String? = null,
)

sealed interface TvLinkHostEvent {
    /**
     * Filmax на экране ТВ и вошёл в аккаунт — принимать телефоны. Свернули или вышли из аккаунта —
     * [Hidden]: ТВ пропадает из сети, иначе телефон «подключался» бы к приложению, которого не видно.
     */
    data object Shown : TvLinkHostEvent
    data object Hidden : TvLinkHostEvent
}

sealed interface TvLinkHostSideEffect {
    /**
     * Телефон отдал тайтл: открыть плеер. [replacePlayer] — плеер уже открыт (другой тайтл или серия):
     * новый встаёт на его место, а не копит стек плееров.
     */
    data class OpenPlayer(val play: RemoteCommand.Play, val replacePlayer: Boolean) : TvLinkHostSideEffect
}
