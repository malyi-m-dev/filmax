package com.filmax.feature.profile.tv

import android.content.pm.PackageManager
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeAuthRepository
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakePlaybackSettingsRepository
import com.filmax.data.fake.FakeThemeSettingsRepository
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import com.filmax.feature.profile.common.ProfileScreenModel
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
import kotlin.test.assertTrue

/**
 * Профиль с пульта на настоящей модели: старт на «Качестве видео», «ОК» открывает панель с
 * фокусом на текущем значении, выбор применяется и возвращает фокус на строку; палитра
 * выбирается так же; «Выйти» спрашивает с фокусом на «Отмена», «Назад» возвращает на строку.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvProfileFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val backend = FakeBackend(latency = FakeLatency.None)
    private val playback = FakePlaybackSettingsRepository()
    private val theme = FakeThemeSettingsRepository()

    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `quality is chosen in the side panel and focus comes back to its row`() {
        val model = showProfile()
        assertFocusedHas("Качество видео")

        press(Key.DirectionCenter)
        assertTrue(model.states.value.settingOpen)
        assertFocusedHas("Авто")

        press(Key.DirectionDown)
        press(Key.DirectionCenter)

        assertFalse(model.states.value.settingOpen, "панель не закрылась после выбора")
        assertEquals("2160p", model.states.value.playback.quality)
        assertFocusedHas("Качество видео")
    }

    @Test
    fun `palette is chosen in the panel and saved`() {
        showProfile()
        repeat(MAX_PRESSES) { if (!focusedHas("Палитра")) press(Key.DirectionDown) }

        press(Key.DirectionCenter)
        assertFocusedHas("Фиолет")
        press(Key.DirectionDown)
        press(Key.DirectionCenter)

        assertEquals(ThemePalette.CORAL, theme.palette.value)
        assertFocusedHas("Палитра")
    }

    @Test
    fun `logout asks with focus on cancel and back returns to the row`() {
        val model = showProfile()
        repeat(MAX_PRESSES) { if (!focusedHas("Выйти из аккаунта")) press(Key.DirectionDown) }

        press(Key.DirectionCenter)
        assertTrue(model.states.value.confirmLogout)
        assertFocusedHas("Отмена")

        pressBack()
        assertFalse(model.states.value.confirmLogout, "«Назад» не закрыл диалог")
        assertFocusedHas("Выйти из аккаунта")
    }

    private fun showProfile(): ProfileScreenModel {
        val model = ProfileScreenModel(
            user = FakeUserRepository(backend),
            watching = FakeWatchingRepository(backend),
            auth = FakeAuthRepository(backend),
            favorites = FakeFavoritesRepository(),
            playbackSettings = playback,
            themeSettings = theme,
        )
        compose.setContent {
            FilmaxTvTheme {
                val state by model.collectAsState()
                ProfileContent(state, ProfileActions(onEvent = model::dispatch, onCheckUpdates = {}))
            }
        }
        compose.waitForIdle()
        return model
    }

    private fun focusedMatcher(text: String): SemanticsMatcher =
        isFocused() and (hasText(text, substring = true) or hasAnyDescendant(hasText(text, substring = true)))

    private fun focusedHas(text: String): Boolean =
        compose.onAllNodes(focusedMatcher(text)).fetchSemanticsNodes().isNotEmpty()

    private fun assertFocusedHas(text: String) {
        assertTrue(focusedHas(text), "в фокусе не «$text», а ${focusedTexts()}")
    }

    private fun focusedTexts(): List<String> = compose.onAllNodes(isFocused())
        .fetchSemanticsNodes()
        .flatMap { node -> node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } }

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
        /** Потолок нажатий «вниз» по колонке настроек. */
        const val MAX_PRESSES = 8
    }
}
