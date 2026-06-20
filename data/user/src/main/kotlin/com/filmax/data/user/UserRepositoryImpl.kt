package com.filmax.data.user

import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.data.catalog.mapper.toDomain
import com.filmax.data.user.remote.UserApi
import com.filmax.data.user.remote.dto.DeviceInfoDto
import javax.inject.Inject

internal class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
) : UserRepository {

    override suspend fun getProfile(): UserProfile {
        val dto = api.getAccountInfo()
        val user = requireNotNull(dto.user)
        return UserProfile(
            id = user.id,
            username = user.username,
            email = user.email,
            avatarUrl = user.avatar,
            subscription = dto.subscription?.let {
                Subscription(
                    active = it.active,
                    endsAt = it.end?.toLong()?.times(1000),
                    daysLeft = it.days,
                )
            },
        )
    }

    override suspend fun getDeviceSettings(): DeviceSettings {
        val info = requireNotNull(api.getDeviceSettings().device)
        return info.toDomain()
    }

    override suspend fun updateDeviceSettings(settings: DeviceSettings) {
        api.updateDeviceSettings(
            id = settings.id,
            supportSsl = if (settings.supportSsl) 1 else 0,
            supportHevc = if (settings.supportHevc) 1 else 0,
            supportHdr = if (settings.supportHdr) 1 else 0,
            support4k = if (settings.support4k) 1 else 0,
            mixedPlaylist = 0,
            streamingType = settings.streamingType,
            serverLocation = settings.serverLocation,
        )
    }

    override suspend fun registerDevice(title: String, hardware: String, software: String) {
        api.registerDevice(title, hardware, software)
    }

    override suspend fun getBookmarkFolders(): List<BookmarkFolder> =
        api.getBookmarks().items.map {
            BookmarkFolder(
                id = it.id,
                title = it.title,
                count = it.count,
                updatedAt = it.updatedAt?.toLong()?.times(1000),
            )
        }

    override suspend fun getBookmarkItems(folderId: Int, page: Int): ItemPage =
        api.getBookmarkItems(folderId, page).toDomain()

    override suspend fun createBookmarkFolder(title: String): BookmarkFolder {
        val result = api.createBookmark(title)
        return BookmarkFolder(
            id = requireNotNull(result.id),
            title = title,
            count = 0,
            updatedAt = null,
        )
    }

    override suspend fun deleteBookmarkFolder(folderId: Int) {
        api.deleteBookmark(folderId)
    }

    override suspend fun addToBookmark(itemId: Int, folderId: Int) {
        api.addBookmarkItem(itemId, folderId)
    }

    override suspend fun removeFromBookmark(itemId: Int, folderId: Int) {
        api.removeBookmarkItem(itemId, folderId)
    }

    private fun DeviceInfoDto.toDomain() = DeviceSettings(
        id = id,
        title = title,
        supportSsl = supportSsl == 1,
        supportHevc = supportHevc == 1,
        supportHdr = supportHdr == 1,
        support4k = support4k == 1,
        streamingType = streamingType,
        serverLocation = serverLocation,
    )
}
