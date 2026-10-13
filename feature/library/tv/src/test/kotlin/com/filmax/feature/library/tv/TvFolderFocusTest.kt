package com.filmax.feature.library.tv

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeFolderAppearanceRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.feature.library.common.FolderScreenModel
import com.filmax.feature.library.common.FolderSideEffect
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Экран папки с пульта: старт на первом постере, вверх — к действиям папки, «Переименовать»
 * открывает панель с клавиатурой и после «Назад» возвращает фокус на себя, «Удалить» спрашивает
 * с фокусом на «Отмена» и после подтверждения уводит назад. Папка — «Пересмотреть с семьёй»
 * из фейкового бэкенда, первый в ней — «Дом на краю карты».
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvFolderFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val images = FakeImagesRule()

    private val remote = TvRemote(compose)
    private val calls = mutableListOf<String>()

    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `folder opens on the first poster and ok opens the title`() {
        showFolder()
        remote.assertFocusedHas(FIRST_TITLE)

        remote.press(Key.DirectionCenter)

        assertEquals("open ${FakeIds.HERO_MOVIE}", calls.last())
    }

    @Test
    fun `up reaches the folder actions and play all starts the first title`() {
        showFolder()

        goToActions()
        remote.assertFocusedHas("Смотреть подряд")
        remote.press(Key.DirectionCenter)

        assertEquals("play ${FakeIds.HERO_MOVIE}", calls.last())
    }

    @Test
    fun `rename opens the editor with the current title and back returns focus`() {
        val model = showFolder()
        goToActions()
        remote.press(Key.DirectionRight)
        remote.assertFocusedHas("Переименовать")

        remote.press(Key.DirectionCenter)
        assertEquals(FOLDER_TITLE, model.states.value.draft?.title)
        remote.assertFocusedHas(FIRST_KEY)

        remote.back()
        assertNull(model.states.value.draft, "«Назад» не закрыл панель")
        remote.assertFocusedHas("Переименовать")
    }

    @Test
    fun `delete asks with focus on cancel and confirming leaves the screen`() {
        val model = showFolder()
        goToActions()
        remote.press(Key.DirectionRight)
        remote.press(Key.DirectionRight)
        remote.assertFocusedHas("Удалить папку")

        remote.press(Key.DirectionCenter)
        assertTrue(model.states.value.confirmDelete)
        remote.assertFocusedHas("Отмена")

        remote.press(Key.DirectionRight)
        remote.press(Key.DirectionCenter)

        assertEquals("back", calls.last())
    }

    /** С постера вверх — к кнопкам шапки: первая из них — «Смотреть подряд». */
    private fun goToActions() {
        repeat(MAX_PRESSES) { if (!remote.focusedHas("Смотреть подряд")) remote.press(Key.DirectionUp) }
    }

    private fun showFolder(): FolderScreenModel {
        val user = FakeUserRepository(FakeBackend(latency = FakeLatency.None))
        val model = FolderScreenModel(
            savedStateHandle = SavedStateHandle(mapOf("folderId" to FOLDER_ID)),
            user = user,
            folders = BookmarkFolders(user, FakeFolderAppearanceRepository()),
        )
        compose.setContent {
            FilmaxTvTheme {
                val state by model.collectAsState()
                model.collectSideEffect { effect -> if (effect == FolderSideEffect.Deleted) calls += "back" }
                FolderContent(
                    state = state,
                    actions = FolderActions(
                        onEvent = model::dispatch,
                        onOpenItem = { id -> calls += "open $id" },
                        onPlay = { id -> calls += "play $id" },
                        onOpenCatalog = { calls += "catalog" },
                    ),
                )
            }
        }
        compose.waitForIdle()
        assertNotNull(model.states.value.folder, "папка не загрузилась")
        return model
    }

    private companion object {
        const val FOLDER_ID = 2
        const val FOLDER_TITLE = "Пересмотреть с семьёй"
        const val FIRST_TITLE = "Дом на краю карты"
        const val FIRST_KEY = "а"

        /** Потолок нажатий «вверх» с постера до шапки. */
        const val MAX_PRESSES = 3
    }
}
