package com.filmax.feature.details.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.downloads.DownloadsRepository
import com.filmax.core.domain.downloads.model.DownloadedItem
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.toFavoriteItem
import com.filmax.core.domain.person.CastRepository
import com.filmax.core.domain.usecase.watching.ToggleWatchLaterUseCase
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.details.common.navigation.DetailsRoute
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
class DetailsScreenModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val downloads: DownloadsRepository,
    private val favorites: FavoritesRepository,
    private val cast: CastRepository,
    private val toggleWatchLater: ToggleWatchLaterUseCase,
) : BaseScreenModel<DetailsState, DetailsSideEffect, DetailsEvent>(DetailsState()) {

    private val route = savedStateHandle.toRoute<DetailsRoute>()

    /**
     * Сезоны под фокусом пульта — применяются с задержкой, см. [DetailsEvent.SelectSeason]. При
     * переполнении теряется старый запрос, а не новый: иначе быстрое пролистывание чипов
     * оставляло на экране сезон, с которого фокус давно ушёл.
     */
    private val seasonRequests = MutableSharedFlow<Int>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        onFetchData()
        observeDownloadState()
        observeFavoriteState()
        observeSeasonRequests()
    }

    override fun dispatch(event: DetailsEvent) {
        when (event) {
            DetailsEvent.ToggleFav -> toggleFav()
            DetailsEvent.ToggleDownload -> toggleDownload()
            is DetailsEvent.SelectSeason -> seasonRequests.tryEmit(event.number)
            DetailsEvent.OpenDescription -> screenModelScope { _ -> updateState { it.copy(descriptionOpen = true) } }
            DetailsEvent.CloseDescription -> screenModelScope { _ -> updateState { it.copy(descriptionOpen = false) } }
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            val itemResult = catalog.getItemDetails(route.itemId)
            val similar = catalog.getSimilarItems(route.itemId).getOrNull().orEmpty()
            when (itemResult) {
                is RequestResult.Success -> {
                    val series = itemResult.data.takeIf { it.isSeries() }?.let { calculateSeriesData(it.tracklist) }
                    updateState {
                        it.copy(
                            loading = false,
                            item = itemResult.data,
                            similar = similar,
                            series = series,
                            // Сериал открывается на сезоне серии «продолжить».
                            selectedSeason = series?.seasons?.getOrNull(series.resumeSeasonIndex)?.first,
                        )
                    }
                    // Down-sync: если на сервере фильм уже в watchlist — заносим в локальный кэш.
                    if (itemResult.data.inWatchlist) {
                        favorites.add(itemResult.data.toFavoriteItem())
                    }
                    loadCast(itemResult.data.imdbId)
                }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = itemResult.message) }
                    showError(itemResult)
                }
            }
        }
    }

    /**
     * Фото актёров грузим отдельным запросом ПОСЛЕ показа тайтла: экран уже виден со строкой имён,
     * а фото «доезжают» без блокировки. Пустой ответ (нет ключа/совпадения) молча оставляет строку.
     */
    private fun loadCast(imdbId: String?) {
        screenModelScope { _ ->
            val members = cast.getCast(imdbId)
            if (members.isNotEmpty()) {
                updateState { it.copy(cast = members) }
            }
        }
    }

    private fun observeDownloadState() {
        screenModelScope {
            downloads.isDownloaded(route.itemId).collect { downloaded ->
                updateState { it.copy(isDownloaded = downloaded) }
            }
        }
    }

    private fun observeFavoriteState() {
        screenModelScope {
            favorites.isFavorite(route.itemId).collect { favorite ->
                updateState { it.copy(isFav = favorite) }
            }
        }
    }

    private fun toggleFav() {
        val item = state.item ?: return
        screenModelScope { _ -> toggleWatchLater(item) }
    }

    private fun observeSeasonRequests() {
        screenModelScope { _ ->
            seasonRequests.debounce(SEASON_SELECT_DELAY_MS).collect { number ->
                updateState { it.copy(selectedSeason = number) }
            }
        }
    }

    private fun toggleDownload() {
        val item = state.item ?: return
        screenModelScope {
            if (state.isDownloaded) {
                downloads.remove(item.id)
            } else {
                downloads.add(
                    DownloadedItem(
                        id = item.id,
                        title = item.title,
                        posterSmall = item.posters.medium.ifBlank { item.posters.small },
                        year = item.year,
                        durationMinutes = item.duration.averageMinutes?.toInt() ?: 0,
                    ),
                )
            }
        }
    }
}

/** Пауза перед сменой сезона по фокусу: быстрое пролистывание чипов не меняет сезон на каждом. */
private const val SEASON_SELECT_DELAY_MS = 250L
