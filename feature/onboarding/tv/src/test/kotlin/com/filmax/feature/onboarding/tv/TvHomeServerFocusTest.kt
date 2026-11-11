package com.filmax.feature.onboarding.tv

import android.content.pm.PackageManager
import android.view.KeyEvent
import androidx.activity.ComponentActivity
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
import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.navigation.AppNavigator
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeContentSourceRepository
import com.filmax.data.fake.FakeHomeServerRepository
import com.filmax.feature.onboarding.common.HomeServerScreenModel
import com.filmax.feature.onboarding.common.HomeServerStep
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
import kotlin.test.assertTrue

/**
 * Вход на домашний сервер с пульта: найденный сервер — первый в фокусе и открывается «ОК»,
 * код Quick Connect показывается без клавиатуры, адрес набирается экранной клавиатурой справа,
 * «Назад» со входа возвращает к выбору сервера, а с выбора — к kino.pub.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvHomeServerFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val images = FakeImagesRule()

    private val sources = FakeContentSourceRepository()
    private var backToKinopub = false

    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `found server is focused first and signs in with a code`() {
        val model = show(FakeHomeServerRepository(approveOnCheck = Int.MAX_VALUE))
        assertFocusedHas("Дом")

        press(Key.DirectionCenter)

        assertEquals(HomeServerStep.CODE, model.states.value.step)
        compose.onNode(hasText("482913")).assertExists()
        assertFocusedHas("Войти по паролю")
    }

    @Test
    fun `address is typed with the keyboard on the right`() {
        val model = show(FakeHomeServerRepository(found = emptyList()))
        assertFocusedHas("Адрес сервера")

        press(Key.DirectionRight)
        assertFocusedHas("q")
        press(Key.DirectionCenter)

        assertEquals("q", model.states.value.address)
        compose.onNode(hasText("q▏", substring = true)).assertExists()
    }

    @Test
    fun `back goes from sign-in to the server choice and then to kinopub`() {
        val model = show(FakeHomeServerRepository(approveOnCheck = Int.MAX_VALUE))
        press(Key.DirectionCenter)
        assertEquals(HomeServerStep.CODE, model.states.value.step)

        pressBack()
        assertEquals(HomeServerStep.SERVER, model.states.value.step)
        assertFocusedHas("Дом")

        pressBack()
        assertTrue(backToKinopub, "«Назад» с выбора сервера не вернул к kino.pub")
        assertEquals(ContentSource.KINOPUB, sources.source.value)
    }

    private fun show(servers: FakeHomeServerRepository): HomeServerScreenModel {
        val model = HomeServerScreenModel(
            servers = servers,
            sources = sources,
            client = ClientInfo(appVersion = "2.8.0", isTv = true, device = "Test TV", os = "Android 14"),
        )
        compose.setContent {
            FilmaxTvTheme {
                TvHomeServerStep(onBack = { backToKinopub = true }, screenModel = model, navigator = AppNavigator())
            }
        }
        compose.waitForIdle()
        return model
    }

    private fun focusedMatcher(text: String): SemanticsMatcher =
        isFocused() and (hasText(text, substring = true) or hasAnyDescendant(hasText(text, substring = true)))

    private fun assertFocusedHas(text: String) {
        val focused = compose.onAllNodes(focusedMatcher(text)).fetchSemanticsNodes().isNotEmpty()
        assertTrue(focused, "в фокусе не «$text», а ${focusedTexts()}")
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
}
