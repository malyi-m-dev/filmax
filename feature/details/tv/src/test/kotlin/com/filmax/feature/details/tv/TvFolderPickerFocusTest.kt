package com.filmax.feature.details.tv

import android.content.pm.PackageManager
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeFolderAppearanceRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.FolderPickerScreenModel
import com.filmax.feature.details.common.calculateSeriesData
import com.filmax.feature.details.common.isSeries
import kotlinx.coroutines.runBlocking
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Панель «В папку» с пульта на настоящей модели и фейковом бэкенде: «В папку» рядом с «Буду
 * смотреть», фокус в панели — на первой папке без тайтла, «ОК» отмечает сразу, «Назад» закрывает
 * панель и возвращает фокус на кнопку; «Создать папку» открывает редактор с фокусом на
 * клавиатуре, «Назад» из редактора — к списку, а не из панели. Фильм — «Дом на краю карты»: он
 * уже лежит в «Пересмотреть с семьёй».
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvFolderPickerFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val images = FakeImagesRule()

    private val backend = FakeBackend(latency = FakeLatency.None)
    private val user = FakeUserRepository(backend)

    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `folder panel opens on the first folder without the title and back returns to the button`() {
        val picker = showMovie()
        openPanel()

        assertTrue(picker.states.value.open)
        assertFocusedHas(LATER_FOLDER)

        pressBack()
        assertFalse(picker.states.value.open, "«Назад» не закрыл панель")
        assertFocusedHas("В папку")
    }

    @Test
    fun `ok on a folder marks the title at once`() {
        val picker = showMovie()
        openPanel()

        press(Key.DirectionCenter)

        assertTrue(FakeIds.LARGE_FOLDER in picker.states.value.memberOf)
        val inFolder = runBlocking { user.getBookmarkItems(FakeIds.LARGE_FOLDER).getOrNull()?.items.orEmpty() }
        assertTrue(inFolder.any { it.id == FakeIds.HERO_MOVIE }, "отметка не дошла до сервера")
    }

    @Test
    fun `create folder opens the keyboard and back goes back to the list`() {
        val picker = showMovie()
        openPanel()
        repeat(MAX_PRESSES) { if (!focusedHas("Создать папку")) press(Key.DirectionDown) }

        press(Key.DirectionCenter)
        assertNotNull(picker.states.value.draft, "редактор не открылся")
        assertFocusedHas(FIRST_KEY)
        press(Key.DirectionCenter)
        assertEquals("А", picker.states.value.draft?.title)

        pressBack()
        assertNull(picker.states.value.draft, "«Назад» не вернул к списку")
        assertTrue(picker.states.value.open, "«Назад» из редактора закрыл всю панель")
        assertFocusedHas(LATER_FOLDER)
    }

    @Test
    fun `series has a round folder button next to watch later`() {
        val picker = show(FakeIds.LONG_SERIES)
        // Сериал открывается на серии «продолжить»: влево — к кнопкам, дальше вправо по ним.
        press(Key.DirectionLeft)
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        assertFocusedHas("В папку")

        press(Key.DirectionCenter)

        assertTrue(picker.states.value.open)
    }

    /** С «Смотреть» вправо — «Буду смотреть», ещё вправо — «В папку»; «ОК» открывает панель. */
    private fun openPanel() {
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        assertFocusedHas("В папку")
        press(Key.DirectionCenter)
    }

    private fun showMovie(): FolderPickerScreenModel = show(FakeIds.HERO_MOVIE)

    private fun show(itemId: Int): FolderPickerScreenModel {
        val catalog = FakeCatalogRepository(backend)
        val item = runBlocking { checkNotNull(catalog.getItemDetails(itemId).getOrNull()) }
        val series = item.takeIf { it.isSeries() }?.let { calculateSeriesData(it.tracklist) }
        val picker = FolderPickerScreenModel(
            savedStateHandle = SavedStateHandle(mapOf("itemId" to itemId)),
            user = user,
            folders = BookmarkFolders(user, FakeFolderAppearanceRepository()),
            favorites = FakeFavoritesRepository(),
        )
        compose.setContent {
            FilmaxTvTheme {
                val pickerState by picker.collectAsState()
                val buttonFocus = remember { FocusRequester() }
                DetailsWithSheet(
                    state = DetailsState(
                        loading = false,
                        item = item,
                        series = series,
                        selectedSeason = series?.let { it.seasons[it.resumeSeasonIndex].first },
                    ),
                    item = item,
                    actions = actions(),
                    folders = DetailsFolders(pickerState, picker::dispatch, buttonFocus),
                )
            }
        }
        compose.waitForIdle()
        return picker
    }

    private fun actions() = DetailsActions(
        onPlay = { _, _ -> },
        onToggleWatchLater = {},
        onTrailer = null,
        onOpenItem = {},
        onOpenPerson = { _, _ -> },
        onOpenDescription = {},
        onCloseDescription = {},
        onSelectSeason = {},
    )

    private fun focusedMatcher(text: String): SemanticsMatcher {
        val has = hasText(text, substring = true) or hasContentDescription(text, substring = true)
        return isFocused() and (has or hasAnyDescendant(has))
    }

    private fun focusedHas(text: String): Boolean =
        compose.onAllNodes(focusedMatcher(text)).fetchSemanticsNodes().isNotEmpty()

    private fun assertFocusedHas(text: String) {
        assertTrue(focusedHas(text), "в фокусе не «$text», а ${focusedTexts()}")
    }

    private fun focusedTexts(): List<String> = compose.onAllNodes(isFocused())
        .fetchSemanticsNodes()
        .flatMap { node ->
            node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } +
                node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
        }

    @OptIn(ExperimentalTestApi::class)
    private fun press(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    /** «Назад» через окно активити, как на устройстве: только так он доходит до BackHandler. */
    private fun pressBack() {
        compose.runOnUiThread {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        }
        compose.waitForIdle()
    }

    private companion object {
        /** Первая папка без «Дома на краю карты» — «Отложено на потом». */
        const val LATER_FOLDER = "Отложено на потом"
        const val FIRST_KEY = "а"

        /** Потолок нажатий «вниз» до «Создать папку». */
        const val MAX_PRESSES = 6
    }
}
