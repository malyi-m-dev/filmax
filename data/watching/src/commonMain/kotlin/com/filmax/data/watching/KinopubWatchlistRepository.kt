package com.filmax.data.watching

import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.common.safeRequest
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watchlist.WATCHLIST_FOLDER_TITLE
import com.filmax.core.domain.watchlist.WatchlistRepository
import com.filmax.core.domain.watchlist.model.WatchlistItem
import com.filmax.core.domain.watchlist.model.toWatchlistItem
import com.filmax.data.watching.remote.WatchingApi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * «Буду смотреть» kino.pub — поверх папки-закладки и флага `in_watchlist`.
 *
 * Список kino.pub не отдаёт: у него есть только переключатель `togglewatchlist` и флаг на самом
 * тайтле. Зато у закладок список есть (`bookmarks/{id}`), поэтому тайтлы лежат в выделенной папке
 * [WATCHLIST_FOLDER_TITLE] — она общая для телефона и ТВ и переживает переустановку. Флаг ставится
 * вместе с папкой: по нему отметку видят веб и официальные клиенты, а детали подхватывают
 * отметку, поставленную там ([adopt]).
 *
 * Локальный кэш ([CACHE_KEY]) — зеркало для мгновенного показа и работы офлайн; источник правды
 * — сервер. Список принадлежит аккаунту: после выхода кэш и id папки стираются, после входа
 * список перечитывается, а до входа в сеть репозиторий не ходит.
 */
