package com.filmax.feature.library.tv

import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import kotlin.test.assertTrue

/**
 * Пульт для UI-тестов фокуса: стрелки и «ОК» — прямо в Compose, «Назад» и долгое «ОК» — через
 * окно активити, как их получает приложение на устройстве.
 */
internal class TvRemote(private val compose: AndroidComposeTestRule<*, ComponentActivity>) {

    @OptIn(ExperimentalTestApi::class)
    fun press(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    /**
     * «Назад»: сначала событие получает Compose (фокус), и только не съеденное доходит до
     * BackHandler. performKeyInput шлёт клавишу прямо в Compose и до BackHandler её не доводит.
     */
    fun back() = dispatch(
        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK),
        KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK),
    )

    /**
     * Долгое «ОК» так, как его присылает система: нажатие, повтор с флагом долгого нажатия и
     * отпускание с флагом «долгое нажатие отменено» — клик после него не срабатывает.
     */
    fun longPressOk() {
        val down = SystemClock.uptimeMillis()
        val held = down + LONG_PRESS_MS
        dispatch(
            okEvent(down, down, KeyEvent.ACTION_DOWN, repeat = 0, flags = 0),
            okEvent(down, held, KeyEvent.ACTION_DOWN, repeat = 1, flags = KeyEvent.FLAG_LONG_PRESS),
            okEvent(down, held, KeyEvent.ACTION_UP, repeat = 0, flags = KeyEvent.FLAG_CANCELED_LONG_PRESS),
        )
    }

    private fun okEvent(down: Long, time: Long, action: Int, repeat: Int, flags: Int): KeyEvent =
        KeyEvent(down, time, action, KeyEvent.KEYCODE_DPAD_CENTER, repeat, 0, 0, 0, flags)

    fun focusedHas(text: String): Boolean = compose.onAllNodes(focusedMatcher(text)).fetchSemanticsNodes().isNotEmpty()

    fun assertFocusedHas(text: String) {
        assertTrue(focusedHas(text), "в фокусе не «$text», а ${focusedTexts()}")
    }

    /** Подпись карточки лежит под ней, вне фокусируемого узла, — поэтому ищем и по описанию кадра. */
    private fun focusedMatcher(text: String): SemanticsMatcher {
        val has = hasText(text, substring = true) or hasContentDescription(text, substring = true)
        return isFocused() and (has or hasAnyDescendant(has))
    }

    /** Тексты узла в фокусе — для сообщения о провале: куда фокус ушёл на самом деле. */
    fun focusedTexts(): List<String> = compose.onAllNodes(isFocused())
        .fetchSemanticsNodes()
        .flatMap { node ->
            node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } +
                node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
        }

    private fun dispatch(vararg events: KeyEvent) {
        compose.runOnUiThread { events.forEach { compose.activity.dispatchKeyEvent(it) } }
        compose.waitForIdle()
    }

    private companion object {
        /** Системный порог долгого нажатия — с запасом. */
        const val LONG_PRESS_MS = 600L
    }
}
