package com.filmax.app.tv.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ТВ-оболочка целиком — меню и разделы на мок-данных: выбор раздела в меню уводит фокус в раздел,
 * капля фокуса перетекает с пунктов и на карточку профиля внизу меню.
 */
@RunWith(RobolectricTestRunner::class)
// Своё Application: приложение поднимает Koin в onCreate, а тест — свой граф с фейками.
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvSideNavFocusTest {

    private val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(TvShellTestRule()).around(compose)

    @Test
    fun `profile chosen in the menu takes the focus`() {
        showShell()
        openMenu()

        repeat(MAX_PRESSES) { if (!focusedHas("ДЗ")) press(Key.DirectionDown) }
        press(Key.DirectionCenter)

        assertFocusedHas("Качество видео")
    }

    @Test
    fun `my stuff chosen in the menu takes the focus`() {
        showShell()
        openMenu()

        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)

        assertTrue(!focusedHas("Главная") && !focusedHas("Моё"), "фокус остался в меню: ${focusedTexts()}")
    }

    @Test
    fun `drop flows from my stuff onto the profile card`() {
        showShell()
        openMenu()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        assertDropOver("Моё")

        press(Key.DirectionDown)

        assertDropOver("Демо Зритель")
    }

    /** Капля встала ровно на элемент в фокусе: её верх и низ совпадают с его границами. */
    private fun assertDropOver(text: String) {
        val target = compose.onAllNodes(focusedMatcher(text)).fetchSemanticsNodes().firstOrNull()
        assertNotNull(target, "в фокусе не «$text», а ${focusedTexts()}")
        val drop = compose.onNodeWithTag(NAV_DROP_TAG).getBoundsInRoot()
        val tolerance = 1.dp
        assertTrue(
            abs((drop.top - target.boundsInRoot.top.toDp()).value) <= tolerance.value &&
                abs((drop.bottom - target.boundsInRoot.bottom.toDp()).value) <= tolerance.value,
            "капля $drop не на «$text» (${target.boundsInRoot})",
        )
    }

    private fun Float.toDp() = with(compose.density) { this@toDp.toDp() }

    private fun showShell() {
        compose.setContent { FilmaxTvTheme { FilmaxTvNavGraph(onCheckUpdates = {}) } }
        compose.waitForIdle()
    }

    /** Влево с левого края Главной — в меню; вход в меню — на открытый раздел. */
    private fun openMenu() {
        repeat(MAX_PRESSES) { if (!focusedHas("Главная")) press(Key.DirectionLeft) }
        assertFocusedHas("Главная")
    }

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

    private companion object {
        /** Потолок нажатий до нужного пункта меню. */
        const val MAX_PRESSES = 6
    }
}
