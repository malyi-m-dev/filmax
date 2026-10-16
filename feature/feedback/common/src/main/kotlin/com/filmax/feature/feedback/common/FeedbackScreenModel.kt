package com.filmax.feature.feedback.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.feedback.Feedback
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.feedback.FeedbackRepository
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.core.presentation.input.SPACE_KEY
import com.filmax.core.presentation.input.erasedText
import com.filmax.core.presentation.input.typedText
import com.filmax.feature.feedback.common.navigation.FeedbackRoute

/**
 * Окно оценки: звёзды, комментарий и отправка. Само открывшееся окно (после обновления или
 * досмотра) отмечается в [FeedbackPrompts] сразу — следующий показ не раньше чем через 60 дней,
 * даже если человек закрыл окно, ничего не отправив.
 */
class FeedbackScreenModel(
    savedStateHandle: SavedStateHandle,
    private val repository: FeedbackRepository,
    private val prompts: FeedbackPrompts,
    private val user: UserRepository,
    private val client: ClientInfo,
) : BaseScreenModel<FeedbackState, FeedbackSideEffect, FeedbackEvent>(
    FeedbackState(trigger = triggerOf(savedStateHandle.toRoute<FeedbackRoute>())),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        if (state.trigger == FeedbackTrigger.Profile) return
        screenModelScope { _ -> prompts.onPrompted() }
    }

    override fun onEvent(event: FeedbackEvent) {
        when (event) {
            is FeedbackEvent.Rate -> update {
                it.copy(rating = event.rating.coerceIn(Feedback.RATING_RANGE), error = null)
            }
            is FeedbackEvent.Comment -> update { it.copy(comment = event.text.take(Feedback.COMMENT_MAX_LENGTH)) }
            is FeedbackEvent.Key -> update { it.applied(event.input) }
            FeedbackEvent.Send -> send()
            FeedbackEvent.Dismiss -> screenModelScope { _ -> postSideEffect(FeedbackSideEffect.Close) }
        }
    }

    private fun send() {
        screenModelScope { current ->
            when {
                // На ТВ «Отправить» нажимается и без оценки — подсказываем, чего не хватает.
                current.rating == 0 -> updateState { it.copy(error = RATE_FIRST) }
                current.canSend -> deliver(current)
            }
        }
    }

    private suspend fun deliver(current: FeedbackState) {
        updateState { it.copy(sending = true, error = null) }
        val feedback = Feedback(
            rating = current.rating,
            comment = current.comment.trim(),
            trigger = current.trigger,
            client = client,
            username = user.getProfile().getOrNull()?.username,
        )
        when (repository.send(feedback)) {
            is RequestResult.Success -> {
                prompts.onSent()
                updateState { it.copy(sending = false, sent = true) }
            }
            is RequestResult.Error -> updateState { it.copy(sending = false, error = SEND_ERROR) }
        }
    }

    private fun update(transform: (FeedbackState) -> FeedbackState) {
        screenModelScope { _ -> updateState(transform) }
    }

    private companion object {
        const val SEND_ERROR = "Не удалось отправить — проверьте сеть и попробуйте ещё раз"
        const val RATE_FIRST = "Сначала поставьте оценку — звёзды выше"

        fun triggerOf(route: FeedbackRoute): FeedbackTrigger =
            FeedbackTrigger.entries.firstOrNull { it.name == route.trigger } ?: FeedbackTrigger.Profile
    }
}

/** Клавиша экранной клавиатуры — в комментарий. */
private fun FeedbackState.applied(input: FeedbackInput): FeedbackState = when (input) {
    is FeedbackInput.Letter -> copy(comment = typedText(comment, input.key, Feedback.COMMENT_MAX_LENGTH))
    FeedbackInput.Space -> copy(comment = typedText(comment, SPACE_KEY, Feedback.COMMENT_MAX_LENGTH))
    FeedbackInput.Erase -> copy(comment = erasedText(comment))
    is FeedbackInput.Layout -> copy(layout = input.layout)
}