// Реализация интерфейса и связные приватные помощники (папка, флаг, кэш, перенос старого
// списка). Дробить на классы значило бы размазать одну ответственность — «Буду смотреть» kino.pub.
@Suppress("TooManyFunctions")
internal class KinopubWatchlistRepository(
    private val folders: UserRepository,
    private val api: WatchingApi,
    private val settings: Settings,
    signedIn: Flow<Boolean>,
    scope: CoroutineScope,
) : WatchlistRepository {

    private val folderMutex = Mutex()
    private val state = MutableStateFlow(loadCache())

    init {
        scope.launch {
            signedIn.distinctUntilChanged().collectLatest { signed ->
                if (signed) {
                    migrateLegacyIfNeeded()
                    refresh()
                } else {
                    forget()
                }
            }
        }
    }

    override val items: Flow<List<WatchlistItem>> = state.asStateFlow()

    override fun contains(itemId: Int): Flow<Boolean> = state.map { list -> list.any { it.id == itemId } }

    override suspend fun add(item: WatchlistItem) {
        putInFolder(item)
        markOnServer(item.id, marked = true)
    }

    override suspend fun remove(itemId: Int) {
        updateState(state.value.filterNot { it.id == itemId })
        ensureFolderId()?.let { folderId -> folders.removeFromBookmark(itemId, folderId) }
        markOnServer(itemId, marked = false)
    }

    // Уже в списке — ничего не шлём: детали зовут это на каждое открытие отмеченного тайтла.
    override suspend fun adopt(item: WatchlistItem) {
        if (state.value.none { it.id == item.id }) putInFolder(item)
    }

    /** Оптимистично: кнопка и список реагируют сразу, папка на сервере догоняет. */
    private suspend fun putInFolder(item: WatchlistItem) {
        updateState(state.value.filterNot { it.id == item.id } + item)
        val folderId = ensureFolderId() ?: return
        folders.addToBookmark(item.id, folderId)
    }

    /**
     * Флаг `in_watchlist` у kino.pub — только переключатель, без «поставить» и «снять». Ответ
     * говорит, что вышло; вышло не то — переключаем ещё раз. Без снятия флага первое же открытие
     * деталей вернуло бы снятый тайтл в список.
     */
    private suspend fun markOnServer(itemId: Int, marked: Boolean) {
        val now = safeRequest { api.toggleWatchlist(itemId).isMarked() }.getOrNull() ?: return
        if (now != marked) safeRequest { api.toggleWatchlist(itemId) }
    }

    /** Перечитывает папку с сервера в кэш. Тихо выходит, если папки или сети нет. */
    private suspend fun refresh() {
        val folderId = ensureFolderId() ?: return
        val collected = mutableListOf<WatchlistItem>()
        var page = 1
        var hasMore = true
        while (hasMore && page <= MAX_PAGES) {
            val pageResult = folders.getBookmarkItems(folderId, page).getOrNull()
            val pageItems = pageResult?.items.orEmpty()
            collected += pageItems.map { it.toWatchlistItem() }
            hasMore = pageResult != null && pageItems.isNotEmpty() && page < pageResult.pagination.total
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
        val existing = folders.getBookmarkFolders().getOrNull() ?: return null
        val folderId = existing.firstOrNull { it.title == WATCHLIST_FOLDER_TITLE }?.id
            ?: folders.createBookmarkFolder(WATCHLIST_FOLDER_TITLE).getOrNull()?.id
            ?: return null
        settings.putInt(FOLDER_ID_KEY, folderId)
        folderId
    }

    /** Выход из аккаунта: чужой список и id чужой папки следующему аккаунту не нужны. */
    private fun forget() {
        settings.remove(FOLDER_ID_KEY)
        settings.remove(CACHE_KEY)
        state.value = emptyList()
    }

    /** Одноразовый перенос старого локального списка на сервер. */
    private suspend fun migrateLegacyIfNeeded() {
        if (settings.getBoolean(MIGRATED_KEY, false)) return
        val legacy = settings.getStringOrNull(LEGACY_KEY)
            ?.let { runCatching { json.decodeFromString<List<Stored>>(it) }.getOrNull() }
            .orEmpty()
        // Пустой список — переносить нечего, помечаем готовым. Иначе шлём на сервер, но только
        // если папка доступна: нет сети (folderId == null) — не помечаем, попробуем при следующем
        // входе.
        val migrated = if (legacy.isEmpty()) {
            true
        } else {
            ensureFolderId()?.also { folderId ->
                legacy.forEach { folders.addToBookmark(it.id, folderId) }
            } != null
        }
        if (migrated) settings.putBoolean(MIGRATED_KEY, true)
    }

    /**
     * Единственная точка записи списка — и единственное место, где он дедуплицируется.
     *
     * Страницы `bookmarks/{id}` у kino.pub пересекаются (тайтл приходит и на первой, и на
     * второй), а список уходит в LazyGrid с `key = id` — дубликат роняет экран «Моё» на
     * `IllegalArgumentException: Key … was already used`. Так же лечится содержимое обычных
     * папок (FolderScreenModel) и выдача каталога/поиска.
     */
    private fun updateState(list: List<WatchlistItem>) {
        val unique = list.distinctBy { it.id }
        settings.putString(CACHE_KEY, json.encodeToString(unique.map { it.toStored() }))
        state.value = unique
    }

    /** Дедуп и на чтении: в кэше уже мог осесть список с дублями, записанный до этой правки. */
    private fun loadCache(): List<WatchlistItem> =
        settings.getStringOrNull(CACHE_KEY)
            ?.let { runCatching { json.decodeFromString<List<Stored>>(it) }.getOrNull() }
            ?.map { it.toModel() }
            ?.distinctBy { it.id }
            ?: emptyList()

    @Serializable
    private data class Stored(
        val id: Int,
        val title: String,
        val posterSmall: String,
        val year: Int,
        val durationMinutes: Int,
        // Оценку стали хранить позже: в старом кэше её нет — отсюда значение по умолчанию.
        val rating: String? = null,
    )

    // Оценка в кэше — строкой, как её хранили до появления второго источника: старый кэш читается.
    private fun WatchlistItem.toStored() = Stored(id, title, posterSmall, year, durationMinutes, rating?.toString())
    private fun Stored.toModel() =
        WatchlistItem(id, title, posterSmall, year, durationMinutes, rating?.toDoubleOrNull())

    private companion object {
        const val FOLDER_ID_KEY = "watchlist_folder_id"
        const val CACHE_KEY = "watchlist_cache"
        const val MIGRATED_KEY = "watchlist_migrated"
        const val LEGACY_KEY = "favorite_items"
        const val MAX_PAGES = 10
        const val MARKED = 1
        val json = Json { ignoreUnknownKeys = true }

        /** Ответ `togglewatchlist`: `{"watching": 1}` — тайтл теперь отмечен. */
        fun Map<String, Int>.isMarked(): Boolean = this["watching"] == MARKED
    }
}
