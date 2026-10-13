package com.filmax.core.domain.usecase.bookmarks

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.common.map
import com.filmax.core.domain.favorites.WATCHLIST_FOLDER_TITLE
import com.filmax.core.domain.user.FolderAppearanceRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.FolderAppearance
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

/** Сколько постеров веером на карточке папки. */
const val FOLDER_PREVIEW_POSTERS = 3

/**
 * Предел страниц, которые просматриваются в поисках тайтла: папка на сотни тайтлов — редкость,
 * а без потолка открытие панели «В папку» могло бы растянуться на десятки запросов.
 */
private const val MAX_SCANNED_PAGES = 10

/** Серверный список папок и постеры для веера на их карточках (по id папки). */
data class FolderShelf(val folders: List<BookmarkFolder>, val posters: Map<Int, List<String>>)

/**
 * Папки-закладки пользователя целиком: серверная часть (kino.pub) и локальное оформление (цвет,
 * переименование — у сервера их нет, см. FolderAppearance). Одна точка для «Моё», экрана папки и
 * панели «В папку»: все три видят папку одинаково, и правило «удалил папку — забудь её цвет»
 * живёт в одном месте, а не в каждой модели.
 */
class BookmarkFolders(
    private val user: UserRepository,
    private val appearance: FolderAppearanceRepository,
) {
    /** Оформление папок потоком: перекраска и переименование сразу видны на всех экранах. */
    val appearances: Flow<Map<Int, FolderAppearance>> get() = appearance.appearances

    suspend fun list(): RequestResult<List<BookmarkFolder>> = user.getBookmarkFolders()

    /**
     * Папки вместе с постерами для веера — первыми тайтлами первой страницы каждой папки.
     *
     * Список папок kino.pub (`bookmarks`) приходит без содержимого, поэтому за постерами — по
     * одному запросу `bookmarks/{id}` на непустую папку пользователя, параллельно (у системной
     * «Буду смотреть» карточки нет). Папка, чей запрос упал, остаётся без веера: это украшение.
     */
    suspend fun shelf(): RequestResult<FolderShelf> =
        user.getBookmarkFolders().map { folders -> FolderShelf(folders, posters(folders)) }

    /**
     * В каких папках лежит тайтл. Запроса «папки тайтла» в репозиториях нет, поэтому содержимое
     * папок просматривается постранично, пока тайтл не найдётся, — все непустые папки параллельно.
     */
    suspend fun itemFolders(itemId: Int, folders: List<BookmarkFolder>): Set<Int> = coroutineScope {
        folders
            .filter { it.count > 0 }
            .map { folder -> async { folder.id.takeIf { contains(folder.id, itemId) } } }
            .awaitAll()
            .filterNotNull()
            .toSet()
    }

    /**
     * Создаёт папку с цветом и, если передан [itemId], сразу кладёт в неё тайтл — «Создать и
     * добавить». Цвет сохраняется только после того, как сервер выдал id папки.
     */
    suspend fun create(title: String, tint: Int, itemId: Int? = null): RequestResult<BookmarkFolder> {
        val created = user.createBookmarkFolder(title)
        val folder = created.getOrNull() ?: return created
        appearance.setTint(folder.id, tint)
        if (itemId != null) user.addToBookmark(itemId, folder.id)
        return created
    }

    /** Переименование и цвет — локальные: у kino.pub переименования папки нет. */
    suspend fun restyle(folderId: Int, title: String, tint: Int) {
        appearance.rename(folderId, title)
        appearance.setTint(folderId, tint)
    }

    /** Удаляет папку вместе с её оформлением — иначе оно осело бы в настройках навсегда. */
    suspend fun delete(folderId: Int): RequestResult<Unit> {
        val deleted = user.deleteBookmarkFolder(folderId)
        if (deleted is RequestResult.Success) appearance.forget(folderId)
        return deleted
    }

    private suspend fun posters(folders: List<BookmarkFolder>): Map<Int, List<String>> = coroutineScope {
        folders
            .filter { it.count > 0 && it.title != WATCHLIST_FOLDER_TITLE }
            .map { folder -> async { folder.id to firstPosters(folder.id) } }
            .awaitAll()
            .toMap()
    }

    private suspend fun firstPosters(folderId: Int): List<String> =
        user.getBookmarkItems(folderId).getOrNull()?.items.orEmpty()
            .take(FOLDER_PREVIEW_POSTERS)
            .map { it.previewPoster() }

    /** Листает папку, пока тайтл не найдётся, страницы не кончатся или не упадёт запрос. */
    private suspend fun contains(folderId: Int, itemId: Int): Boolean {
        var page = 1
        var found = false
        var hasMore = true
        while (!found && hasMore && page <= MAX_SCANNED_PAGES) {
            val itemPage = user.getBookmarkItems(folderId, page).getOrNull()
            found = itemPage?.items.orEmpty().any { it.id == itemId }
            hasMore = itemPage?.pagination?.hasNextPage == true
            page++
        }
        return found
    }
}

private fun Item.previewPoster(): String = posters.medium.ifBlank { posters.small }
