package com.filmax.feature.search.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.NavCommand
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeNavigator
import com.filmax.data.fake.FakeSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Каталог и поиск на фейковом data-слое: витрина по чипам, догрузка и счётчик, поиск с лучшим
 * совпадением, экранная клавиатура ТВ, панели сортировки и фильтров, недавние запросы.
 *
 * Фикстуры: 29 фильмов, 18 сериалов, 5 документальных — по 20 на страницу, как у kino.pub. ТВ-шоу,
 * концерты и документальные фильмы в «Все» не входят — у них свои разделы.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend(latency = FakeLatency.None)
    private val catalog = RecordingCatalog(FakeCatalogRepository(backend))
    private val navigator = FakeNavigator()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `catalog opens with all types interleaved, video genres and an estimated count`() = runTest(dispatcher) {
        val state = catalogModel().states.value

        assertEquals(FIRST_PAGE_OF_ALL, state.catalogItems.size)
        assertEquals(
            listOf(ItemType.MOVIE, ItemType.SERIES, ItemType.DOCUMENTARY),
            state.catalogItems.take(BROWSE_TYPES).map { it.type },
        )
        assertTrue(state.genres.isNotEmpty())
        assertTrue(state.genres.none { it.title == "Blues" }, "музыкальные жанры просочились в чипы")
        assertTrue(state.countries.isNotEmpty())
        assertEquals(CatalogCount(ESTIMATED_ALL, exact = false), state.catalogCount)
        assertFalse(state.catalogEndReached)
    }

    @Test
    fun `type chip narrows the catalog and a one-page type is counted exactly`() = runTest(dispatcher) {
        val model = catalogModel()

        model.dispatch(SearchEvent.FilterChange(ItemType.SERIES))
        advanceUntilIdle()

        val state = model.states.value
        assertTrue(state.catalogItems.all { it.type == ItemType.SERIES })
        assertEquals(CatalogCount(SERIES_COUNT, exact = true), state.catalogCount)
        assertTrue(state.catalogEndReached)
    }

    @Test
    fun `tv shows section from the home sheet shows only tv shows`() = runTest(dispatcher) {
        val model = SearchScreenModel(
            search = FakeSearchRepository(backend),
            catalog = catalog,
            navigator = navigator,
            savedStateHandle = SavedStateHandle(mapOf("section" to CatalogSection.TV_SHOWS.name)),
        )
        model.dispatch(SearchEvent.LoadCatalog)
        advanceUntilIdle()

        val state = model.states.value
        assertTrue(state.catalogItems.isNotEmpty())
        assertTrue(state.catalogItems.all { it.type == ItemType.TV_SHOW })
        assertEquals("ТВ-шоу", state.title)
        assertEquals(CatalogSection.TV_SHOWS, state.section)
    }

    @Test
    fun `genre chip keeps only titles of that genre`() = runTest(dispatcher) {
        val model = catalogModel()

        model.dispatch(SearchEvent.GenreChange(SCI_FI_GENRE))
        advanceUntilIdle()

        val items = model.states.value.catalogItems
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { item -> item.genres.any { it.id == SCI_FI_GENRE } })
    }

    @Test
    fun `load more appends the rest and makes the count exact`() = runTest(dispatcher) {
        val model = catalogModel()

        model.dispatch(SearchEvent.LoadMoreCatalog)
        advanceUntilIdle()

        val state = model.states.value
        assertEquals(ALL_TITLES, state.catalogItems.size)
        assertEquals(ALL_TITLES, state.catalogItems.map { it.id }.distinct().size)
        assertEquals(CatalogCount(ALL_TITLES, exact = true), state.catalogCount)
        assertTrue(state.catalogEndReached)
        assertFalse(state.catalogLoadingMore)
    }

    @Test
    fun `chips switched quickly show the last choice even if it answered first`() = runTest(dispatcher) {
        val slowSeries = RecordingCatalog(FakeCatalogRepository(backend)) { type ->
            if (type == ItemType.SERIES) SLOW_ANSWER_MS else 0L
        }
        val model = catalogModel(slowSeries)

        model.dispatch(SearchEvent.FilterChange(ItemType.SERIES))
        model.dispatch(SearchEvent.FilterChange(ItemType.MOVIE))
        advanceUntilIdle()

        val state = model.states.value
        assertEquals(ItemType.MOVIE, state.filter)
        assertTrue(state.catalogItems.all { it.type == ItemType.MOVIE }, "в сетку попали сериалы")
        assertFalse(state.loading)
    }

    @Test
    fun `catalog that failed to load shows the error and retry brings it back`() = runTest(dispatcher) {
        catalog.offline = true
        val model = catalogModel()
        assertEquals(RecordingCatalog.OFFLINE_MESSAGE, model.states.value.error)
        assertTrue(model.states.value.catalogItems.isEmpty())
        assertNull(model.states.value.catalogCount)

        catalog.offline = false
        model.dispatch(SearchEvent.RetryCatalog)
        advanceUntilIdle()

        assertNull(model.states.value.error)
        assertEquals(FIRST_PAGE_OF_ALL, model.states.value.catalogItems.size)
    }

    @Test
    fun `typed query searches with a debounce and puts the best match first`() = runTest(dispatcher) {
        val model = catalogModel()

        model.dispatch(SearchEvent.QueryChange("хрони"))
        advanceUntilIdle()

        val state = model.states.value
        assertEquals(FakeIds.LONG_SERIES, state.bestMatch?.id)
        assertEquals(state.results.drop(1), state.otherResults)
        assertEquals(state.results, state.visibleItems)
    }

    @Test
    fun `one letter is not a query and has no best match`() = runTest(dispatcher) {
        val model = catalogModel()

        model.dispatch(SearchEvent.QueryChange("х"))
        advanceUntilIdle()

        assertNull(model.states.value.bestMatch)
        assertTrue(model.states.value.otherResults.isEmpty())
    }

    @Test
    fun `keyboard types, skips a leading space, deletes and switches layout`() = runTest(dispatcher) {
        val model = catalogModel()
        model.dispatch(SearchEvent.OpenSearch())

        listOf(SPACE_KEY, "х", "р", SPACE_KEY, SPACE_KEY, "о").forEach { model.dispatch(SearchEvent.TypeKey(it)) }
        advanceUntilIdle()
        assertEquals("хр о", model.states.value.query)

        model.dispatch(SearchEvent.DeleteLast)
        model.dispatch(SearchEvent.DeleteLast)
        model.dispatch(SearchEvent.SwitchLayout(KeyboardLayout.EN))
        advanceUntilIdle()

        assertEquals("хр", model.states.value.query)
        assertEquals(KeyboardLayout.EN, model.states.value.keyboardLayout)
        assertTrue(model.states.value.searchOpen)
    }

    @Test
    fun `closing the search returns the same catalog without reloading it`() = runTest(dispatcher) {
        val model = catalogModel()
        model.dispatch(SearchEvent.LoadMoreCatalog)
        advanceUntilIdle()
        val shown = model.states.value.catalogItems
        val requestsBefore = catalog.requests.size

        model.dispatch(SearchEvent.OpenSearch())
        "хрони".forEach { model.dispatch(SearchEvent.TypeKey(it.toString())) }
        advanceUntilIdle()
        model.dispatch(SearchEvent.CloseSearch)
        advanceUntilIdle()

        val state = model.states.value
        assertFalse(state.searchOpen)
        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertEquals(shown, state.catalogItems)
        assertEquals(requestsBefore, catalog.requests.size, "витрина перезапросилась")
    }

    @Test
    fun `tv search ignores catalog chips the viewer cannot see`() = runTest(dispatcher) {
        val model = catalogModel()
        model.dispatch(SearchEvent.FilterChange(ItemType.MOVIE))
        advanceUntilIdle()

        model.dispatch(SearchEvent.OpenSearch("хрони"))
        advanceUntilIdle()

        assertEquals(FakeIds.LONG_SERIES, model.states.value.bestMatch?.id)
    }

    @Test
    fun `filters are edited as a draft and change the catalog only on apply`() = runTest(dispatcher) {
        val model = catalogModel()
        val before = model.states.value.catalogItems
        val only4k = CatalogFilters(only4k = true)

        model.dispatch(SearchEvent.OpenSheet(CatalogSheet.FILTERS))
        model.dispatch(SearchEvent.EditFilters(only4k))
        advanceUntilIdle()
        assertTrue(model.states.value.sheetOpen)
        assertEquals(only4k, model.states.value.filtersDraft)
        assertEquals(CatalogFilters(), model.states.value.filters)
        assertEquals(before, model.states.value.catalogItems)

        model.dispatch(SearchEvent.ApplyDraftFilters)
        advanceUntilIdle()

        val state = model.states.value
        assertFalse(state.sheetOpen)
        assertEquals(CatalogSheet.FILTERS, state.sheet, "панель забыла, какой была, — фокус не вернётся на чип")
        assertEquals(only4k, state.filters)
        assertTrue(state.catalogItems.size < before.size)
    }

    @Test
    fun `reset clears the draft, and reopening starts from the applied filters`() = runTest(dispatcher) {
        val model = catalogModel()
        val only4k = CatalogFilters(only4k = true)
        model.dispatch(SearchEvent.OpenSheet(CatalogSheet.FILTERS))
        model.dispatch(SearchEvent.EditFilters(only4k))
        model.dispatch(SearchEvent.ApplyDraftFilters)
        advanceUntilIdle()

        model.dispatch(SearchEvent.OpenSheet(CatalogSheet.FILTERS))
        advanceUntilIdle()
        assertEquals(only4k, model.states.value.filtersDraft)

        model.dispatch(SearchEvent.ResetDraftFilters)
        advanceUntilIdle()
        assertEquals(CatalogFilters(), model.states.value.filtersDraft)
        assertEquals(only4k, model.states.value.filters, "сброс черновика применился без «Применить»")

        model.dispatch(SearchEvent.CloseSheet)
        model.dispatch(SearchEvent.OpenSheet(CatalogSheet.FILTERS))
        advanceUntilIdle()
        assertEquals(only4k, model.states.value.filtersDraft, "закрытая без «Применить» панель оставила черновик")
    }

    @Test
    fun `picking a sort closes the sort sheet and reloads the catalog`() = runTest(dispatcher) {
        val model = catalogModel()
        model.dispatch(SearchEvent.OpenSheet(CatalogSheet.SORT))
        advanceUntilIdle()
        val requestsBefore = catalog.requests.size

        val byYear = model.states.value.sort.copy(field = CatalogSort.YEAR)
        model.dispatch(SearchEvent.SortChange(byYear))
        advanceUntilIdle()

        val state = model.states.value
        assertFalse(state.sheetOpen)
        assertEquals(byYear, state.sort)
        assertTrue(catalog.requests.size > requestsBefore)
        val years = state.catalogItems.map { it.year }
        assertEquals(years.sortedDescending(), years)
    }

    @Test
    fun `recent queries remember submitted and opened queries, not every typed prefix`() = runTest(dispatcher) {
        val model = catalogModel()
        model.dispatch(SearchEvent.OpenSearch())
        "хрони".forEach { model.dispatch(SearchEvent.TypeKey(it.toString())) }
        advanceUntilIdle()
        assertTrue(model.states.value.recentQueries.isEmpty(), "набранный запрос попал в недавние")

        model.dispatch(SearchEvent.OpenResult(FakeIds.LONG_SERIES))
        model.dispatch(SearchEvent.SubmitQuery("северный"))
        advanceUntilIdle()

        assertEquals(listOf("северный", "хрони"), model.states.value.recentQueries)
    }

    @Test
    fun `cards and results lead to details, back leaves the section`() = runTest(dispatcher) {
        val model = catalogModel()

        model.dispatch(SearchEvent.OpenItem(itemId = 7))
        model.dispatch(SearchEvent.OpenResult(itemId = 9))
        model.dispatch(SearchEvent.Back)
        advanceUntilIdle()

        assertEquals(listOf(Destination.Details(7), Destination.Details(9)), navigator.opened)
        assertEquals(NavCommand.Back, navigator.sent.last())
    }

    /** Модель с витриной: экран каталога уже заявил о себе ([SearchEvent.LoadCatalog]). */
    private fun TestScope.catalogModel(repository: RecordingCatalog = catalog): SearchScreenModel {
        val model = SearchScreenModel(FakeSearchRepository(backend), repository, navigator)
        model.dispatch(SearchEvent.LoadCatalog)
        advanceUntilIdle()
        return model
    }

    private companion object {
        /** Первая страница «Все»: 20 фильмов + 18 сериалов + 5 документальных. */
        const val FIRST_PAGE_OF_ALL = 43

        /** Оценка «Все» по первым страницам: фильмов две страницы по 20, остальные — как есть. */
        const val ESTIMATED_ALL = 63
        const val ALL_TITLES = 52
        const val SERIES_COUNT = 18
        const val BROWSE_TYPES = 3

        /** «Фантастика» в фикстурах. */
        const val SCI_FI_GENRE = 4
        const val SLOW_ANSWER_MS = 1_000L
    }
}
