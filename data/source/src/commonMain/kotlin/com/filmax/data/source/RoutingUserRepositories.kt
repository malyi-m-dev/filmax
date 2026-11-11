// Переключатели профиля и просмотра: контракты повторяются метод в метод.
package com.filmax.data.source

import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watching.model.WatchHistory

// Столько же функций, сколько в контракте профиля.
@Suppress("TooManyFunctions")
internal class RoutingUserRepository(private val switch: SourceSwitch<UserRepository>) : UserRepository {

    override suspend fun getProfile(): RequestResult<UserProfile> = switch.current.getProfile()

    override suspend fun registerDevice(title: String, hardware: String, software: String): RequestResult<Unit> =
        switch.current.registerDevice(title, hardware, software)

    override suspend fun getBookmarkFolders(): RequestResult<List<BookmarkFolder>> =
        switch.current.getBookmarkFolders()

    override suspend fun getBookmarkItems(folderId: Int, page: Int): RequestResult<ItemPage> =
        switch.current.getBookmarkItems(folderId, page)

    override suspend fun createBookmarkFolder(title: String): RequestResult<BookmarkFolder> =
        switch.current.createBookmarkFolder(title)

    override suspend fun deleteBookmarkFolder(folderId: Int): RequestResult<Unit> =
        switch.current.deleteBookmarkFolder(folderId)

    override suspend fun addToBookmark(itemId: Int, folderId: Int): RequestResult<Unit> =
        switch.current.addToBookmark(itemId, folderId)

    override suspend fun removeFromBookmark(itemId: Int, folderId: Int): RequestResult<Unit> =
        switch.current.removeFromBookmark(itemId, folderId)
}

// Столько же функций, сколько в контракте просмотра.
@Suppress("TooManyFunctions")
internal class RoutingWatchingRepository(private val switch: SourceSwitch<WatchingRepository>) : WatchingRepository {

    override suspend fun getHistory(type: String): RequestResult<List<WatchHistory>> =
        switch.current.getHistory(type)

    override suspend fun saveProgress(itemId: Int, videoId: Int, timeSeconds: Int): RequestResult<Unit> =
        switch.current.saveProgress(itemId, videoId, timeSeconds)

    override suspend fun saveProgressSerial(
        itemId: Int,
        season: Int,
        videoId: Int,
        timeSeconds: Int,
    ): RequestResult<Unit> = switch.current.saveProgressSerial(itemId, season, videoId, timeSeconds)

    override suspend fun toggleWatched(itemId: Int): RequestResult<Unit> = switch.current.toggleWatched(itemId)

    override suspend fun clearHistory(itemId: Int): RequestResult<Unit> = switch.current.clearHistory(itemId)
}
