package com.filmax.data.feedback

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.safeRequest
import com.filmax.core.domain.feedback.Feedback
import com.filmax.core.domain.feedback.FeedbackRepository

internal class FeedbackRepositoryImpl(private val api: FirestoreFeedbackApi) : FeedbackRepository {

    override suspend fun send(feedback: Feedback): RequestResult<Unit> = safeRequest { api.send(feedback) }
}
