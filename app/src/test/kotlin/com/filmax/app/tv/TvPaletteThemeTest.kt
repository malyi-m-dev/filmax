package com.filmax.app.tv

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.Coral
import com.filmax.core.tv.designsystem.FxColors
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.LocalTvPlainFocus
import com.filmax.core.tv.designsystem.Mint
import com.filmax.core.tv.designsystem.Violet
import com.filmax.data.fake.FakeThemeSettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Палитра из Профиля доходит до всего ТВ-UI: тема отдаёт её через LocalFxColors и Material-схему
 * и меняет на лету — без перезапуска и повторного входа на экран.
 */
@RunWith(RobolectricTestRunner::class)
// Своё Application: приложение поднимает Koin в onCreate, а этому тесту Koin не нужен.
@Config(sdk = [34], application = Application::class)
class TvPaletteThemeTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val images = FakeImagesRule()

    private val settings = FakeThemeSettingsRepository()
    private var palette: FxColors? = null
    private var primary: Color? = null

    @Test
    fun `chosen palette repaints the screen at once`() {
        compose.setContent {
            FilmaxTvPaletteTheme(themeSettings = settings) {
                palette = LocalFxColors.current
                primary = MaterialTheme.colorScheme.primary
            }
        }
        compose.waitForIdle()
        assertEquals(Violet, palette)

        choose(ThemePalette.CORAL)
        assertEquals(Coral, palette)
        assertEquals(Coral.accent, primary)

        choose(ThemePalette.MINT)
        assertEquals(Mint, palette)
    }

    @Test
    fun `plain focus from the ui settings reaches every focusable at once`() {
        var plain: Boolean? = null
        compose.setContent {
            FilmaxTvPaletteTheme(themeSettings = settings) { plain = LocalTvPlainFocus.current }
        }
        compose.waitForIdle()
        assertEquals(false, plain, "по умолчанию обводка и свечение есть")

        runBlocking { settings.setPlainFocus(true) }
        compose.waitForIdle()
        assertEquals(true, plain)
    }

    private fun choose(next: ThemePalette) {
        runBlocking { settings.setPalette(next) }
        compose.waitForIdle()
    }
}
