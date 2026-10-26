package com.filmax.feature.collections.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute

class CollectionDetailScreenModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val navigator: Navigator,
) : BaseScreenModel<CollectionDetailState, CollectionDetailSideEffect, CollectionDetailEvent>(
    initialState(savedStateHandle.toRoute<CollectionDetailRoute>()),
) {

    private val collectionId = state.collectionId

    init {
        onFetchData()
    }

    override fun onEvent(event: CollectionDetailEvent) {
        when (event) {
            CollectionDetailEvent.LoadMore -> onLoadMore()
            is CollectionDetailEvent.FocusRow -> onFocusRow(event.row)
            is CollectionDetailEvent.OpenItem -> navigator.open(Destination.Details(event.itemId))
            CollectionDetailEvent.PlayAll -> onPlayAll()
            CollectionDetailEvent.Back -> navigator.back()
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            updateState { it.copy(loading = true, error = null) }
            when (val result = catalog.getCollectionItems(collectionId, page = 1)) {
                is RequestResult.Success -> {
                    val items = result.data.items.distinctBy(Item::id)
                    val cover = coverUrl(result.data.collection)
                    updateState { it.withFirstPage(items, result.data.pagination.hasNextPage, cover) }
                }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    showError(result)
                }
            }
        }
    }

    /** Повтор той же строки — не событие: фокус ходит по ней влево-вправо, шапка от этого не меняется. */
    private fun onFocusRow(row: Int?) {
        screenModelScope { current ->
            if (row == current.focusedRow) return@screenModelScope
            updateState { it.withFocusedRow(row) }
        }
    }

    private fun onPlayAll() {
        val startItemId = state.startItemId ?: return
        navigator.open(Destination.Player(itemId = startItemId))
    }

    /**
     * Следующая страница подборки. Идемпотентна: UI дёргает её на каждом подходе к хвосту сетки,
     * а пока страница в пути или она последняя — повторный вызов ничего не делает.
     */
    private fun onLoadMore() {
        screenModelScope { current ->
            if (current.loading || current.loadingMore || !current.hasNextPage) return@screenModelScope
            val nextPage = current.page + 1
            updateState { it.copy(loadingMore = true) }
            when (val result = catalog.getCollectionItems(collectionId, page = nextPage)) {
                is RequestResult.Success -> {
                    // Дубли гасим по id: LazyGrid падает на неуникальном ключе, а границы
                    // страниц у kino.pub плывут, если подборку правят во время просмотра.
                    val items = (state.items + result.data.items).distinctBy(Item::id)
                    updateState { it.withNextPage(items, nextPage, result.data.pagination.hasNextPage) }
                }

                // Тихий фейл: показанное не рушим, следующий подход к хвосту повторит.
                is RequestResult.Error -> updateState { it.copy(loadingMore = false) }
            }
        }
    }
}

/** Стартовое состояние — из маршрута: заголовок виден до ответа сети. */
private fun initialState(route: CollectionDetailRoute): CollectionDetailState =
    CollectionDetailState(collectionId = route.collectionId, title = route.title)

/**
 * Кадр для шапки подборки: широкий, если kino.pub его прислал, иначе большой постер. Пустая строка —
 * не картинка: kino.pub отдаёт поле, даже когда файла нет.
 */
private fun coverUrl(collection: Collection?): String? {
    val posters = collection?.posters ?: return null
    val cover = posters.wide?.takeIf { it.isNotBlank() } ?: posters.big.ifBlank { posters.medium }
    return cover.takeIf { it.isNotBlank() }
}
