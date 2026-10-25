package com.filmax.feature.home.common

import com.filmax.core.domain.common.LastValueCache
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchLaterUseCase
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.core.navigation.Destination
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeFeedbackPromptStore
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeNavigator
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Логика Главной на фейковом data-слое: карусель hero, ряды, превью подборок, «Буду смотреть», переходы. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val navigator = FakeNavigator()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `hero carousel turns while the hero is focused and goes round in a circle`() {
        val model = homeModel()
        val count = model.states.value.heroes.size
        assertTrue(count > 1, "в фиде должно быть несколько hero, а их $count")

        model.dispatch(HomeEvent.HeroFocused(true))
        repeat(count - 1) { advanceHeroTimer() }
        assertEquals(count - 1, model.states.value.heroIndex)
        assertEquals(model.states.value.heroes.last(), model.states.value.hero)

        advanceHeroTimer()
        assertEquals(0, model.states.value.heroIndex)
    }

    @Test
    fun `hero carousel stops when the focus leaves the hero`() {
        val model = homeModel()

        model.dispatch(HomeEvent.HeroFocused(true))
        model.dispatch(HomeEvent.HeroFocused(false))
        advanceHeroTimer()

        assertEquals(0, model.states.value.heroIndex)
    }

    @Test
    fun `rows follow the design order and fresh row is not paged`() {
        val model = homeModel()

        assertEquals(
            listOf(HomeRowId.CONTINUE, HomeRowId.TRENDING, HomeRowId.COLLECTIONS, HomeRowId.FOR_YOU, HomeRowId.FRESH),
            model.states.value.rows.map { it.id },
        )
        val fresh = model.states.value.rows.single { it.id == HomeRowId.FRESH } as HomeRow.Titles
        assertTrue(fresh.paging.endReached)
        assertFalse(model.states.value.isEmpty)
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
        val wasMarked = model.states.value.heroInWatchLater

        model.dispatch(HomeEvent.ToggleWatchLater(hero))

        assertEquals(!wasMarked, model.states.value.heroInWatchLater)
    }

    @Test
    fun `cards lead to details, player, collection and catalog sections`() {
        val model = homeModel()
        val collection = (model.states.value.rows.single { it.id == HomeRowId.COLLECTIONS } as HomeRow.Collections)
            .paging.items.first()

        model.dispatch(HomeEvent.OpenItem(itemId = 7))
        model.dispatch(HomeEvent.Play(itemId = 7))
        model.dispatch(HomeEvent.OpenCollection(collection))
        model.dispatch(HomeEvent.OpenCatalog)

        assertEquals(
            listOf(
                Destination.Details(7),
                Destination.Player(7),
                Destination.CollectionDetail(collection.id, collection.title),
                Destination.Catalog,
            ),
            navigator.opened,
        )
    }

    @Test
    fun `continue watching opens the player on the episode from history`() {
        val model = homeModel()
        val progress = WatchProgress(status = 0, timeSeconds = 100, durationSeconds = 2000, videoId = 5, season = 2)
        val entry = WatchHistory(itemId = 9, title = "Сериал", posterSmall = null, progress = progress)

        model.dispatch(HomeEvent.ContinueWatching(entry))

        assertEquals(listOf(Destination.Player(itemId = 9, videoId = 5, season = 2)), navigator.opened)
    }

    @Test
    fun `sections sheet is state`() {
        val model = homeModel()

        model.dispatch(HomeEvent.OpenSections)
        assertTrue(model.states.value.sectionsOpen)

        model.dispatch(HomeEvent.CloseSections)
        assertFalse(model.states.value.sectionsOpen)
    }

    /** Тик таймера карусели: задача на ровно HERO_INTERVAL_MS исполняется после advanceTimeBy только runCurrent'ом. */
    private fun advanceHeroTimer() {
        scheduler.advanceTimeBy(HERO_INTERVAL_MS)
        scheduler.runCurrent()
    }

    private fun homeModel(): HomeScreenModel {
        val backend = FakeBackend(latency = FakeLatency.None)
        val catalog = FakeCatalogRepository(backend)
        val watching = FakeWatchingRepository(backend)
        val favorites = FakeFavoritesRepository()
        return HomeScreenModel(
            getHomeFeed = GetHomeFeedUseCase(catalog, watching, LastValueCache()),
            catalog = catalog,
            user = FakeUserRepository(backend),
            favorites = favorites,
            toggleWatchLater = ToggleWatchLaterUseCase(favorites, watching),
            feedbackPrompts = FeedbackPrompts(FakeFeedbackPromptStore(), appVersion = "test", now = { 0L }),
            navigator = navigator,
        )
    }

    private companion object {
        /** Страница каталога фейка — потолок «точного» счёта подборки. */
        const val ONE_PAGE = 20
    }
}
