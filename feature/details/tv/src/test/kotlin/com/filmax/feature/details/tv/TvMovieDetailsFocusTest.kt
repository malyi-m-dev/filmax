package com.filmax.feature.details.tv

import android.content.pm.PackageManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.feature.details.common.DetailsState
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
import kotlin.test.assertTrue

/**
 * Фокус в деталях фильма с пульта: старт на «Смотреть», фильм играется без сезона, «Буду
 * смотреть» рядом, «вниз» доводит до «Похожих». Фильм — hero мок-сборки «Дом на краю карты».
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvMovieDetailsFocusTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val images = FakeImagesRule()

    private val calls = mutableListOf<String>()
    private val catalog = FakeCatalogRepository(FakeBackend(latency = FakeLatency.None))

    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `screen opens on play and plays the movie without a season`() {
        showMovie()
        assertFocusedHas("Смотреть")

        pressKey(Key.DirectionCenter)

        val (season, _) = calls.last().removePrefix("play ").split("/").map(String::toInt)
        assertTrue(season <= 0, "фильм запущен с сезоном $season")
    }

    @Test
    fun `watch later sits right of play`() {
        showMovie()

        pressKey(Key.DirectionRight)
        pressKey(Key.DirectionCenter)

        assertEquals("watch later", calls.last())
    }

    @Test
    fun `down reaches similar titles and ok opens one of them`() {
        val similar = showMovie().similar

        // Вниз по ленте с «ОК» на каждом ряду: состав открывает актёра, «Похожие» — тайтл.
        repeat(MAX_PRESSES) {
            if (calls.none { it.startsWith("open") }) {
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionCenter)
            }
        }

        val opened = checkNotNull(calls.lastOrNull { it.startsWith("open") }) { "до «Похожих» не дошли: $calls" }
        assertTrue(similar.any { it.id == opened.removePrefix("open ").toInt() }, "открыт не тайтл из «Похожих»")
    }

    private fun showMovie(): DetailsState {
        val state = runBlocking {
            DetailsState(
                loading = false,
                item = checkNotNull(catalog.getItemDetails(FakeIds.HERO_MOVIE).getOrNull()),
                similar = catalog.getSimilarItems(FakeIds.HERO_MOVIE).getOrNull().orEmpty(),
            )
        }
        val item = checkNotNull(state.item)
        assertTrue(state.similar.isNotEmpty(), "у фикстуры нет «Похожих»")
        compose.setContent {
            FilmaxTvTheme { DetailsWithSheet(state, item, actions()) }
        }
        compose.waitForIdle()
        return state
    }

    private fun actions() = DetailsActions(
        onPlay = { season, videoId -> calls += "play $season/$videoId" },
        onToggleWatchLater = { calls += "watch later" },
        onTrailer = null,
        onOpenItem = { id -> calls += "open $id" },
        onOpenPerson = { name, _ -> calls += "person $name" },
        onOpenDescription = { calls += "description" },
        onCloseDescription = {},
        onSelectSeason = { number -> calls += "season $number" },
    )

    private fun focusedHas(text: String): Boolean = compose.onAllNodes(
        isFocused() and (hasText(text, substring = true) or hasAnyDescendant(hasText(text, substring = true))),
    ).fetchSemanticsNodes().isNotEmpty()

    private fun assertFocusedHas(text: String) {
        assertTrue(focusedHas(text), "в фокусе не «$text»")
    }

    @OptIn(ExperimentalTestApi::class)
    private fun pressKey(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private companion object {
        /** Потолок нажатий «вниз» до «Похожих»: шапка, состав — с запасом. */
        const val MAX_PRESSES = 8
    }
}
