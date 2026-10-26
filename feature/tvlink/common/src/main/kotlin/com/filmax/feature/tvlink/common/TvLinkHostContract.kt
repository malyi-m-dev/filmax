package com.filmax.feature.tvlink.common

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

/** Одноразовых эффектов нет: тайтл с телефона открывает плеер через навигатор в модели. */
sealed interface TvLinkHostSideEffect
