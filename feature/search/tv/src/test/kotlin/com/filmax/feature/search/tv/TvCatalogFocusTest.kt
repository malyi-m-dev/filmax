package com.filmax.feature.search.tv

import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.ComponentActivity
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
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.navigation.AppNavigator
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeSearchRepository
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Фокус каталога и поиска с пульта на настоящем [SearchScreenModel] и фейковом бэкенде: старт на
 * поле, «вниз» по чипам в сетку и обратно, поиск с клавиатурой и лучшим совпадением, «Назад»,
 * боковые панели сортировки и фильтров.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
// Нативная графика меряет текст по-настоящему: от ширины чипов зависит, куда уходит фокус.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvCatalogFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val images = FakeImagesRule()

    private val backend = FakeBackend(latency = FakeLatency.None)
    private val model = SearchScreenModel(FakeSearchRepository(backend), FakeCatalogRepository(backend))

    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `screen opens with focus on the search field`() {
        showCatalog()

        assertFocusedHas(PLACEHOLDER)
    }

    @Test
    fun `down goes through type chips and genres into the grid`() {
        showCatalog()

        pressKey(Key.DirectionDown)
        assertFocusedHas("Все")
        pressKey(Key.DirectionDown)
        assertFocusedHas(FIRST_GENRE)
        pressKey(Key.DirectionDown)

        assertPosterFocused(model.states.value.catalogItems.first().title)
    }

    @Test
    fun `up from the grid returns to the genre the viewer left`() {
        showCatalog()
        repeat(DOWN_TO_GRID) { pressKey(Key.DirectionDown) }
        assertPosterFocused(model.states.value.catalogItems.first().title)

        pressKey(Key.DirectionUp)

        assertFocusedHas(FIRST_GENRE)
    }

    @Test
    fun `second row scrolls the header away and the first row brings it back`() {
        showCatalog()
        repeat(DOWN_TO_GRID + 1) { pressKey(Key.DirectionDown) }
        assertFalse(fieldShown(), "вторая строка не встала на опорную точку — шапка осталась на экране")

        pressKey(Key.DirectionUp)

        assertPosterFocused(model.states.value.catalogItems.first().title)
        assertTrue(fieldShown(), "первая строка вернулась без шапки")
    }

    @Test
    fun `ok on the field opens the search with focus on the keyboard`() {
        showCatalog()

        pressKey(Key.DirectionCenter)

        assertTrue(model.states.value.searchOpen, "поиск не открылся")
        assertFocusedHas("а")
        pressKey(Key.DirectionCenter)
        assertEquals("а", model.states.value.query)
    }

    @Test
    fun `right from the keyboard edge goes to the best match`() {
        showCatalog()
        pressKey(Key.DirectionCenter)
        "хрони".forEach { model.dispatch(SearchEvent.TypeKey(it.toString())) }
        waitForSearch()

        repeat(KEYBOARD_COLUMNS) { pressKey(Key.DirectionRight) }

        assertFocusedHas(BEST_MATCH_TITLE)
    }

    @Test
    fun `query submitted at once moves focus to the best match`() {
        showCatalog()

        compose.runOnUiThread { model.dispatch(SearchEvent.OpenSearch("хрони")) }
        compose.waitForIdle()

        assertEquals(FakeIds.LONG_SERIES, model.states.value.bestMatch?.id)
        assertFocusedHas(BEST_MATCH_TITLE)
    }

    @Test
    fun `back in the search closes it and returns focus to the field`() {
        showCatalog()
        pressKey(Key.DirectionCenter)
        pressKey(Key.DirectionCenter)

        pressBack()

        assertFalse(model.states.value.searchOpen, "поиск не закрылся")
        assertEquals("", model.states.value.query)
        assertFocusedHas(PLACEHOLDER)
    }

    @Test
    fun `sort sheet opens on the current sort and returns focus to its chip`() {
        showCatalog()
        pressKey(Key.DirectionDown)
        pressUntilFocused(Key.DirectionRight, "Просмотры")

        pressKey(Key.DirectionCenter)
        assertTrue(model.states.value.sheetOpen, "панель сортировки не открылась")
        assertFocusedHas("Просмотры")
        pressKey(Key.DirectionDown)
        pressKey(Key.DirectionCenter)

        assertEquals(CatalogSort.YEAR, model.states.value.sort.field)
        assertFalse(model.states.value.sheetOpen, "панель не закрылась после выбора")
        assertFocusedHas("Год")
    }

    @Test
    fun `filters are applied from the sheet and focus comes back to the chip`() {
        showCatalog()
        pressKey(Key.DirectionDown)
        pressUntilFocused(Key.DirectionRight, "Фильтры")

        pressKey(Key.DirectionCenter)
        assertTrue(model.states.value.sheetOpen, "панель фильтров не открылась")
        assertFocusedHas("Любой")
        pressKey(Key.DirectionRight)
        pressKey(Key.DirectionCenter)
        assertEquals(DECADE_START, model.states.value.filtersDraft.yearFrom)
        assertEquals(null, model.states.value.filters.yearFrom, "черновик применился без «Применить»")
        pressUntilFocused(Key.DirectionDown, "Применить")
        pressKey(Key.DirectionCenter)

        assertEquals(DECADE_START, model.states.value.filters.yearFrom)
        assertFalse(model.states.value.sheetOpen, "панель не закрылась после «Применить»")
        assertFocusedHas("Фильтры · 1")
    }

    @Test
    fun `back closes the filters sheet without applying the draft`() {
        showCatalog()
        pressKey(Key.DirectionDown)
        pressUntilFocused(Key.DirectionRight, "Фильтры")
        pressKey(Key.DirectionCenter)
        pressKey(Key.DirectionRight)
        pressKey(Key.DirectionCenter)

        pressBack()

        assertFalse(model.states.value.sheetOpen, "«Назад» не закрыл панель")
        assertEquals(null, model.states.value.filters.yearFrom)
        assertFocusedHas("Фильтры")
    }

    private fun showCatalog() {
        compose.setContent {
            FilmaxTvTheme { TvCatalogScreen(screenModel = model, navigator = AppNavigator()) }
        }
        compose.waitForIdle()
    }

    /** Набранный запрос ищется после debounce модели — время главного потока сдвигаем вручную. */
    private fun waitForSearch() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SEARCH_WAIT_MS))
        compose.waitForIdle()
    }

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /**
     * Поле поиска на экране — шапка сетки не уехала за верхний край. Уехавшая шапка остаётся в дереве
     * (сетка держит её ради памяти фокуса), но не размещается — её границы пустые.
     */
    private fun fieldShown(): Boolean = compose.onAllNodes(hasText(PLACEHOLDER)).fetchSemanticsNodes()
        .any { node -> !node.boundsInWindow.isEmpty && node.boundsInWindow.top >= 0f }

    private fun pressUntilFocused(key: Key, text: String) {
        repeat(MAX_PRESSES) { if (!focusedHas(text)) pressKey(key) }
        assertFocusedHas(text)
    }

    private fun focusedMatcher(text: String): SemanticsMatcher =
        isFocused() and (hasText(text, substring = true) or hasAnyDescendant(hasText(text, substring = true)))

    private fun focusedHas(text: String): Boolean =
        compose.onAllNodes(focusedMatcher(text)).fetchSemanticsNodes().isNotEmpty()

    private fun assertFocusedHas(text: String) {
        assertTrue(focusedHas(text), "в фокусе не «$text», а ${focusedTexts()}")
    }

    /** Подпись постера лежит под карточкой, а не в ней, — постер узнаём по описанию картинки. */
    private fun assertPosterFocused(title: String) {
        val matcher = isFocused() and (hasContentDescription(title) or hasAnyDescendant(hasContentDescription(title)))
        assertTrue(
            compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty(),
            "в фокусе не постер «$title», а ${focusedTexts()}"
        )
    }

    /** Тексты узла в фокусе — для сообщения о провале: куда фокус ушёл на самом деле. */
    private fun focusedTexts(): List<String> = compose.onAllNodes(isFocused(), useUnmergedTree = false)
        .fetchSemanticsNodes()
        .flatMap { node -> node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } }

    @OptIn(ExperimentalTestApi::class)
    private fun pressKey(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private companion object {
        const val PLACEHOLDER = "Название фильма или сериала"
        const val FIRST_GENRE = "Драма"
        const val BEST_MATCH_TITLE = "Хроники Пограничья"

        /** Поле → типы → жанры → сетка. */
        const val DOWN_TO_GRID = 3

        /** «Вправо» с первой клавиши: семь шагов до края ряда и восьмой — за край. */
        const val KEYBOARD_COLUMNS = 8
        const val DECADE_START = 2020
        const val SEARCH_WAIT_MS = 500L
        const val MAX_PRESSES = 12
    }
}
