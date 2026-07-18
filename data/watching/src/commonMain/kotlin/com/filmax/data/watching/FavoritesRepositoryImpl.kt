package com.filmax.data.watching

import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.favorites.model.toFavoriteItem
import com.filmax.core.domain.user.UserRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * «Буду смотреть» на сервере — поверх папки-закладки kino.pub, а не в SharedPreferences.
 *
 * Раньше список жил только локально: не переживал переустановку и не синхронизировался между
 * устройствами (сервер отдаёт лишь тоггл `togglewatchlist` и флаг `in_watchlist`, но НЕ список).
 * У закладок список есть (`bookmarks/{id}`), поэтому «Буду смотреть» держим в выделенной папке
 * [FOLDER_TITLE]: `getBookmarkFolders` → найти/создать, `addToBookmark`/`removeFromBookmark` для
 * тоггла, `getBookmarkItems` для чтения. Итог: список общий между телефоном и ТВ и переживает
 * переустановку.
 *
 * Локальный кэш ([CACHE_KEY]) — зеркало для мгновенного показа и работы офлайн; источник правды
 * всё равно сервер, кэш обновляется после каждого сетевого ответа.
 */
// Репозиторий: реализация интерфейса + связные приватные помощники (папка, кэш, миграция).
// Дробить на классы значило бы размазать одну ответственность — «Буду смотреть на сервере».
@Suppress("TooManyFunctions")
internal class FavoritesRepositoryImpl(
    private val userRepository: UserRepository,
    private val settings: Settings,
) : FavoritesRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val folderMutex = Mutex()
    private val state = MutableStateFlow(loadCache())

    init {
        scope.launch {
            migrateLegacyIfNeeded()
            refresh()
        }
    }

    override val favorites: Flow<List<FavoriteItem>> = state.asStateFlow()

    override val favoriteIds: Flow<Set<Int>> = state.map { list -> list.map { it.id }.toSet() }

    override fun isFavorite(id: Int): Flow<Boolean> = state.map { list -> list.any { it.id == id } }

    override suspend fun toggle(item: FavoriteItem): Boolean =
        if (state.value.any { it.id == item.id }) {
            remove(item.id)
            false
        } else {
            add(item)
            true
        }

    override suspend fun add(item: FavoriteItem) {
        // Оптимистично: сердечко/список реагируют мгновенно, сервер догоняет. Расхождение
        // при сбое выправит следующий refresh (источник правды — сервер).
        updateState(state.value.filterNot { it.id == item.id } + item)
        val folderId = ensureFolderId() ?: return
        userRepository.addToBookmark(item.id, folderId)
    }

    override suspend fun remove(id: Int) {
        updateState(state.value.filterNot { it.id == id })
        val folderId = ensureFolderId() ?: return
        userRepository.removeFromBookmark(id, folderId)
    }

    /** Перечитывает папку с сервера в кэш. Тихо выходит, если папки/сети нет. */
    private suspend fun refresh() {
        val folderId = ensureFolderId() ?: return
        val collected = mutableListOf<FavoriteItem>()
        var page = 1
        var hasMore = true
        while (hasMore && page <= MAX_PAGES) {
            val pageResult = userRepository.getBookmarkItems(folderId, page).getOrNull()
            val items = pageResult?.items.orEmpty()
            collected += items.map { it.toFavoriteItem() }
            hasMore = pageResult != null && items.isNotEmpty() && page < pageResult.pagination.total
            page++
        }
        updateState(collected)
    }

    /**
     * Id папки «Буду смотреть»: из кэша, иначе найти по имени, иначе создать. Под мьютексом —
     * иначе два параллельных `add` создали бы две одноимённые папки.
     */
    private suspend fun ensureFolderId(): Int? = folderMutex.withLock {
        settings.getIntOrNull(FOLDER_ID_KEY)?.let { return it }
        val folders = userRepository.getBookmarkFolders().getOrNull() ?: return null
        val existing = folders.firstOrNull { it.title == FOLDER_TITLE }
        val folderId = existing?.id
            ?: userRepository.createBookmarkFolder(FOLDER_TITLE).getOrNull()?.id
            ?: return null
        settings.putInt(FOLDER_ID_KEY, folderId)
        folderId
    }

    /** Одноразовый перенос старого локального списка на сервер. */
    private suspend fun migrateLegacyIfNeeded() {
        if (settings.getBoolean(MIGRATED_KEY, false)) return
        val legacy = settings.getStringOrNull(LEGACY_KEY)
            ?.let { runCatching { json.decodeFromString<List<Stored>>(it) }.getOrNull() }
            .orEmpty()
        // Пустой список — переносить нечего, помечаем готовым. Иначе шлём на сервер, но только
        // если папка доступна: нет сети (folderId == null) — не помечаем, попробуем при следующем
        // запуске.
        val migrated = if (legacy.isEmpty()) {
            true
        } else {
            ensureFolderId()?.also { folderId ->
                legacy.forEach { userRepository.addToBookmark(it.id, folderId) }
            } != null
        }
        if (migrated) settings.putBoolean(MIGRATED_KEY, true)
    }

    private fun updateState(list: List<FavoriteItem>) {
        settings.putString(CACHE_KEY, json.encodeToString(list.map { it.toStored() }))
        state.value = list
    }

    private fun loadCache(): List<FavoriteItem> =
        settings.getStringOrNull(CACHE_KEY)
            ?.let { runCatching { json.decodeFromString<List<Stored>>(it) }.getOrNull() }
            ?.map { it.toModel() }
            ?: emptyList()

    @Serializable
    private data class Stored(
        val id: Int,
        val title: String,
        val posterSmall: String,
        val year: Int,
        val durationMinutes: Int,
    )

    private fun FavoriteItem.toStored() = Stored(id, title, posterSmall, year, durationMinutes)
    private fun Stored.toModel() = FavoriteItem(id, title, posterSmall, year, durationMinutes)

    private companion object {
        const val FOLDER_TITLE = "Буду смотреть"
        const val FOLDER_ID_KEY = "watchlist_folder_id"
        const val CACHE_KEY = "watchlist_cache"
        const val MIGRATED_KEY = "watchlist_migrated"
        const val LEGACY_KEY = "favorite_items"
        const val MAX_PAGES = 10
        val json = Json { ignoreUnknownKeys = true }
    }
}
