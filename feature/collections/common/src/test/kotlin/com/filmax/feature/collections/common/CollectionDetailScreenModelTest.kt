package com.filmax.feature.collections.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
 * Подборка на фейковом data-слое: постраничная загрузка «Зрители рекомендуют» (45 тайтлов, три
 * страницы), число тайтлов только точное, обложка шапки и сворачивание шапки по строке с фокусом.
 * Robolectric — ради маршрута: `toRoute` читает аргументы из SavedStateHandle через Bundle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectionDetailScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend(latency = FakeLatency.None)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `large collection opens on its first page without a guessed count`() = runTest(dispatcher) {
        val state = loaded(FakeIds.LARGE_COLLECTION).states.value

        assertFalse(state.loading)
        assertEquals(PAGE_SIZE, state.items.size)
        assertTrue(state.hasNextPage)
        assertNull(state.titlesCount, "пока есть следующая страница, числа на экране быть не должно")
        assertEquals(state.items.first().id, state.startItemId)
        assertNotNull(state.cover, "у подборки фикстур есть обложка — шапка без кадра")
    }

    @Test
    fun `tail of the grid loads every page and then the count is exact`() = runTest(dispatcher) {
        val model = loaded(FakeIds.LARGE_COLLECTION)

        repeat(PAGES_AFTER_FIRST) {
            model.dispatch(CollectionDetailEvent.LoadMore)
            advanceUntilIdle()
        }
        // Лишний подход к хвосту, когда страниц больше нет, ничего не меняет.
        model.dispatch(CollectionDetailEvent.LoadMore)
        advanceUntilIdle()

        val state = model.states.value
        assertEquals(LARGE_COLLECTION_SIZE, state.items.size)
        assertEquals(state.items.size, state.items.distinctBy { it.id }.size)
        assertFalse(state.hasNextPage)
        assertEquals(LARGE_COLLECTION_SIZE, state.titlesCount)
    }

    @Test
    fun `single page collection knows its count right away`() = runTest(dispatcher) {
        val state = loaded(SMALL_COLLECTION).states.value

        assertEquals(SMALL_COLLECTION_SIZE, state.titlesCount)
    }

    @Test
    fun `header collapses below the first row and expands on the first row or the header`() = runTest(dispatcher) {
        val model = loaded(FakeIds.LARGE_COLLECTION)
        assertFalse(model.states.value.headerCollapsed, "свежий экран открывается с раскрытой шапкой")

        val collapsedByRow = listOf(0, 1, 3, 0, 2, null).map { row ->
            model.dispatch(CollectionDetailEvent.FocusRow(row))
            advanceUntilIdle()
            model.states.value.headerCollapsed
        }

        assertEquals(listOf(false, true, true, false, true, false), collapsedByRow)
    }

    @Test
    fun `collapse rule is decided by the focused row only`() {
        assertFalse(isHeaderCollapsed(null))
        assertFalse(isHeaderCollapsed(0))
        assertTrue(isHeaderCollapsed(1))
        assertTrue(isHeaderCollapsed(LAST_ROW_OF_LARGE_COLLECTION))
    }

    private fun TestScope.loaded(collectionId: Int): CollectionDetailScreenModel {
        val model = CollectionDetailScreenModel(
            savedStateHandle = SavedStateHandle(mapOf("collectionId" to collectionId, "title" to "Подборка")),
            catalog = FakeCatalogRepository(backend),
        )
        advanceUntilIdle()
        return model
    }

    private companion object {
        /** Страница фейкового каталога — как у `api/v1/items`. */
        const val PAGE_SIZE = 20
        const val LARGE_COLLECTION_SIZE = 45
        const val PAGES_AFTER_FIRST = 2
        const val LAST_ROW_OF_LARGE_COLLECTION = 7

        /** «Сериалы на один вечер»: шесть тайтлов, одна страница. */
        const val SMALL_COLLECTION = 102
        const val SMALL_COLLECTION_SIZE = 6
    }
}
