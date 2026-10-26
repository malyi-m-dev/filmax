package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.RemoteCommand

data class TvLinkHostState(
    /** Имя телефона, который только что подключился как пульт, — плашка на пару секунд. */
    val joinedPhone: String? = null,
    /**
     * Последнее объявленное имя. Плашка гаснет уже после того, как [joinedPhone] сброшен, и
     * без этого поля исчезала бы пустой.
     */
    val lastJoinedPhone: String = "",
)

sealed interface TvLinkHostEvent {
    /**
     * Filmax на экране ТВ и вошёл в аккаунт — принимать телефоны. Свернули или вышли из аккаунта —
     * [Hidden]: ТВ пропадает из сети, иначе телефон «подключался» бы к приложению, которого не видно.
     */
    data object Shown : TvLinkHostEvent
    data object Hidden : TvLinkHostEvent
}

/** Тайтл с телефона — в плеер; переход исполняет навигатором хост на экране ТВ. */
sealed interface TvLinkHostSideEffect {
    /** Плеер ничего не играет — открыть поверх текущего экрана. */
    data class OpenPlayer(val play: RemoteCommand.Play) : TvLinkHostSideEffect

    /** Плеер уже на экране — новый тайтл встаёт на его место, а не копит стек плееров. */
    data class ReplacePlayer(val play: RemoteCommand.Play) : TvLinkHostSideEffect
}
