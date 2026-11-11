package com.filmax.data.jellyfin

import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.Pagination
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile

/**
 * Профиль на домашнем сервере. Подписки нет — сервер свой, поэтому доступ всегда открыт, и
 * плеер не показывает плашку «нужна подписка». Папок-закладок у сервера в смысле kino.pub нет
 * (плейлисты раскладывают сериал на серии), поэтому папки пока только у kino.pub, а «Буду
 * смотреть» здесь — избранное сервера.
 */
// Реализация всего контракта UserRepository — столько же методов.
@Suppress("TooManyFunctions")
internal class JellyfinUserRepository(private val context: JellyfinContext) : UserRepository {

    override suspend fun getProfile(): RequestResult<UserProfile> = context.request { mapper ->
        val user = context.api.me()
        UserProfile(
            id = JellyfinIds.preferredId(user.id.orEmpty()),
            username = user.name.orEmpty(),
            email = null,
            avatarUrl = mapper.images.user(user.id, user.primaryImageTag),
            subscription = Subscription(active = true, endsAt = null, daysLeft = null),
        )
    }

    // Устройство сервер и так знает: имя и id идут в заголовке каждого запроса.
    override suspend fun registerDevice(title: String, hardware: String, software: String): RequestResult<Unit> =
        RequestResult.Success(Unit)

    override suspend fun getBookmarkFolders(): RequestResult<List<BookmarkFolder>> = RequestResult.Success(emptyList())

    override suspend fun getBookmarkItems(folderId: Int, page: Int): RequestResult<ItemPage> =
        RequestResult.Success(ItemPage(emptyList(), Pagination(total = 1, current = 1, perPage = 0)))

    override suspend fun createBookmarkFolder(title: String): RequestResult<BookmarkFolder> =
        RequestResult.Error(NO_FOLDERS)

    override suspend fun deleteBookmarkFolder(folderId: Int): RequestResult<Unit> = RequestResult.Error(NO_FOLDERS)

    override suspend fun addToBookmark(itemId: Int, folderId: Int): RequestResult<Unit> =
        RequestResult.Error(NO_FOLDERS)

    override suspend fun removeFromBookmark(itemId: Int, folderId: Int): RequestResult<Unit> =
        RequestResult.Error(NO_FOLDERS)

    private companion object {
        const val NO_FOLDERS = "Папки пока есть только у kino.pub"
    }
}
