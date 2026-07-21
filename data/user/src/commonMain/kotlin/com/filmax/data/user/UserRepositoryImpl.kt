package com.filmax.data.user

import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.common.ErrorReporting
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.onSuccess
import com.filmax.core.domain.common.safeRequest
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.data.catalog.mapper.toDomain
import com.filmax.data.user.remote.UpdateDeviceSettingsParams
import com.filmax.data.user.remote.UserApi

internal class UserRepositoryImpl(
    private val api: UserApi,
) : UserRepository {

    override suspend fun getProfile(): RequestResult<UserProfile> = safeRequest {
        val dto = api.getAccountInfo()
        val user = requireNotNull(dto.user)
        // Подписка может прийти вложенной в `user` (актуальный ответ kino.pub)
        // или на верхнем уровне — берём то, что есть.
        val subscriptionDto = user.subscription ?: dto.subscription
        UserProfile(
            id = user.id ?: 0,
            username = user.username,
            email = user.email,
            avatarUrl = user.avatar ?: user.profile?.avatar,
            subscription = subscriptionDto?.let {
                Subscription(
                    active = it.active,
                    endsAt = it.endTime?.times(MILLIS_IN_SECOND),
                    daysLeft = it.days?.toInt(),
                )
            },
        )
    }
        // Профиль — единственное место, где известен username: привязываем к нему телеметрию.
        .onSuccess { profile -> ErrorReporting.reporter.setUser(profile.username) }

    override suspend fun getDeviceSettings(): RequestResult<DeviceSettings> = safeRequest {
        requireNotNull(api.getDeviceSettings().device).toDomain()
    }

    override suspend fun updateDeviceSettings(settings: DeviceSettings): RequestResult<Unit> = safeRequest {
        api.updateDeviceSettings(
            UpdateDeviceSettingsParams(
                id = settings.id,
                supportSsl = if (settings.supportSsl) 1 else 0,
                supportHevc = if (settings.supportHevc) 1 else 0,
                supportHdr = if (settings.supportHdr) 1 else 0,
                support4k = if (settings.support4k) 1 else 0,
                mixedPlaylist = if (settings.mixedPlaylist) 1 else 0,
                streamingType = settings.streamingType,
                serverLocation = settings.serverLocation,
            ),
        )
    }

    override suspend fun registerDevice(
        title: String,
        hardware: String,
        software: String,
    ): RequestResult<Unit> = safeRequest { api.registerDevice(title, hardware, software) }

    override suspend fun getBookmarkFolders(): RequestResult<List<BookmarkFolder>> = safeRequest {
        api.getBookmarks().items.map {
            BookmarkFolder(
                id = it.id,
                title = it.title,
                count = it.count,
                updatedAt = it.updatedAt?.toLong()?.times(MILLIS_IN_SECOND),
            )
        }
    }

    override suspend fun getBookmarkItems(folderId: Int, page: Int): RequestResult<ItemPage> =
        safeRequest { api.getBookmarkItems(folderId, page).toDomain() }

    override suspend fun createBookmarkFolder(title: String): RequestResult<BookmarkFolder> = safeRequest {
        val result = api.createBookmark(title)
        BookmarkFolder(
            id = requireNotNull(result.id),
            title = title,
            count = 0,
            updatedAt = null,
        )
    }

    override suspend fun deleteBookmarkFolder(folderId: Int): RequestResult<Unit> =
        safeRequest { api.deleteBookmark(folderId) }

    override suspend fun addToBookmark(itemId: Int, folderId: Int): RequestResult<Unit> =
        safeRequest { api.addBookmarkItem(itemId, folderId) }

    override suspend fun removeFromBookmark(itemId: Int, folderId: Int): RequestResult<Unit> =
        safeRequest { api.removeBookmarkItem(itemId, folderId) }

    private companion object {
        // kino.pub отдаёт временные метки в секундах — переводим в миллисекунды.
        const val MILLIS_IN_SECOND = 1000
    }
}
