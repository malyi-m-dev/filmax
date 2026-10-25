package com.filmax.feature.library.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeFolderAppearanceRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeNavigator
import com.filmax.data.fake.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * Экран папки на фейковом бэкенде: постраничное содержимое, удаление тайтла, переименование
 * (локальное — у kino.pub его нет) и удаление папки. Robolectric — ради маршрута: `toRoute`
 * читает аргументы из SavedStateHandle через Bundle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend(latency = FakeLatency.None)
    private val user = FakeUserRepository(backend)
    private val appearance = FakeFolderAppearanceRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `large folder loads page by page`() = runTest(dispatcher) {
        val model = model(FakeIds.LARGE_FOLDER)
        advanceUntilIdle()
        val first = model.states.value
        assertEquals("Отложено на потом", first.folder?.title)
        assertEquals(PAGE_SIZE, first.items.size)
        assertFalse(first.endReached)

        model.dispatch(FolderEvent.LoadMore)
        advanceUntilIdle()

        val second = model.states.value
        assertEquals(LARGE_FOLDER_SIZE, second.items.size)
        assertTrue(second.endReached)
    }

    @Test
    fun `long press removes the title from the folder and the server`() = runTest(dispatcher) {
        val model = model(FOLDER_WITH_HERO)
        advanceUntilIdle()
        val before = assertNotNull(model.states.value.folder).count

        model.dispatch(FolderEvent.RemoveItem(FakeIds.HERO_MOVIE))
        advanceUntilIdle()

        val state = model.states.value
        assertTrue(state.items.none { it.id == FakeIds.HERO_MOVIE })
        assertEquals(before - 1, state.folder?.count)
        val onServer = user.getBookmarkItems(FOLDER_WITH_HERO).getOrNull()?.items.orEmpty()
        assertTrue(onServer.none { it.id == FakeIds.HERO_MOVIE })
    }

    @Test
    fun `rename edits the current title and keeps the new one with its color`() = runTest(dispatcher) {
        val model = model(FakeIds.EMPTY_FOLDER)
        advanceUntilIdle()

        model.dispatch(FolderEvent.StartRename)
        advanceUntilIdle()
        assertEquals("Новогоднее", model.states.value.draft?.title)

        repeat("Новогоднее".length) { model.dispatch(FolderEvent.EditFolder(FolderInput.Erase)) }
        "ёлка".forEach { letter -> model.dispatch(FolderEvent.EditFolder(FolderInput.Key(letter.toString()))) }
        model.dispatch(FolderEvent.EditFolder(FolderInput.Tint(GREEN)))
        model.dispatch(FolderEvent.SubmitRename)
        advanceUntilIdle()

        val state = model.states.value
        assertNull(state.draft)
        assertEquals("Ёлка", state.folder?.title)
        assertEquals(GREEN, state.folder?.tint)
        assertEquals("Ёлка", appearance.appearances.first()[FakeIds.EMPTY_FOLDER]?.title)
    }

    @Test
    fun `cancel rename leaves the folder as it was`() = runTest(dispatcher) {
        val model = model(FakeIds.EMPTY_FOLDER)
        advanceUntilIdle()

        model.dispatch(FolderEvent.StartRename)
        model.dispatch(FolderEvent.EditFolder(FolderInput.Erase))
        model.dispatch(FolderEvent.CancelRename)
        advanceUntilIdle()

        assertNull(model.states.value.draft)
        assertEquals("Новогоднее", model.states.value.folder?.title)
    }

    @Test
    fun `delete asks first and then removes the folder with its color`() = runTest(dispatcher) {
        appearance.setTint(FakeIds.EMPTY_FOLDER, GREEN)
        val model = model(FakeIds.EMPTY_FOLDER)
        advanceUntilIdle()

        model.dispatch(FolderEvent.AskDelete)
        advanceUntilIdle()
        assertTrue(model.states.value.confirmDelete)
        assertTrue(folderExists(FakeIds.EMPTY_FOLDER), "папка удалилась без подтверждения")

        model.dispatch(FolderEvent.ConfirmDelete)
        advanceUntilIdle()

        assertFalse(model.states.value.confirmDelete)
        assertFalse(folderExists(FakeIds.EMPTY_FOLDER))
        assertNull(appearance.appearances.first()[FakeIds.EMPTY_FOLDER])
    }

    @Test
    fun `dismissing the delete dialog keeps the folder`() = runTest(dispatcher) {
        val model = model(FakeIds.EMPTY_FOLDER)
        advanceUntilIdle()

        model.dispatch(FolderEvent.AskDelete)
        model.dispatch(FolderEvent.DismissDelete)
        advanceUntilIdle()

        assertFalse(model.states.value.confirmDelete)
        assertTrue(folderExists(FakeIds.EMPTY_FOLDER))
    }

    @Test
    fun `unknown folder ends loading without a header`() = runTest(dispatcher) {
        val model = model(UNKNOWN_FOLDER)
        advanceUntilIdle()

        assertFalse(model.states.value.loading)
        assertNull(model.states.value.folder)
    }

    private suspend fun folderExists(id: Int): Boolean =
        user.getBookmarkFolders().getOrNull().orEmpty().any { it.id == id }

    private fun model(folderId: Int) = FolderScreenModel(
        savedStateHandle = SavedStateHandle(mapOf("folderId" to folderId)),
        user = user,
        folders = BookmarkFolders(user, appearance),
        navigator = FakeNavigator(),
    )

    private companion object {
        /** Папка «Пересмотреть с семьёй» — в ней лежит «Дом на краю карты». */
        const val FOLDER_WITH_HERO = 2
        const val PAGE_SIZE = 20
        const val LARGE_FOLDER_SIZE = 25
        const val UNKNOWN_FOLDER = 999
        const val GREEN = 3
    }
}
