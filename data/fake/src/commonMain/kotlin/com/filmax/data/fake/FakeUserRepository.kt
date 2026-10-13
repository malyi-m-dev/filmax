package com.filmax.data.fake

import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.data.fake.store.FolderState

/**
 * Профиль «Демо Зритель» с активной подпиской, настройки устройства и папки-закладки. Среди папок
 * есть «Буду смотреть» — по ней FavoritesRepositoryImpl из data:watching строит избранное, поэтому
 * сердечко в деталях и раздел «Моё» работают через этот же репозиторий, без своих фейков.
 */
class FakeUserRepository(private val backend: FakeBackend) : UserRepository {

    override suspend fun getProfile(): RequestResult<UserProfile> = backend.respond { success(account.profile) }

    override suspend fun getDeviceSettings(): RequestResult<DeviceSettings> =
        backend.respond { success(account.deviceSettings()) }

    override suspend fun updateDeviceSettings(settings: DeviceSettings): RequestResult<Unit> = backend.respond {
        account.updateDeviceSettings(settings)
        success(Unit)
    }

    override suspend fun registerDevice(title: String, hardware: String, software: String): RequestResult<Unit> =
        backend.respond {
            account.registerDevice(title)
            success(Unit)
        }

    override suspend fun getBookmarkFolders(): RequestResult<List<BookmarkFolder>> =
        backend.respond { success(library.folders().map { it.toDomain() }) }

    override suspend fun getBookmarkItems(folderId: Int, page: Int): RequestResult<ItemPage> = backend.respond {
        val folder = library.folder(folderId) ?: return@respond notFound("bookmark folder $folderId")
        success(itemPage(folder.itemIds.mapNotNull { catalog.entry(it) }, page))
    }

    override suspend fun createBookmarkFolder(title: String): RequestResult<BookmarkFolder> =
        backend.respond { success(library.createFolder(title).toDomain()) }

    override suspend fun deleteBookmarkFolder(folderId: Int): RequestResult<Unit> = backend.respond {
        if (library.deleteFolder(folderId)) success(Unit) else notFound("bookmark folder $folderId")
    }

    override suspend fun addToBookmark(itemId: Int, folderId: Int): RequestResult<Unit> = backend.respond {
        when {
            catalog.entry(itemId) == null -> notFound("item $itemId")
            library.addItem(folderId, itemId) -> success(Unit)
            else -> notFound("bookmark folder $folderId")
        }
    }

    override suspend fun removeFromBookmark(itemId: Int, folderId: Int): RequestResult<Unit> = backend.respond {
        if (library.removeItem(folderId, itemId)) success(Unit) else notFound("bookmark folder $folderId")
    }
}

private fun FolderState.toDomain(): BookmarkFolder =
    BookmarkFolder(id = id, title = title, count = itemIds.size, updatedAt = updatedAt)
