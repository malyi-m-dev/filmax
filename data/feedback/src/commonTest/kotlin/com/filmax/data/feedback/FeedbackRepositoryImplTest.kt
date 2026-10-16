package com.filmax.data.feedback

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.feedback.Feedback
import com.filmax.core.domain.feedback.FeedbackTrigger
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FeedbackRepositoryImplTest {

    private val feedback = Feedback(
        rating = 4,
        comment = "Нравится, но хочется тёмную тему",
        trigger = FeedbackTrigger.AfterWatch,
        client = ClientInfo(appVersion = "2.2.0", isTv = true, device = "Xiaomi MIBOX4", os = "Android 9"),
        username = "viewer",
    )

    @Test
    fun `feedback goes to the feedback collection as typed firestore fields`() = runTest {
        var method: HttpMethod? = null
        var path = ""
        var body = ""
        val engine = MockEngine { request ->
            method = request.method
            path = request.url.encodedPath
            body = (request.body as TextContent).text
            respond("{}", HttpStatusCode.OK)
        }

        val result = FeedbackRepositoryImpl(FirestoreFeedbackApi(engine, projectId = "filmax-test")).send(feedback)

        assertIs<RequestResult.Success<Unit>>(result)
        assertEquals(HttpMethod.Post, method)
        assertEquals("/v1/projects/filmax-test/databases/(default)/documents/feedback", path)
        val fields = Json.parseToJsonElement(body).jsonObject.getValue("fields").jsonObject
        assertEquals("4", fields.value("rating", "integerValue"))
        assertEquals("tv", fields.value("platform", "stringValue"))
        assertEquals("AfterWatch", fields.value("trigger", "stringValue"))
        assertEquals("viewer", fields.value("username", "stringValue"))
    }

    @Test
    fun `rejected write is an error, not a crash`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.Forbidden) }

        val result = FeedbackRepositoryImpl(FirestoreFeedbackApi(engine, projectId = "filmax-test")).send(feedback)

        assertIs<RequestResult.Error>(result)
    }

    private fun kotlinx.serialization.json.JsonObject.value(field: String, type: String): String =
        getValue(field).jsonObject.getValue(type).jsonPrimitive.content
}
