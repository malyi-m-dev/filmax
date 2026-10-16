package com.filmax.data.fake

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.feedback.Feedback
import com.filmax.core.domain.feedback.FeedbackRepository

/**
 * Отзывы без Firestore: мок-сборка не пишет в боевую базу, а тесты видят, что ушло. [fails] —
 * сервер отказал (нет сети, правила базы отклонили запись).
 */
class FakeFeedbackRepository(private val backend: FakeBackend) : FeedbackRepository {

    val sent = mutableListOf<Feedback>()
    var fails = false

    override suspend fun send(feedback: Feedback): RequestResult<Unit> = backend.respond {
        if (fails) {
            RequestResult.Error("Не удалось отправить отзыв")
        } else {
            sent += feedback
            RequestResult.Success(Unit)
        }
    }
}
