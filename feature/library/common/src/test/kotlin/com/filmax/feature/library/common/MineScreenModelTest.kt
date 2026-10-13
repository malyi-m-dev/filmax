package com.filmax.feature.library.common

import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.domain.usecase.watching.RemoveFromWatchLaterUseCase
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeFolderAppearanceRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * «Моё» на фейковом бэкенде с фиксированным «сейчас» (14 октября, полдень по Москве): сегменты и
 * счётчики, «Продолжить», история по дням, сортировки, удаление долгим «ОК», очистка истории и
 * создание папки с цветом. Фейк и модель смотрят на одни и те же часы, поэтому «сегодня» и
 * «вчера» в истории не зависят от того, когда запущен тест.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MineScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-14T09:00:00Z"), ZoneId.of("Europe/Moscow"))
    private val backend = FakeBackend(latency = FakeLatency.None, clock = { clock.millis() })
    private val watching = FakeWatchingRepository(backend)
    private val user = FakeUserRepository(backend)
    private val favorites = FakeFavoritesRepository()
    private val appearance = FakeFolderAppearanceRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `segments count watchlist titles and user folders without the system one`() = runTest(dispatcher) {
        favorites.add(favorite(1012, "Б", rating = "7.1"))
        favorites.add(favorite(2007, "А", rating = "8.4"))

        val state = loaded()

        assertFalse(state.loading)
        assertEquals(MineSegment.CONTINUE, state.segment)
        assertEquals(2, state.countOf(MineSegment.WATCHLIST))
        // «Буду смотреть» — системная папка: у неё свой сегмент, среди папок её нет.
        assertEquals(USER_FOLDERS, state.countOf(MineSegment.FOLDERS))
        assertTrue(state.folders.none { it.folder.id == FakeIds.WATCHLIST_FOLDER })
        assertNull(state.countOf(MineSegment.CONTINUE))
        assertNull(state.countOf(MineSegment.HISTORY))
    }

    @Test
    fun `segment follows the focused tab`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        model.dispatch(MineEvent.SelectSegment(MineSegment.HISTORY))
        advanceUntilIdle()

        assertEquals(MineSegment.HISTORY, model.states.value.segment)
    }

    @Test
    fun `continue keeps only started and unfinished titles`() = runTest(dispatcher) {
        val ids = loaded().continueWatching.map { it.itemId }

        assertTrue(FakeIds.LONG_SERIES in ids)
        assertTrue(FakeIds.MOVIE_IN_PROGRESS in ids)
        assertFalse(FakeIds.WATCHED_MOVIE in ids, "досмотренный фильм попал в «Продолжить»")
        assertFalse(FakeIds.MEDIUM_SERIES in ids, "сериал с досмотренной последней серией попал в «Продолжить»")
    }

    @Test
    fun `history is grouped by days, fresh days first`() = runTest(dispatcher) {
        val days = loaded().history

        assertEquals(listOf("Сегодня", "Вчера", "11 октября", "5 октября"), days.map { it.label })
        assertEquals(
            listOf(FakeIds.LONG_SERIES, FakeIds.MOVIE_IN_PROGRESS, FakeIds.SHORT_SERIES),
            days.first().entries.map { it.entry.itemId },
        )
        // Просмотр 20 минут назад от полудня — 11:40 по часам устройства.
        assertEquals("11:40", days.first().entries.first().time)
    }

    @Test
    fun `watchlist sort cycles from newest to rating to title`() = runTest(dispatcher) {
        favorites.add(favorite(1, "Яблоко", rating = "6.0"))
        favorites.add(favorite(2, "Арбуз", rating = null))
        favorites.add(favorite(3, "Вишня", rating = "8.5"))
        val model = model()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), model.states.value.watchlist.map { it.id })

        model.dispatch(MineEvent.CycleWatchlistSort)
        advanceUntilIdle()
        assertEquals(WatchlistSort.RATING, model.states.value.watchlistSort)
        assertEquals(listOf(3, 1, 2), model.states.value.watchlist.map { it.id })

        model.dispatch(MineEvent.CycleWatchlistSort)
        advanceUntilIdle()
        assertEquals(listOf(2, 3, 1), model.states.value.watchlist.map { it.id })
    }

    @Test
    fun `folder sort switches between recent change and title`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        assertEquals(listOf(2, FakeIds.LARGE_FOLDER, FakeIds.EMPTY_FOLDER), model.folderIds())

        model.dispatch(MineEvent.CycleFolderSort)
        advanceUntilIdle()

        // «Новогоднее», «Отложено на потом», «Пересмотреть с семьёй».
        assertEquals(listOf(FakeIds.EMPTY_FOLDER, FakeIds.LARGE_FOLDER, 2), model.folderIds())
    }

    @Test
    fun `folder cards carry posters and when they changed`() = runTest(dispatcher) {
        val folders = loaded().folders.associateBy { it.folder.id }

        assertEquals(3, folders.getValue(FakeIds.LARGE_FOLDER).posters.size)
        assertTrue(folders.getValue(FakeIds.EMPTY_FOLDER).posters.isEmpty())
        assertEquals("изменена 2 октября", folders.getValue(2).changed)
    }

    @Test
    fun `long press on a watchlist poster removes it`() = runTest(dispatcher) {
        favorites.add(favorite(1012, "Первый"))
        favorites.add(favorite(2007, "Второй"))
        val model = model()
        advanceUntilIdle()

        model.dispatch(MineEvent.RemoveFromWatchlist(1012))
        advanceUntilIdle()

        assertEquals(listOf(2007), model.states.value.watchlist.map { it.id })
        assertEquals(listOf(2007), favorites.favorites.first().map { it.id })
    }

    @Test
    fun `long press on continue forgets the title on the server`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        model.dispatch(MineEvent.RemoveFromContinue(FakeIds.MOVIE_IN_PROGRESS))
        advanceUntilIdle()

        val state = model.states.value
        assertTrue(state.continueWatching.none { it.itemId == FakeIds.MOVIE_IN_PROGRESS })
        assertTrue(state.history.flatMap { it.entries }.none { it.entry.itemId == FakeIds.MOVIE_IN_PROGRESS })
        assertTrue(watching.getHistory().getOrNull().orEmpty().none { it.itemId == FakeIds.MOVIE_IN_PROGRESS })
    }

    @Test
    fun `clearing history asks first and then empties it everywhere`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        model.dispatch(MineEvent.AskClearHistory)
        advanceUntilIdle()
        assertTrue(model.states.value.confirmClearHistory)
        assertTrue(model.states.value.history.isNotEmpty(), "история очистилась без подтверждения")

        model.dispatch(MineEvent.ConfirmClearHistory)
        advanceUntilIdle()

        val state = model.states.value
        assertFalse(state.confirmClearHistory)
        assertTrue(state.history.isEmpty())
        assertTrue(state.continueWatching.isEmpty())
        assertTrue(watching.getHistory().getOrNull().orEmpty().isEmpty())
    }

    @Test
    fun `dismissing the clear history dialog keeps the history`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        model.dispatch(MineEvent.AskClearHistory)
        model.dispatch(MineEvent.DismissClearHistory)
        advanceUntilIdle()

        assertFalse(model.states.value.confirmClearHistory)
        assertTrue(model.states.value.history.isNotEmpty())
    }

    @Test
    fun `new folder is typed on the screen keyboard and keeps its color`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        model.dispatch(MineEvent.StartCreateFolder)
        "дача".forEach { letter -> model.dispatch(MineEvent.EditFolder(FolderInput.Key(letter.toString()))) }
        model.dispatch(MineEvent.EditFolder(FolderInput.Tint(BLUE)))
        advanceUntilIdle()
        assertEquals("Дача", model.states.value.draft?.title)

        model.dispatch(MineEvent.SubmitFolder)
        advanceUntilIdle()

        val state = model.states.value
        assertNull(state.draft, "панель создания не закрылась")
        val created = assertNotNull(state.folders.firstOrNull { it.folder.title == "Дача" })
        assertEquals(BLUE, created.folder.tint)
        // Только что созданная — самая свежая: наверху сетки.
        assertEquals(created, state.folders.first())
        assertEquals(BLUE, appearance.appearances.first()[created.folder.id]?.tint)
    }

    @Test
    fun `empty folder name is not submitted`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        model.dispatch(MineEvent.StartCreateFolder)
        model.dispatch(MineEvent.EditFolder(FolderInput.Space))
        model.dispatch(MineEvent.SubmitFolder)
        advanceUntilIdle()

        assertNotNull(model.states.value.draft, "пустое название закрыло панель")
        assertEquals(USER_FOLDERS, model.states.value.folders.size)
    }

    private fun MineScreenModel.folderIds(): List<Int> = states.value.folders.map { it.folder.id }

    private fun TestScope.loaded(): MineState {
        val model = model()
        advanceUntilIdle()
        return model.states.value
    }

    private fun model() = MineScreenModel(
        watching = watching,
        favorites = favorites,
        folders = BookmarkFolders(user, appearance),
        removeFromWatchLater = RemoveFromWatchLaterUseCase(favorites, watching),
        clock = clock,
    )

    private fun favorite(id: Int, title: String, rating: String? = null) =
        FavoriteItem(id = id, title = title, posterSmall = "", year = 2020, durationMinutes = 100, rating = rating)

    private companion object {
        /** Папки фикстуры без «Буду смотреть»: обычная, большая и пустая. */
        const val USER_FOLDERS = 3

        /** Второй цвет палитры папок — синий. */
        const val BLUE = 1
    }
}
