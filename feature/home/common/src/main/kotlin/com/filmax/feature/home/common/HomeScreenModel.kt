package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchLaterUseCase
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.initials
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class HomeScreenModel(
    private val getHomeFeed: GetHomeFeedUseCase,
    private val catalog: CatalogRepository,
    private val user: UserRepository,
    private val favorites: FavoritesRepository,
    private val toggleWatchLater: ToggleWatchLaterUseCase,
    private val feedbackPrompts: FeedbackPrompts,
    private val navigator: Navigator,
) : BaseScreenModel<HomeState, HomeSideEffect, HomeEvent>(HomeState()) {

    /** Таймер карусели hero на ТВ: идёт, пока hero в фокусе. */
    private var heroTimer: Job? = null

    init {
        onFetchData()
        fetchUserInitials()
        observeWatchLater()
        askFeedbackAfterUpdate()
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Load -> onFetchData()
            is HomeEvent.LoadMoreRow -> onLoadMoreRow(event.id)
            is HomeEvent.HeroFocused -> onHeroFocused(event.focused)
            is HomeEvent.ToggleWatchLater -> onToggleWatchLater(event.item)
            is HomeEvent.OpenItem -> navigator.open(Destination.Details(event.itemId))
            is HomeEvent.Play -> navigator.open(Destination.Player(event.itemId))
            is HomeEvent.ContinueWatching -> onContinueWatching(event.entry)
            is HomeEvent.OpenCollection -> onOpenCollection(event.collection)
            is HomeEvent.OpenSection -> navigator.open(Destination.Browse(event.section.name))
            HomeEvent.OpenSections -> onSectionsSheet(open = true)
            HomeEvent.CloseSections -> onSectionsSheet(open = false)
            HomeEvent.OpenTv -> navigator.open(Destination.TvPicker())
            HomeEvent.OpenCatalog -> navigator.open(Destination.Catalog)
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            updateState { it.copy(loading = true, error = null) }
            val feed = getHomeFeed()
            // Состав, порядок и заголовки ленты — здесь и только здесь.
            val rows = listOf(
                HomeRow.Continue(feed.continueWatching),
                HomeRow.Titles(HomeRowId.TRENDING, "В тренде", "на этой неделе", RowPaging(feed.trending)),
                HomeRow.Collections(RowPaging(feed.collections)),
                // Заголовок честный: forYou — топ сериалов по рейтингу, персонализации в фиде нет.
                HomeRow.Titles(
                    HomeRowId.FOR_YOU,
                    "Сериалы с высоким рейтингом",
                    "КиноПоиск 8+",
                    RowPaging(feed.forYou),
                ),
                // Новинки — сводка двух источников, листать её из каталога нечем.
                HomeRow.Titles(HomeRowId.FRESH, "Новинки", null, RowPaging(feed.fresh, endReached = true)),
            )
            val hero = feed.heroes.firstOrNull()
            val isEmpty = hero == null && rows.all { it.isEmpty }
            updateState {
                it.copy(
                    loading = false,
                    heroes = feed.heroes,
                    heroIndex = 0,
                    hero = hero,
                    rows = rows,
                    isEmpty = isEmpty,
                    error = feed.error,
                )
            }
            when {
                // Контент из кэша при офлайне (issue #42) — показываем баннер, не модалку.
                feed.fromCache -> showOfflineBanner()
                // Пусто + ошибка — блокирующая модалка.
                !feed.hasContent && feed.error != null -> showError(feed.error)
                // Свежие данные приехали — прячем баннер, если висел.
                else -> dismissOfflineBanner()
            }
            loadCollectionPreviews(feed.collections)
        }
    }

    /**
     * Главная — первый экран после входа на обеих платформах: здесь и узнаём, что приложение
     * обновилось. Окно открывается не чаще, чем разрешает [FeedbackPrompts].
     */
    private fun askFeedbackAfterUpdate() {
        screenModelScope { _ ->
            if (feedbackPrompts.afterAppStart()) navigator.open(Destination.Feedback(FeedbackTrigger.AfterUpdate.name))
        }
    }

    /** Инициалы для аватара в шапке — best-effort, ошибки не мешают ленте. */
    private fun fetchUserInitials() {
        screenModelScope {
            val profile = user.getProfile() as? RequestResult.Success ?: return@screenModelScope
            val initials = profile.data.initials()
            updateState { it.copy(initials = initials) }
        }
    }

    /** Отметки «Буду смотреть» — живой поток: кнопка hero и детали меняют их с разных экранов. */
    private fun observeWatchLater() {
        screenModelScope {
            favorites.favoriteIds.collect { ids -> updateState { it.copy(watchLaterIds = ids) } }
        }
    }

    private fun onToggleWatchLater(item: Item) {
        screenModelScope { _ -> toggleWatchLater(item) }
    }

    /**
     * Карусель листается сама, пока hero в фокусе: зритель смотрит на него, а не на ряды. Ушёл
     * фокус — таймер стоит; вернулся — отсчёт с нуля. Один тайтл не листается.
     */
    private fun onHeroFocused(focused: Boolean) {
        if (focused && heroTimer?.isActive == true) return
        heroTimer?.cancel()
        if (!focused || state.heroes.size < 2) return
        heroTimer = screenModelScope { _ ->
            while (true) {
                delay(HERO_INTERVAL_MS)
                showNextHero()
            }
        }
    }

    private suspend fun showNextHero() {
        val heroes = state.heroes
        if (heroes.size < 2) return
        val index = (state.heroIndex + 1) % heroes.size
        updateState { it.copy(heroIndex = index, hero = heroes[index]) }
    }

    /**
     * Ряд «Продолжить» ведёт сразу в плеер, минуя детали: недосмотренная серия — это сезон и номер
     * видео из истории, позицию внутри трека восстановит плеер.
     */
    private fun onContinueWatching(entry: WatchHistory) {
        val progress = entry.progress
        navigator.open(
            Destination.Player(
                itemId = entry.itemId,
                videoId = progress?.videoId ?: Destination.Player.UNKNOWN,
                season = progress?.season ?: Destination.Player.UNKNOWN,
            ),
        )
    }

    private fun onOpenCollection(collection: Collection) {
        navigator.open(Destination.CollectionDetail(collectionId = collection.id, title = collection.title))
    }

    private fun onSectionsSheet(open: Boolean) {
        screenModelScope { _ -> updateState { it.copy(sectionsOpen = open) } }
    }

    /**
     * Превью подборок: по первой странице каждой — два постера и, если подборка в одну
     * страницу, точное число тайтлов. Грузятся параллельно и приходят в ряд по одной.
     */
    private fun loadCollectionPreviews(collections: List<Collection>) {
        collections.take(COLLECTION_PREVIEWS_LIMIT).forEach { collection ->
            screenModelScope { _ ->
                val result = catalog.getCollectionItems(collection.id) as? RequestResult.Success
                val page = result?.data ?: return@screenModelScope
                val preview = CollectionPreview(
                    posters = page.items.take(PREVIEW_POSTERS).map { it.posters.medium.ifBlank { it.posters.small } },
                    count = page.items.size.takeUnless { page.pagination.hasNextPage },
                )
                addCollectionPreview(collection.id, preview)
            }
        }
    }

    private fun onLoadMoreRow(id: HomeRowId) {
        when (val row = state.rows.firstOrNull { it.id == id }) {
            // История приходит из фида целиком — листать нечего.
            is HomeRow.Continue, null -> Unit
            is HomeRow.Titles -> loadMoreTitles(row)
            is HomeRow.Collections -> loadMoreCollections(row)
        }
    }

    private fun loadMoreTitles(row: HomeRow.Titles) {
        val source = TITLE_SOURCES[row.id] ?: return
        if (!canLoadMore(row.paging)) return
        val nextPage = row.paging.page + 1
        screenModelScope { _ ->
            updateTitlesPaging(row.id, row.paging.copy(loadingMore = true))
            val result = catalog.getItems(source.type, source.sort, nextPage)
            val paging = when (result) {
                is RequestResult.Success ->
                    appended(row.paging, result.data.items, Item::id, result.data.pagination.hasNextPage)
                is RequestResult.Error -> row.paging.copy(loadingMore = false)
            }
            updateTitlesPaging(row.id, paging)
        }
    }

    /**
     * Догрузка «Подборок» отдельно от [loadMoreTitles]: другой источник, и репозиторий отдаёт
     * список без пагинации — конец определяется пустой страницей.
     */
    private fun loadMoreCollections(row: HomeRow.Collections) {
        if (!canLoadMore(row.paging)) return
        val nextPage = row.paging.page + 1
        screenModelScope { _ ->
            updateCollectionsPaging(row.paging.copy(loadingMore = true))
            val result = catalog.getCollections(nextPage)
            val paging = when (result) {
                is RequestResult.Success -> {
                    val page = result.data
                    appended(row.paging, page, Collection::id, hasNextPage = page.isNotEmpty())
                }
                is RequestResult.Error -> row.paging.copy(loadingMore = false)
            }
            updateCollectionsPaging(paging)
        }
    }

    /** Ряд можно листать дальше: не занят, не кончился, не упёрся в потолок и вообще не пуст. */
    private fun canLoadMore(paging: RowPaging<*>): Boolean =
        !paging.loadingMore && !paging.endReached && paging.items.isNotEmpty() && paging.items.size < HOME_ROW_MAX

    /**
     * Приклеивает страницу к ряду: дедуп по [key] (страницы kino.pub пересекаются) и потолок ряда.
     * Пустая страница, отсутствие следующей или упёршийся потолок означают конец.
     */
    private fun <T> appended(
        paging: RowPaging<T>,
        page: List<T>,
        key: (T) -> Int,
        hasNextPage: Boolean,
    ): RowPaging<T> {
        val seen = paging.items.mapTo(HashSet(), key)
        val merged = (paging.items + page.filterNot { key(it) in seen }).take(HOME_ROW_MAX)
        return paging.copy(
            items = merged,
            page = paging.page + 1,
            loadingMore = false,
            endReached = page.isEmpty() || !hasNextPage || merged.size >= HOME_ROW_MAX,
        )
    }

    /** Ряды меняются по одному, из текущего состояния: превью и страницы приезжают параллельно. */
    private suspend fun updateTitlesPaging(id: HomeRowId, paging: RowPaging<Item>) {
        val rows = state.rows.map { row ->
            if (row is HomeRow.Titles && row.id == id) row.copy(paging = paging) else row
        }
        updateState { it.copy(rows = rows) }
    }

    private suspend fun updateCollectionsPaging(paging: RowPaging<Collection>) {
        val rows = state.rows.map { row -> if (row is HomeRow.Collections) row.copy(paging = paging) else row }
        updateState { it.copy(rows = rows) }
    }

    private suspend fun addCollectionPreview(collectionId: Int, preview: CollectionPreview) {
        val rows = state.rows.map { row ->
            if (row is HomeRow.Collections) row.copy(previews = row.previews + (collectionId to preview)) else row
        }
        updateState { it.copy(rows = rows) }
    }
}

/** Карусель hero листается сама раз в восемь секунд, пока hero в фокусе. */
internal const val HERO_INTERVAL_MS = 8_000L

/** Сколько подборок получают превью — столько видно в ряду до первой прокрутки и чуть дальше. */
private const val COLLECTION_PREVIEWS_LIMIT = 6

/** Постеров веером на карточке подборки. */
private const val PREVIEW_POSTERS = 2

/**
 * Потолок карточек в одном ряду: бесконечный ряд на пульте — сотни нажатий вправо, а каждая
 * сотня карточек ещё и держит в памяти постеры. Дальше пусть зовёт Каталог.
 */
private const val HOME_ROW_MAX = 100

/** Чем наполняется ряд тайтлов при догрузке: фид даёт стартовую горстку, дальше — каталог. */
private data class TitleSource(val type: ItemType, val sort: CatalogSort)

private val TITLE_SOURCES = mapOf(
    HomeRowId.TRENDING to TitleSource(ItemType.MOVIE, CatalogSort.VIEWS),
    HomeRowId.FOR_YOU to TitleSource(ItemType.SERIES, CatalogSort.RATING),
)
