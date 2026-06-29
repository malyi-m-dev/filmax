package com.filmax.core.domain.user

import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.UserProfile

interface UserRepository {

    suspend fun getProfile(): RequestResult<UserProfile>

    suspend fun getDeviceSettings(): RequestResult<DeviceSettings>

    suspend fun updateDeviceSettings(settings: DeviceSettings): RequestResult<Unit>

    suspend fun registerDevice(title: String, hardware: String, software: String): RequestResult<Unit>

    suspend fun getBookmarkFolders(): RequestResult<List<BookmarkFolder>>

    suspend fun getBookmarkItems(folderId: Int, page: Int = 1): RequestResult<ItemPage>

    suspend fun createBookmarkFolder(title: String): RequestResult<BookmarkFolder>

    suspend fun deleteBookmarkFolder(folderId: Int): RequestResult<Unit>

    suspend fun addToBookmark(itemId: Int, folderId: Int): RequestResult<Unit>

    suspend fun removeFromBookmark(itemId: Int, folderId: Int): RequestResult<Unit>
}
