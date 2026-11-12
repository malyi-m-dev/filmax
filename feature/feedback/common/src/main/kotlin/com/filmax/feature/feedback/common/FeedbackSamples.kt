// Неизменные образцы окна оценки — для превью телефона и ТВ.
package com.filmax.feature.feedback.common

import com.filmax.core.domain.feedback.FeedbackTrigger

private const val SAMPLE_RATING = 4

object FeedbackSamples {

    val empty = FeedbackState(trigger = FeedbackTrigger.AfterUpdate)
    val rated = empty.copy(rating = SAMPLE_RATING, comment = "Нравится новая Главная")
    val sending = rated.copy(sending = true)
    val failed = rated.copy(error = "Не удалось отправить — проверьте сеть")
    val sent = rated.copy(sent = true)

    val all: List<FeedbackState> = listOf(empty, rated, sending, failed, sent)
}
