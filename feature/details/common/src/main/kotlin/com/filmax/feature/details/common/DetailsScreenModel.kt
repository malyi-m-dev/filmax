package com.filmax.feature.details.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Item
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
        observeDownloadState()
        observeFavoriteState()
        observeSeasonRequests()
    }

    override fun onEvent(event: DetailsEvent) {
        when (event) {
            DetailsEvent.Back -> sendSideEffect(DetailsSideEffect.Back)
            DetailsEvent.ToggleFav -> onToggleFav()
            DetailsEvent.ToggleDownload -> onToggleDownload()
            DetailsEvent.Play -> onPlay()
            is DetailsEvent.PlayEpisode -> openPlayer(event.season, event.videoId)
            DetailsEvent.PlayOnTv -> onPlayOnTv()
            DetailsEvent.PlayTrailer -> onPlayTrailer()
            DetailsEvent.Share -> onShare()
            is DetailsEvent.OpenItem -> sendSideEffect(DetailsSideEffect.OpenDetails(event.itemId))
            is DetailsEvent.OpenPerson -> onOpenPerson(event)
            is DetailsEvent.SelectSeason -> seasonRequests.tryEmit(SeasonRequest(event.number, SEASON_SELECT_DELAY_MS))
            is DetailsEvent.SwitchSeason -> seasonRequests.tryEmit(SeasonRequest(event.number, delayMs = 0))
            DetailsEvent.OpenDescription -> onDescription(open = true)
            DetailsEvent.CloseDescription -> onDescription(open = false)
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            val itemResult = catalog.getItemDetails(route.itemId)
            val similar = catalog.getSimilarItems(route.itemId).getOrNull().orEmpty()
            if (itemResult is RequestResult.Error) {
                updateState { it.copy(loading = false, error = itemResult.message) }
                showError(itemResult)
                return@screenModelScope
            }
            val item = (itemResult as RequestResult.Success).data
            val series = if (isSeries(item)) calculateSeriesData(item.tracklist) else null
            val loaded = state.loaded(item, similar, series)
            updateState { loaded }
            // Down-sync: если на сервере фильм уже в watchlist — заносим в локальный кэш.
            if (item.inWatchlist) favorites.add(item.toFavoriteItem())
            loadCast(item.imdbId)
        }
    }

    /**
     * Фото актёров грузим отдельным запросом ПОСЛЕ показа тайтла: экран уже виден со строкой имён,
     * а фото «доезжают» без блокировки. Пустой ответ (нет ключа/совпадения) молча оставляет строку.
     */
    private fun loadCast(imdbId: String?) {
        screenModelScope { _ ->
            val members = cast.getCast(imdbId)
            if (members.isEmpty()) return@screenModelScope
            val withCast = state.withCast(members)
            updateState { withCast }
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

    private fun observeSeasonRequests() {
        screenModelScope { _ ->
            seasonRequests.debounce { it.delayMs }.collect { request ->
                val withSeason = state.withSeason(request.number)
                updateState { withSeason }
            }
        }
    }

    private fun onToggleFav() {
        val item = state.item ?: return
        screenModelScope { _ -> toggleWatchLater(item) }
    }

    private fun onToggleDownload() {
        val item = state.item ?: return
        screenModelScope {
            if (state.isDownloaded) {
                downloads.remove(item.id)
            } else {
                downloads.add(downloadedItem(item))
            }
        }
    }

    private fun downloadedItem(item: Item) = DownloadedItem(
        id = item.id,
        title = item.title,
        posterSmall = item.posters.medium.ifBlank { item.posters.small },
        year = item.year,
        durationMinutes = item.duration.averageMinutes?.toInt() ?: 0,
    )

    /** Главная кнопка играет то, что модель выбрала для неё: фильм целиком или нужную серию. */
    private fun onPlay() {
        val play = state.play ?: return
        openPlayer(play.season, play.videoId)
    }

    /** [videoId] — НОМЕР серии, а не её id: тем же числом kino.pub принимает и отдаёт прогресс. */
    private fun openPlayer(season: Int, videoId: Int) {
        sendSideEffect(DetailsSideEffect.OpenPlayer(itemId = route.itemId, videoId = videoId, season = season))
    }

    /** На ТВ — с прогресса kino.pub: телефон эту серию сейчас не играет. */
    private fun onPlayOnTv() {
        val play = state.play ?: return
        sendSideEffect(DetailsSideEffect.OpenTvPicker(route.itemId, play.videoId, play.season))
    }

    private fun onOpenPerson(event: DetailsEvent.OpenPerson) {
        sendSideEffect(DetailsSideEffect.OpenFilmography(event.name, event.isDirector))
    }

    private fun onPlayTrailer() {
        val url = state.trailerUrl ?: return
        val title = state.item?.title ?: return
        sendSideEffect(DetailsSideEffect.OpenTrailer(url = url, title = "Трейлер · $title"))
    }

    private fun onShare() {
        val item = state.item ?: return
        val text = "${item.title} (${item.year}) — смотри в Filmax"
        sendSideEffect(DetailsSideEffect.Share(text))
    }

    private fun onDescription(open: Boolean) {
        screenModelScope { _ -> updateState { it.copy(descriptionOpen = open) } }
    }
}

/** Пауза перед сменой сезона по фокусу: быстрое пролистывание чипов не меняет сезон на каждом. */
private const val SEASON_SELECT_DELAY_MS = 250L

/** Сезон [number], который применится, если за [delayMs] не придёт другой запрос. */
private data class SeasonRequest(val number: Int, val delayMs: Long)
