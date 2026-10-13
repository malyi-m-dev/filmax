package com.filmax.feature.details.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeFolderAppearanceRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
 * Панель «В папку» на фейковом бэкенде. «Дом на краю карты» (HERO_MOVIE) уже лежит в папке
 * «Пересмотреть с семьёй» — по ней видно, что отметки берутся с сервера, а не угадываются.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderPickerScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend(latency = FakeLatency.None)
    private val user = FakeUserRepository(backend)
    private val appearance = FakeFolderAppearanceRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `folders load only when the panel opens`() = runTest(dispatcher) {
        val model = model(FakeIds.HERO_MOVIE)
        advanceUntilIdle()
        assertTrue(model.states.value.folders.isEmpty(), "папки загрузились вместе с деталями")

        model.dispatch(FolderPickerEvent.Open)
        advanceUntilIdle()

        val state = model.states.value
        assertTrue(state.open)
        assertFalse(state.loading)
        assertEquals(USER_FOLDERS, state.folders.size)
        assertTrue(state.folders.none { it.id == FakeIds.WATCHLIST_FOLDER }, "системная папка в списке папок")
    }

    @Test
    fun `title marks come from the server and focus goes to the first folder without it`() = runTest(dispatcher) {
        val state = opened(FakeIds.HERO_MOVIE)

        assertEquals(setOf(FOLDER_WITH_HERO), state.memberOf)
        assertTrue(state.inAnyFolder)
        // Свежая папка с тайтлом — первая в списке, фокус на следующую, где его ещё нет.
        assertEquals(FOLDER_WITH_HERO, state.folders.first().id)
        assertEquals(FakeIds.LARGE_FOLDER, state.focusFolderId)
    }

    @Test
    fun `ok on a folder toggles the mark and the server at once`() = runTest(dispatcher) {
        val model = model(FakeIds.HERO_MOVIE)
        model.dispatch(FolderPickerEvent.Open)
        advanceUntilIdle()

        model.dispatch(FolderPickerEvent.Toggle(FakeIds.EMPTY_FOLDER))
        advanceUntilIdle()
        assertTrue(FakeIds.EMPTY_FOLDER in model.states.value.memberOf)
        assertEquals(1, model.states.value.folders.first { it.id == FakeIds.EMPTY_FOLDER }.count)
        assertTrue(inFolder(FakeIds.HERO_MOVIE, FakeIds.EMPTY_FOLDER))

        model.dispatch(FolderPickerEvent.Toggle(FOLDER_WITH_HERO))
        advanceUntilIdle()
        assertFalse(FOLDER_WITH_HERO in model.states.value.memberOf)
        assertFalse(inFolder(FakeIds.HERO_MOVIE, FOLDER_WITH_HERO))
    }

    @Test
    fun `failed toggle rolls the mark back and says so`() = runTest(dispatcher) {
        val model = model(FakeIds.HERO_MOVIE)
        model.dispatch(FolderPickerEvent.Open)
        advanceUntilIdle()
        // Папку удалили на другом устройстве: сервер ответит «не найдено».
        user.deleteBookmarkFolder(FakeIds.EMPTY_FOLDER)

        model.dispatch(FolderPickerEvent.Toggle(FakeIds.EMPTY_FOLDER))
        advanceUntilIdle()

        val state = model.states.value
        assertFalse(FakeIds.EMPTY_FOLDER in state.memberOf, "отметка не откатилась")
        assertEquals(0, state.folders.first { it.id == FakeIds.EMPTY_FOLDER }.count)
        assertNotNull(state.error)
    }

    @Test
    fun `create and add puts the new folder on top, marked and focused`() = runTest(dispatcher) {
        val model = model(FakeIds.UNRATED_MOVIE)
        model.dispatch(FolderPickerEvent.Open)
        advanceUntilIdle()

        model.dispatch(FolderPickerEvent.StartCreate)
        "дача".forEach { letter -> model.dispatch(FolderPickerEvent.Edit(FolderInput.Key(letter.toString()))) }
        model.dispatch(FolderPickerEvent.Edit(FolderInput.Tint(AMBER)))
        advanceUntilIdle()
        assertNotNull(model.states.value.draft, "редактор не открылся")

        model.dispatch(FolderPickerEvent.Submit)
        advanceUntilIdle()

        val state = model.states.value
        assertNull(state.draft, "после создания панель не вернулась к списку")
        assertTrue(state.open, "панель закрылась вместо возврата к списку")
        val created = state.folders.first()
        assertEquals("Дача", created.title)
        assertEquals(AMBER, created.tint)
        assertTrue(created.id in state.memberOf)
        assertEquals(created.id, state.focusFolderId)
        assertTrue(inFolder(FakeIds.UNRATED_MOVIE, created.id))
        assertEquals(AMBER, appearance.appearances.first()[created.id]?.tint)
    }

    @Test
    fun `cancel in the editor returns to the list and close hides the panel`() = runTest(dispatcher) {
        val model = model(FakeIds.HERO_MOVIE)
        model.dispatch(FolderPickerEvent.Open)
        model.dispatch(FolderPickerEvent.StartCreate)
        advanceUntilIdle()

        model.dispatch(FolderPickerEvent.CancelCreate)
        advanceUntilIdle()
        assertNull(model.states.value.draft)
        assertTrue(model.states.value.open)

        model.dispatch(FolderPickerEvent.Close)
        advanceUntilIdle()
        assertFalse(model.states.value.open)
    }

    @Test
    fun `system row counts the watchlist`() = runTest(dispatcher) {
        val favorites = FakeFavoritesRepository()
        val model = model(FakeIds.HERO_MOVIE, favorites)
        advanceUntilIdle()

        favorites.add(FavoriteItem(1, "Тайтл", "", 2020, 90))
        advanceUntilIdle()

        assertEquals(1, model.states.value.watchlistCount)
    }

    private suspend fun inFolder(itemId: Int, folderId: Int): Boolean =
        user.getBookmarkItems(folderId).getOrNull()?.items.orEmpty().any { it.id == itemId }

    private fun TestScope.opened(itemId: Int): FolderPickerState {
        val model = model(itemId)
        model.dispatch(FolderPickerEvent.Open)
        advanceUntilIdle()
        return model.states.value
    }

    private fun model(itemId: Int, favorites: FakeFavoritesRepository = FakeFavoritesRepository()) =
        FolderPickerScreenModel(
            savedStateHandle = SavedStateHandle(mapOf("itemId" to itemId)),
            user = user,
            folders = BookmarkFolders(user, appearance),
            favorites = favorites,
        )

    private companion object {
        /** «Пересмотреть с семьёй» — папка, где «Дом на краю карты» уже лежит. */
        const val FOLDER_WITH_HERO = 2
        const val USER_FOLDERS = 3

        /** Последний цвет палитры папок — янтарный. */
        const val AMBER = 5
    }
}
