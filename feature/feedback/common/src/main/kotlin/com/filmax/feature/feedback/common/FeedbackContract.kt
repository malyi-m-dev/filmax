package com.filmax.feature.feedback.common

import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.presentation.input.KeyboardLayout

/**
 * Окно оценки. [rating] — 0, пока звезда не выбрана: без оценки отправлять нечего. [layout] —
 * раскладка экранной клавиатуры ТВ; на телефоне комментарий набирают системной.
 */
data class FeedbackState(
    val trigger: FeedbackTrigger = FeedbackTrigger.Profile,
    val rating: Int = 0,
    val comment: String = "",
    val layout: KeyboardLayout = KeyboardLayout.RU,
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
) {
    val canSend: Boolean get() = rating > 0 && !sending && !sent

    /** Заголовок окна — по поводу, с которым оно открылось. */
    val title: String
        get() = when (trigger) {
            FeedbackTrigger.AfterUpdate -> "Как вам обновление?"
            FeedbackTrigger.AfterWatch -> "Как смотрелось?"
            FeedbackTrigger.Profile -> "Оцените Filmax"
        }
}

sealed interface FeedbackEvent {
    data class Rate(val rating: Int) : FeedbackEvent

    /** Комментарий целиком — с системной клавиатуры телефона. */
    data class Comment(val text: String) : FeedbackEvent

    /** Клавиша экранной клавиатуры ТВ. */
    data class Key(val input: FeedbackInput) : FeedbackEvent
    data object Send : FeedbackEvent
    data object Dismiss : FeedbackEvent
}

/** Что пульт делает с комментарием: буква, пробел, «Стереть», раскладка. */
sealed interface FeedbackInput {
    data class Letter(val key: String) : FeedbackInput
    data object Space : FeedbackInput
    data object Erase : FeedbackInput
    data class Layout(val layout: KeyboardLayout) : FeedbackInput
}

sealed interface FeedbackSideEffect {
    /** Окно закрывается: «Не сейчас», «Назад» или «Готово» после отправки. */
    data object Close : FeedbackSideEffect
}
