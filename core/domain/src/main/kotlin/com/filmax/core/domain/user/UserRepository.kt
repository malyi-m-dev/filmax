package com.filmax.core.domain.user

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.UserProfile

interface UserRepository {

    suspend fun getProfile(): UserProfile

    suspend fun getDeviceSettings(): DeviceSettings

    suspend fun updateDeviceSettings(settings: DeviceSettings)

    suspend fun registerDevice(title: String, hardware: String, software: String)

    suspend fun getBookmarkFolders(): List<BookmarkFolder>

    suspend fun getBookmarkItems(folderId: Int, page: Int = 1): ItemPage

    suspend fun createBookmarkFolder(title: String): BookmarkFolder

    suspend fun deleteBookmarkFolder(folderId: Int)

    suspend fun addToBookmark(itemId: Int, folderId: Int)

    suspend fun removeFromBookmark(itemId: Int, folderId: Int)
}
