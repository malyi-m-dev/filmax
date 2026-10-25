package com.filmax.feature.details.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.usecase.watching.ToggleWatchLaterUseCase
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCastRepository
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeDownloadsRepository
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeWatchingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Логика деталей на фейковом data-слое: фильм и сериал, сезон серии «продолжить», смена сезона
 * фокусом с задержкой и стрелкой сразу, панель описания, «Буду смотреть» и загрузки. Robolectric — ради маршрута:
 * `toRoute` читает аргументы из SavedStateHandle через Bundle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DetailsScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend(latency = FakeLatency.None)
    private val watching = FakeWatchingRepository(backend)
    private val favorites = FakeFavoritesRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `movie opens without series data and with similar titles`() = runTest(dispatcher) {
        val state = loaded(FakeIds.HERO_MOVIE)

        assertFalse(state.loading)
        assertEquals(FakeIds.HERO_MOVIE, state.item?.id)
        assertNull(state.series)
        assertNull(state.selectedSeason)
        assertNull(state.episodes)
        assertEquals("Смотреть", state.play?.compact)
        assertTrue(state.similar.isNotEmpty())
    }

    @Test
    fun `series opens on the season of the episode to continue`() = runTest(dispatcher) {
        val state = loaded(FakeIds.LONG_SERIES)
        val series = assertNotNull(state.series)
        val resume = assertNotNull(series.resume)

        assertEquals(LONG_SERIES_SEASONS, series.seasons.size)
        assertEquals(LONG_SERIES_RESUME_SEASON, state.selectedSeason)
        assertEquals(LONG_SERIES_RESUME_SEASON to LONG_SERIES_RESUME_EPISODE, resume.seasonNumber to resume.number)
        val rows = checkNotNull(state.episodes).rows
        assertTrue(rows.all { it.episode.seasonNumber == LONG_SERIES_RESUME_SEASON })
        assertTrue(rows.any { it.episode == resume && it.resume })
        assertEquals("Продолжить · S4E7", state.play?.compact)
    }

    @Test
    fun `finished season sends continue to the next one`() = runTest(dispatcher) {
        val resume = assertNotNull(loaded(FakeIds.MEDIUM_SERIES).series?.resume)

        assertEquals(MEDIUM_SERIES_RESUME, resume.seasonNumber to resume.number)
    }

    @Test
    fun `season under focus is applied only after the remote stops`() = runTest(dispatcher) {
        val model = model(FakeIds.LONG_SERIES)
        advanceUntilIdle()

        // Пульт быстро пролистывает чипы 5 → 6 → 7: промежуточные сезоны не рисуются.
        listOf(5, 6, 7).forEach { model.dispatch(DetailsEvent.SelectSeason(it)) }
        advanceTimeBy(SEASON_DELAY_MS / 2)
        assertEquals(LONG_SERIES_RESUME_SEASON, model.states.value.selectedSeason)

        advanceUntilIdle()
        assertEquals(7, model.states.value.selectedSeason)
        assertTrue(model.states.value.episodes?.rows.orEmpty().all { it.episode.seasonNumber == 7 })
    }

    @Test
    fun `season picked by an arrow applies at once and a chip still waiting does not override it`() =
        runTest(dispatcher) {
            val model = model(FakeIds.LONG_SERIES)
            advanceUntilIdle()

            // Фокус задел чип шестого сезона, и сразу же стрелка с серии выбрала третий.
            model.dispatch(DetailsEvent.SelectSeason(6))
            runCurrent()
            model.dispatch(DetailsEvent.SwitchSeason(3))
            runCurrent()
            assertEquals(3, model.states.value.selectedSeason)

            advanceUntilIdle()
            assertEquals(3, model.states.value.selectedSeason)
        }

    @Test
    fun `description panel opens and closes`() = runTest(dispatcher) {
        val model = model(FakeIds.LONG_TITLE_MOVIE)
        advanceUntilIdle()

        model.dispatch(DetailsEvent.OpenDescription)
        advanceUntilIdle()
        assertTrue(model.states.value.descriptionOpen)

        model.dispatch(DetailsEvent.CloseDescription)
        advanceUntilIdle()
        assertFalse(model.states.value.descriptionOpen)
    }

    @Test
    fun `title in the server watchlist shows up as watch later on open`() = runTest(dispatcher) {
        val state = loaded(FakeIds.HERO_MOVIE)

        assertEquals(true, state.item?.inWatchlist)
        assertTrue(state.isFav)
    }

    @Test
    fun `watch later toggles locally and on the server`() = runTest(dispatcher) {
        val model = model(FakeIds.LONG_TITLE_MOVIE)
        advanceUntilIdle()
        val initial = model.states.value.isFav

        model.dispatch(DetailsEvent.ToggleFav)
        advanceUntilIdle()
        assertEquals(!initial, model.states.value.isFav)
        // Флаг на «сервере»: детали, открытые заново, видят новое состояние watchlist.
        assertEquals(!initial, loaded(FakeIds.LONG_TITLE_MOVIE).item?.inWatchlist)

        model.dispatch(DetailsEvent.ToggleFav)
        advanceUntilIdle()
        assertEquals(initial, model.states.value.isFav)
    }

    @Test
    fun `download toggles in and out`() = runTest(dispatcher) {
        val model = model(FakeIds.HERO_MOVIE)
        advanceUntilIdle()

        model.dispatch(DetailsEvent.ToggleDownload)
        advanceUntilIdle()
        assertTrue(model.states.value.isDownloaded)

        model.dispatch(DetailsEvent.ToggleDownload)
        advanceUntilIdle()
        assertFalse(model.states.value.isDownloaded)
    }

    @Test
    fun `unknown title ends loading with an error`() = runTest(dispatcher) {
        val state = loaded(UNKNOWN_ID)

        assertFalse(state.loading)
        assertNull(state.item)
        assertNotNull(state.error)
    }

    private fun TestScope.loaded(itemId: Int): DetailsState {
        val model = model(itemId)
        advanceUntilIdle()
        return model.states.value
    }

    private fun model(itemId: Int) = DetailsScreenModel(
        savedStateHandle = SavedStateHandle(mapOf("itemId" to itemId)),
        catalog = FakeCatalogRepository(backend),
        downloads = FakeDownloadsRepository(),
        favorites = favorites,
        cast = FakeCastRepository(backend),
        toggleWatchLater = ToggleWatchLaterUseCase(favorites, watching),
    )

    private companion object {
        /** «Хроники Пограничья»: 7 сезонов, S4E7 начата (см. [FakeIds.LONG_SERIES]). */
        const val LONG_SERIES_SEASONS = 7
        const val LONG_SERIES_RESUME_SEASON = 4
        const val LONG_SERIES_RESUME_EPISODE = 7

        /** «Северный ветер»: сезон 1 и S2E1–E4 досмотрены — дальше S2E5. */
        val MEDIUM_SERIES_RESUME = 2 to 5

        const val SEASON_DELAY_MS = 250L
        const val UNKNOWN_ID = 999_999
    }
}
