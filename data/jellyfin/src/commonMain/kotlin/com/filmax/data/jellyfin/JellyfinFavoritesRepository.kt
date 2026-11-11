package com.filmax.data.jellyfin

import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.favorites.model.toFavoriteItem
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
internal class JellyfinFavoritesRepository(
    private val context: JellyfinContext,
    scope: CoroutineScope,
) : FavoritesRepository {

    private val state = MutableStateFlow<List<FavoriteItem>>(emptyList())

    init {
        scope.launch {
            context.session.signedIn.collectLatest { signedIn ->
                if (signedIn) refresh() else state.value = emptyList()
            }
        }
    }

    override val favorites: Flow<List<FavoriteItem>> = state.asStateFlow()

    override val favoriteIds: Flow<Set<Int>> = state.map { list -> list.map { it.id }.toSet() }

    override fun isFavorite(id: Int): Flow<Boolean> = state.map { list -> list.any { it.id == id } }

    override suspend fun toggle(item: FavoriteItem): Boolean {
        val added = state.value.none { it.id == item.id }
        if (added) add(item) else remove(item.id)
        return added
    }

    override suspend fun add(item: FavoriteItem) {
        state.update { list -> list.filterNot { it.id == item.id } + item }
        context.request { _ -> context.api.setFavorite(context.ids.requireGuid(item.id), favorite = true) }
    }

    override suspend fun remove(id: Int) {
        state.update { list -> list.filterNot { it.id == id } }
        context.request { _ -> context.api.setFavorite(context.ids.requireGuid(id), favorite = false) }
    }

    private suspend fun refresh() {
        val query = ItemsQuery(
            includeItemTypes = MOVIES_AND_SERIES,
            filters = listOf(IS_FAVORITE),
            sortBy = listOf(SORT_NAME),
            ascending = true,
        )
        context.request { mapper -> context.api.items(query).items.orEmpty().map { mapper.item(it).toFavoriteItem() } }
            .getOrNull()
            ?.let { list -> state.value = list.distinctBy { it.id } }
    }

    private companion object {
        const val IS_FAVORITE = "IsFavorite"
        const val SORT_NAME = "SortName"
    }
}
