package com.filmax.core.tv.designsystem

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.filmax.core.testing.FakeImagesRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue

/**
 * Вход в ленивый ряд и сетку с пульта: на последний элемент в фокусе, на предпочтительный, на
 * первый видимый — и только на размещённый. Прокрученный ряд, где предпочтительный ушёл из
 * композиции, — случай краша 2.6.0: запасной вход `focusRestorer` звал запросчик без узла.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
// Нативная графика меряет текст по-настоящему: от ширины чипов зависит, что видно и куда уходит фокус.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvLazyFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val images = FakeImagesRule()

    private val above = FocusRequester()
    private lateinit var rowState: LazyListState

    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `first entry goes to the preferred chip`() {
        showRow(preferred = PREFERRED)

        pressKey(Key.DirectionDown)

        assertFocusedHas(chip(PREFERRED))
    }

    @Test
    fun `return goes to the chip focus left from, not to the nearest one`() {
        showRow(preferred = 0)
        pressKey(Key.DirectionDown)
        repeat(PREFERRED) { pressKey(Key.DirectionRight) }
        pressKey(Key.DirectionUp)
        assertFocusedHas(ABOVE)

        pressKey(Key.DirectionDown)

        assertFocusedHas(chip(PREFERRED))
    }

    @Test
    fun `row scrolled past the remembered and preferred chips is entered on the first visible one`() {
        showRow(preferred = 0)
        pressKey(Key.DirectionDown)
        pressKey(Key.DirectionUp)
        compose.runOnIdle { rowState.requestScrollToItem(FAR_CHIP) }
        compose.waitForIdle()
        assertTrue(compose.onAllNodes(hasText(chip(0))).fetchSemanticsNodes().isEmpty(), "первый чип ещё в композиции")

        pressKey(Key.DirectionDown)

        assertFocusedHas(chip(FAR_CHIP))
    }

    @Test
    fun `memory outlives leaving the composition`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { ChipRow(preferred = 0) }
        compose.waitForIdle()
        focusAbove()
        pressKey(Key.DirectionDown)
        repeat(PREFERRED) { pressKey(Key.DirectionRight) }
        pressKey(Key.DirectionUp)

        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        focusAbove()
        pressKey(Key.DirectionDown)

        assertFocusedHas(chip(PREFERRED))
    }

    @Test
    fun `requester before a grid enters it on the preferred item beyond the first screen`() {
        val gridFocus = FocusRequester()
        compose.setContent {
            FilmaxTvTheme {
                val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = FAR_CHIP)
                val focus = rememberTvLazyFocus()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    state = gridState,
                    modifier = Modifier
                        .focusRequester(gridFocus)
                        .tvLazyFocusGroup(focus, gridState, preferred = FAR_CHIP),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(CHIP_COUNT) { index -> Chip(focus, index) }
                }
            }
        }
        compose.waitForIdle()

        compose.runOnIdle { gridFocus.requestFocus() }
        compose.waitForIdle()

        assertFocusedHas(chip(FAR_CHIP))
    }

    private fun showRow(preferred: Int?) {
        compose.setContent { ChipRow(preferred) }
        compose.waitForIdle()
        focusAbove()
    }

    @Composable
    private fun ChipRow(preferred: Int?) {
        FilmaxTvTheme {
            val state = rememberLazyListState()
            rowState = state
            val focus = rememberTvLazyFocus()
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                TvButton(ABOVE, onClick = {}, focusRequester = above)
                LazyRow(
                    state = state,
                    modifier = Modifier.tvLazyFocusGroup(focus, state, preferred),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(CHIP_COUNT) { index -> Chip(focus, index) }
                }
            }
        }
    }

    @Composable
    private fun Chip(focus: TvLazyFocus, index: Int) {
        TvChip(chip(index), selected = false, onClick = {}, modifier = Modifier.tvLazyFocusItem(focus, index))
    }

    private fun focusAbove() {
        compose.runOnIdle { above.requestFocus() }
        compose.waitForIdle()
        assertFocusedHas(ABOVE)
    }

    private fun assertFocusedHas(text: String) {
        val matcher = isFocused() and (hasText(text) or hasAnyDescendant(hasText(text)))
        val found = compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        assertTrue(found, "в фокусе не «$text», а ${focusedTexts()}")
    }

    /** Тексты узла в фокусе — для сообщения о провале: куда фокус ушёл на самом деле. */
    private fun focusedTexts(): List<String> = compose.onAllNodes(isFocused())
        .fetchSemanticsNodes()
        .flatMap { node -> node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } }

    @OptIn(ExperimentalTestApi::class)
    private fun pressKey(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private companion object {
        const val ABOVE = "Над рядом"
        const val CHIP_COUNT = 40
        const val PREFERRED = 3
        const val FAR_CHIP = 24
        const val GRID_COLUMNS = 2

        fun chip(index: Int) = "Чип $index"
    }
}
