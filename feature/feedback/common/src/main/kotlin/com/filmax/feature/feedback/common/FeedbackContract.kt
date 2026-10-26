package com.filmax.feature.feedback.common

import com.filmax.core.domain.feedback.Feedback
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.presentation.input.KeyboardLayout
import com.filmax.core.presentation.input.SPACE_KEY
import com.filmax.core.presentation.input.erasedText
import com.filmax.core.presentation.input.typedText

/**
 * Окно оценки. [rating] — 0, пока звезда не выбрана: без оценки отправлять нечего. [layout] —
 * раскладка экранной клавиатуры ТВ; на телефоне комментарий набирают системной. [title] — по
 * поводу, с которым окно открылось; повод за жизнь окна не меняется.
 */
data class FeedbackState(
    val trigger: FeedbackTrigger = FeedbackTrigger.Profile,
    val title: String = titleOf(trigger),
    val rating: Int = 0,
    val comment: String = "",
    val layout: KeyboardLayout = KeyboardLayout.RU,
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
) {
    val canSend: Boolean get() = rating > 0 && !sending && !sent

    /** Клавиша экранной клавиатуры ТВ — в комментарий. */
    fun withInput(input: FeedbackInput): FeedbackState = when (input) {
        is FeedbackInput.Letter -> copy(comment = typedText(comment, input.key, Feedback.COMMENT_MAX_LENGTH))
        FeedbackInput.Space -> copy(comment = typedText(comment, SPACE_KEY, Feedback.COMMENT_MAX_LENGTH))
        FeedbackInput.Erase -> copy(comment = erasedText(comment))
        is FeedbackInput.Layout -> copy(layout = input.layout)
    }
}

/** Заголовок окна — по поводу, с которым оно открылось. */
fun titleOf(trigger: FeedbackTrigger): String = when (trigger) {
    FeedbackTrigger.AfterUpdate -> "Как вам обновление?"
    FeedbackTrigger.AfterWatch -> "Как смотрелось?"
    FeedbackTrigger.Profile -> "Оцените Filmax"
}

sealed interface FeedbackEvent {
    data class Rate(val rating: Int) : FeedbackEvent

    /** Комментарий целиком — с системной клавиатуры телефона. */
    data class Comment(val text: String) : FeedbackEvent

    /** Клавиша экранной клавиатуры ТВ. */
    data class Key(val input: FeedbackInput) : FeedbackEvent
    data object Send : FeedbackEvent

    /** Окно закрывается: «Не сейчас», «Назад» или «Готово» после отправки. */
    data object Dismiss : FeedbackEvent
}

/** Что пульт делает с комментарием: буква, пробел, «Стереть», раскладка. */
sealed interface FeedbackInput {
    data class Letter(val key: String) : FeedbackInput
    data object Space : FeedbackInput
    data object Erase : FeedbackInput
    data class Layout(val layout: KeyboardLayout) : FeedbackInput
}

/** Одноразовых эффектов нет: окно закрывает навигатор в модели. */
sealed interface FeedbackSideEffect
