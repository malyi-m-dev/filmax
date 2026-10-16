package com.filmax.data.feedback

import com.filmax.core.domain.feedback.Feedback
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Отзывы — документами в коллекцию `feedback` Firestore, через REST, а не Android SDK: модуль
 * общий с iOS, и SDK Firestore тянуть в приложение ради одной записи незачем.
 *
 * Запрос без входа в Firebase: правила базы (`firestore.rules`) разрешают в `feedback` только
 * создание с проверкой полей — прочитать, изменить или удалить отзыв из приложения нельзя. Время
 * отзыва в документе не храним: Firestore сам ставит `createTime`, а часы телевизора бывают
 * сбиты.
 */
internal class FirestoreFeedbackApi(engine: HttpClientEngine, projectId: String) {

    private val client = HttpClient(engine) { expectSuccess = true }

    private val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/feedback"

    suspend fun send(feedback: Feedback) {
        client.post(url) { setBody(TextContent(document(feedback).toString(), ContentType.Application.Json)) }
    }

    /** Документ Firestore: каждое поле — объект с типом значения («stringValue», «integerValue»). */
    internal fun document(feedback: Feedback): JsonObject = buildJsonObject {
        putJsonObject("fields") {
            putJsonObject("rating") { put("integerValue", feedback.rating.toString()) }
            string("comment", feedback.comment.take(Feedback.COMMENT_MAX_LENGTH))
            string("trigger", feedback.trigger.name)
            string("appVersion", feedback.client.appVersion)
            string("platform", if (feedback.client.isTv) "tv" else "mobile")
            string("device", feedback.client.device)
            string("os", feedback.client.os)
            string("username", feedback.username.orEmpty())
        }
    }

    private fun JsonObjectBuilder.string(name: String, value: String) {
        putJsonObject(name) { put("stringValue", JsonPrimitive(value)) }
    }
}
