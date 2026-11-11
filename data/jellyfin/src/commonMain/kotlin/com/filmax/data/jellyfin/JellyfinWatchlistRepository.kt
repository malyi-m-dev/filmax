package com.filmax.data.jellyfin

import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.watchlist.WatchlistRepository
import com.filmax.core.domain.watchlist.model.WatchlistItem
import com.filmax.core.domain.watchlist.model.toWatchlistItem
import com.filmax.data.jellyfin.remote.ItemsQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * «Буду смотреть» домашнего сервера — его избранное. Отметка живёт на сервере, поэтому список
 * общий для всех устройств и приложений этого аккаунта, включая веб-клиент Jellyfin.
 *
 * Список перечитывается при каждом входе: до входа сервера нет, а после выхода прежний список
 * показывать нельзя. Отметка ставится оптимистично — сердечко реагирует сразу, сервер догоняет.
 */
internal class JellyfinWatchlistRepository(
    private val context: JellyfinContext,
    scope: CoroutineScope,
) : WatchlistRepository {

    private val state = MutableStateFlow<List<WatchlistItem>>(emptyList())

    init {
        scope.launch {
            context.session.signedIn.collectLatest { signedIn ->
                if (signedIn) refresh() else state.value = emptyList()
            }
        }
    }

    override val items: Flow<List<WatchlistItem>> = state.asStateFlow()

    override fun contains(itemId: Int): Flow<Boolean> = state.map { list -> list.any { it.id == itemId } }

    override suspend fun add(item: WatchlistItem) {
        adopt(item)
        context.request { _ -> context.api.setFavorite(context.ids.requireGuid(item.id), favorite = true) }
    }

    override suspend fun remove(itemId: Int) {
        state.update { list -> list.filterNot { it.id == itemId } }
        context.request { _ -> context.api.setFavorite(context.ids.requireGuid(itemId), favorite = false) }
    }

    // Избранным тайтл отметили в вебе Jellyfin после входа: список его просто подхватывает.
    override suspend fun adopt(item: WatchlistItem) {
        state.update { list -> list.filterNot { it.id == item.id } + item }
    }

    private suspend fun refresh() {
        val query = ItemsQuery(
            includeItemTypes = MOVIES_AND_SERIES,
            filters = listOf(IS_FAVORITE),
            sortBy = listOf(SORT_NAME),
            ascending = true,
        )
        context.request { mapper -> context.api.items(query).items.orEmpty().map { mapper.item(it).toWatchlistItem() } }
            .getOrNull()
            ?.let { list -> state.value = list.distinctBy { it.id } }
    }

    private companion object {
        const val IS_FAVORITE = "IsFavorite"
        const val SORT_NAME = "SortName"
    }
}
