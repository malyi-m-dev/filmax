package com.filmax.feature.home.common

import com.filmax.core.domain.common.LastValueCache
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchLaterUseCase
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Логика Главной на фейковом data-слое: карусель hero, ряды, превью подборок, «Буду смотреть». */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModelTest {

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
    fun `rows follow the design order and fresh row is not paged`() {
        val model = homeModel()

        assertEquals(
            listOf(HomeRowId.CONTINUE, HomeRowId.TRENDING, HomeRowId.COLLECTIONS, HomeRowId.FOR_YOU, HomeRowId.FRESH),
            model.states.value.rows.map { it.id },
        )
        val fresh = model.states.value.rows.single { it.id == HomeRowId.FRESH } as HomeRow.Titles
        assertTrue(fresh.paging.endReached)
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

    private fun homeModel(): HomeScreenModel {
        val backend = FakeBackend(latency = FakeLatency.None)
        val catalog = FakeCatalogRepository(backend)
        val watching = FakeWatchingRepository(backend)
        val favorites = InMemoryFavorites()
        return HomeScreenModel(
            getHomeFeed = GetHomeFeedUseCase(catalog, watching, LastValueCache()),
            catalog = catalog,
            user = FakeUserRepository(backend),
            favorites = favorites,
            toggleWatchLater = ToggleWatchLaterUseCase(favorites, watching),
        )
    }

    private companion object {
        /** Страница каталога фейка — потолок «точного» счёта подборки. */
        const val ONE_PAGE = 20
    }
}

/** Локальное «Буду смотреть» в памяти — вместо хранилища приложения. */
private class InMemoryFavorites : FavoritesRepository {
    private val items = MutableStateFlow<List<FavoriteItem>>(emptyList())

    override val favorites: Flow<List<FavoriteItem>> = items
    override val favoriteIds: Flow<Set<Int>> = items.map { list -> list.mapTo(HashSet()) { it.id } }

    override fun isFavorite(id: Int): Flow<Boolean> = items.map { list -> list.any { it.id == id } }

    override suspend fun toggle(item: FavoriteItem): Boolean {
        val present = items.value.any { it.id == item.id }
        items.value = if (present) items.value.filterNot { it.id == item.id } else items.value + item
        return !present
    }

    override suspend fun add(item: FavoriteItem) {
        if (items.value.none { it.id == item.id }) items.value = items.value + item
    }

    override suspend fun remove(id: Int) {
        items.value = items.value.filterNot { it.id == id }
    }
}
