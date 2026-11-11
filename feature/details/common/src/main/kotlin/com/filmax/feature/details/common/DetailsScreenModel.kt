package com.filmax.feature.details.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.person.CastRepository
import com.filmax.core.domain.source.ContentSourceRepository
import com.filmax.core.domain.source.capabilities
import com.filmax.core.domain.usecase.watching.ToggleWatchlistUseCase
import com.filmax.core.domain.watchlist.WatchlistRepository
import com.filmax.core.domain.watchlist.model.toWatchlistItem
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
    private val watchlist: WatchlistRepository,
    private val cast: CastRepository,
    private val toggleWatchlist: ToggleWatchlistUseCase,
    sources: ContentSourceRepository,
) : BaseScreenModel<DetailsState, DetailsSideEffect, DetailsEvent>(
    DetailsState(foldersAvailable = sources.source.value.capabilities.folders),
) {

    private val route = savedStateHandle.toRoute<DetailsRoute>()

    /**
     * Запросы сезона: фокус на чипе применяется с задержкой ([DetailsEvent.SelectSeason]), стрелка
     * с серии — сразу ([DetailsEvent.SwitchSeason]). Один поток на оба — немедленный запрос
     * вытесняет выжидающий, и поздний чип не перебьёт сезон, выбранный после него. При
     * переполнении теряется старый запрос, а не новый: иначе быстрое пролистывание чипов
     * оставляло на экране сезон, с которого фокус давно ушёл.
     */
    private val seasonRequests = MutableSharedFlow<SeasonRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        onFetchData()
        observeFavoriteState()
        observeSeasonRequests()
    }

    override fun onEvent(event: DetailsEvent) {
        when (event) {
            DetailsEvent.ToggleFav -> toggleFav()
            is DetailsEvent.SelectSeason -> seasonRequests.tryEmit(SeasonRequest(event.number, SEASON_SELECT_DELAY_MS))
            is DetailsEvent.SwitchSeason -> seasonRequests.tryEmit(SeasonRequest(event.number, delayMs = 0))
            DetailsEvent.OpenDescription -> screenModelScope { updateState { it.copy(descriptionOpen = true) } }
            DetailsEvent.CloseDescription -> screenModelScope { updateState { it.copy(descriptionOpen = false) } }
        }
    }

    override fun onFetchData() {
        screenModelScope {
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
                        watchlist.adopt(itemResult.data.toWatchlistItem())
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
        screenModelScope {
            val members = cast.getCast(imdbId)
            if (members.isNotEmpty()) {
                updateState { it.copy(cast = members) }
            }
        }
    }

    private fun observeFavoriteState() {
        screenModelScope {
            watchlist.contains(route.itemId).collect { favorite ->
                updateState { it.copy(isFav = favorite) }
            }
        }
    }

    private fun toggleFav() {
        val item = state.item ?: return
        screenModelScope { toggleWatchlist(item) }
    }

    private fun observeSeasonRequests() {
        screenModelScope {
            seasonRequests.debounce { it.delayMs }.collect { request ->
                updateState { it.copy(selectedSeason = request.number) }
            }
        }
    }
}

/** Пауза перед сменой сезона по фокусу: быстрое пролистывание чипов не меняет сезон на каждом. */
private const val SEASON_SELECT_DELAY_MS = 250L

/** Сезон [number], который применится, если за [delayMs] не придёт другой запрос. */
private data class SeasonRequest(val number: Int, val delayMs: Long)
