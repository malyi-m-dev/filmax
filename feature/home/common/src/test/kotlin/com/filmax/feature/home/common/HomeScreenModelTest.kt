package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.LastValueCache
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchLaterUseCase
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeFeedbackPromptStore
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Логика Главной на фейковом data-слое: карусель hero, ряды и тематические ряды, превью подборок,
 * «Буду смотреть».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModelTest {

    private val backend = FakeBackend(latency = FakeLatency.None)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `hero carousel goes round in a circle`() {
        val model = homeModel()
        val count = model.states.value.heroes.size
        assertTrue(count > 1, "в фиде должно быть несколько hero, а их $count")

        repeat(count) { model.dispatch(HomeEvent.NextHero) }

        assertEquals(0, model.states.value.heroIndex)
    }

    @Test
    fun `rows follow the design order, theme rows go after fresh and fresh row is not paged`() {
        val model = homeModel()

        assertEquals(
            listOf(HomeRowId.CONTINUE, HomeRowId.TRENDING, HomeRowId.COLLECTIONS, HomeRowId.FOR_YOU, HomeRowId.FRESH) +
                THEME_SOURCES.keys,
            model.states.value.rows.map { it.id },
        )
        assertTrue(model.states.value.titles(HomeRowId.FRESH).paging.endReached)
    }

    @Test
    fun `feed is shown without waiting for theme rows, they arrive afterwards`() {
        val catalog = RecordingHomeCatalog(FakeCatalogRepository(backend), held = THEME_SOURCES.values.toSet())
        val model = homeModel(catalog)

        val feedOnly = model.states.value
        assertFalse(feedOnly.loading)
        assertNotNull(feedOnly.hero)
        assertTrue(feedOnly.titles(HomeRowId.TRENDING).paging.items.isNotEmpty())
        THEME_SOURCES.keys.forEach { id -> assertTrue(feedOnly.titles(id).isEmpty, "ряд $id пришёл вместе с фидом") }

        catalog.release()

        val loaded = model.states.value
        THEME_SOURCES.forEach { (id, source) ->
            val items = loaded.titles(id).paging.items
            assertTrue(items.isNotEmpty(), "ряд $id пуст на мок-данных — на mock-сборке его не будет")
            assertTrue(items.all { it.belongsTo(source) }, "в ряду $id чужие тайтлы: ${items.map { it.title }}")
        }
    }

    @Test
    fun `theme row is loaded once, from page one of its own source`() {
        val catalog = RecordingHomeCatalog(FakeCatalogRepository(backend))
        homeModel(catalog)

        THEME_SOURCES.values.forEach { source ->
            assertEquals(listOf(TitlesCall(source, 1)), catalog.calls.filter { it.source == source })
        }
    }

    @Test
    fun `load more of a theme row asks its own source for the next page`() {
        val catalog = RecordingHomeCatalog(FakeCatalogRepository(backend), endless = true)
        val model = homeModel(catalog)

        THEME_SOURCES.forEach { (id, source) ->
            catalog.calls.clear()

            model.dispatch(HomeEvent.LoadMoreRow(id))

            assertEquals(listOf(TitlesCall(source, 2)), catalog.calls, "догрузка ряда $id")
            assertEquals(2, model.states.value.titles(id).paging.page)
        }
    }

    @Test
    fun `reload retries a failed theme row and keeps the loaded ones`() {
        val concerts = THEME_SOURCES.getValue(HomeRowId.CONCERTS)
        val comedies = THEME_SOURCES.getValue(HomeRowId.COMEDIES)
        val catalog = RecordingHomeCatalog(FakeCatalogRepository(backend), failing = setOf(concerts))
        val model = homeModel(catalog)
        assertTrue(model.states.value.titles(HomeRowId.CONCERTS).isEmpty)
        val loadedComedies = model.states.value.titles(HomeRowId.COMEDIES)

        catalog.failing = emptySet()
        model.dispatch(HomeEvent.Load)

        assertTrue(model.states.value.titles(HomeRowId.CONCERTS).paging.items.isNotEmpty())
        assertEquals(loadedComedies, model.states.value.titles(HomeRowId.COMEDIES))
        assertEquals(1, catalog.calls.count { it.source == comedies }, "загруженный ряд перезапросили")
    }

    @Test
    fun `collection previews carry two posters and an exact count only for one-page collections`() {
        val model = homeModel()
        val row = model.states.value.rows.single { it.id == HomeRowId.COLLECTIONS } as HomeRow.Collections

        assertTrue(row.previews.isNotEmpty())
        row.previews.values.forEach { preview ->
            assertTrue(preview.posters.size <= 2)
            preview.count?.let { count -> assertTrue(count in 1..ONE_PAGE) }
        }
    }

    @Test
    fun `watch later toggle shows up in state`() {
        val model = homeModel()
        val hero = checkNotNull(model.states.value.hero)
        val wasMarked = hero.id in model.states.value.watchLaterIds

        model.dispatch(HomeEvent.ToggleWatchLater(hero))

        assertEquals(!wasMarked, hero.id in model.states.value.watchLaterIds)
    }

    private fun homeModel(catalog: CatalogRepository = FakeCatalogRepository(backend)): HomeScreenModel {
        val watching = FakeWatchingRepository(backend)
        val favorites = FakeFavoritesRepository()
        return HomeScreenModel(
            getHomeFeed = GetHomeFeedUseCase(catalog, watching, LastValueCache()),
            catalog = catalog,
            user = FakeUserRepository(backend),
            favorites = favorites,
            toggleWatchLater = ToggleWatchLaterUseCase(favorites, watching),
            feedbackPrompts = FeedbackPrompts(FakeFeedbackPromptStore(), appVersion = "test", now = { 0L }),
        )
    }

    private fun HomeState.titles(id: HomeRowId): HomeRow.Titles = rows.single { it.id == id } as HomeRow.Titles

    /** Тайтл из выдачи источника: тот же тип и, если источник с жанром, тот же жанр. */
    private fun Item.belongsTo(source: TitleSource): Boolean = when (source) {
        is TitleSource.Catalog ->
            type == source.type && (source.genreId == null || genres.any { it.id == source.genreId })
        is TitleSource.Shortcut -> type == source.type
    }

    private companion object {
        /** Страница каталога фейка — потолок «точного» счёта подборки. */
        const val ONE_PAGE = 20
    }
}
