package com.filmax.data.user

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.network.networkJson
import com.filmax.data.user.remote.UserApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * `bookmarks/create` на ответах в формате kino.pub. Фейковый data-слой mock-сборки отдаёт папку
 * сразу объектом и формат ответа не проверяет — проверка здесь.
 */
class BookmarkFolderCreateTest {

    @Test
    fun `new folder id is read from the nested folder`() = runTest {
        val repository = repositoryAnswering(
            """
            {"status":200,"folder":{"id":417,"title":"Дача","views":0,"count":0,
            "created":1760400000,"updated":1760400000}}
            """,
        )

        val created = repository.createBookmarkFolder("Дача")

        val folder = assertIs<RequestResult.Success<BookmarkFolder>>(created).data
        assertEquals(BOOKMARK_FOLDER_ID, folder.id)
        assertEquals("Дача", folder.title)
    }

    @Test
    fun `answer without a folder is an error, not a folder with a made-up id`() = runTest {
        val repository = repositoryAnswering("""{"status":200}""")

        assertIs<RequestResult.Error>(repository.createBookmarkFolder("Дача"))
    }

    private fun repositoryAnswering(body: String): UserRepositoryImpl {
        val engine = MockEngine {
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(networkJson) } }
        return UserRepositoryImpl(UserApi(client))
    }

    private companion object {
        const val BOOKMARK_FOLDER_ID = 417
    }
}
