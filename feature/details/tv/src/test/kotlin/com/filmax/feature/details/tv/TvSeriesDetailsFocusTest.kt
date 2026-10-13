package com.filmax.feature.details.tv

import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.calculateSeriesData
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
 * Фокус в деталях сериала с пульта: старт на серии «продолжить», «←» из серий — на главную
 * кнопку, сезон — фокусом на чипе, «ОК» на серии — плеер с сезоном и номером серии. Сериал —
 * «Хроники Пограничья» из фейкового бэкенда: 7 сезонов, S4E7 начата.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
// Нативная графика меряет текст по-настоящему: описание фокусируется, только если не влезло.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvSeriesDetailsFocusTest {

    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `screen opens with focus on the episode to continue`() {
        showSeries()

        assertFocusedHas(RESUME_TITLE)
    }

    @Test
    fun `ok on an episode plays it with its season`() {
        showSeries()

        pressKey(Key.DirectionCenter)

        assertEquals("play 4/7", calls.last())
    }

    @Test
    fun `left from the episodes goes to the main button, not to the description`() {
        showSeries()

        pressKey(Key.DirectionLeft)

        assertFocusedHas("Продолжить · S4E7")
    }

    @Test
    fun `season changes when focus lands on its chip`() {
        showSeries()

        // Вверх по сериям до ряда чипов: фокус встаёт на выбранный сезон.
        repeat(MAX_PRESSES) { if (!focusedHas("Сезон 4")) pressKey(Key.DirectionUp) }
        assertFocusedHas("Сезон 4")
        pressKey(Key.DirectionRight)

        assertEquals("season 5", calls.last())
    }

    @Test
    fun `description sheet closes on left and gives focus back to the description`() {
        val item = longSeries()
        var state by mutableStateOf(seriesState(item))
        compose.setContent {
            FilmaxTvTheme {
                DetailsWithSheet(
                    state,
                    item,
                    actions(
                        onOpen = { state = state.copy(descriptionOpen = true) },
                        onClose = { state = state.copy(descriptionOpen = false) },
                    ),
                )
            }
        }
        compose.waitForIdle()

        // С серии «←» на главную кнопку, выше — описание: «ОК» открывает панель полного текста.
        pressKey(Key.DirectionLeft)
        repeat(MAX_PRESSES) { if (!focusedHas(READ_MORE)) pressKey(Key.DirectionUp) }
        assertFocusedHas(READ_MORE)
        pressKey(Key.DirectionCenter)
        assertTrue(state.descriptionOpen, "панель описания не открылась")

        pressKey(Key.DirectionLeft)

        assertTrue(!state.descriptionOpen, "панель описания не закрылась")
        assertFocusedHas(READ_MORE)
    }

    private fun showSeries() {
        val item = longSeries()
        compose.setContent {
            FilmaxTvTheme { DetailsWithSheet(seriesState(item), item, actions()) }
        }
        compose.waitForIdle()
    }

    private fun longSeries(): Item = runBlocking {
        val catalog = FakeCatalogRepository(FakeBackend(latency = FakeLatency.None))
        checkNotNull(catalog.getItemDetails(FakeIds.LONG_SERIES).getOrNull())
    }

    /** Состояние, как его собирает модель: сериал открыт на сезоне серии «продолжить». */
    private fun seriesState(item: Item): DetailsState {
        val series = calculateSeriesData(item.tracklist)
        return DetailsState(
            loading = false,
            item = item,
            series = series,
            selectedSeason = series.seasons[series.resumeSeasonIndex].first,
        )
    }

    private fun actions(onOpen: () -> Unit = {}, onClose: () -> Unit = {}) = DetailsActions(
        onPlay = { season, videoId -> calls += "play $season/$videoId" },
        onToggleWatchLater = { calls += "watch later" },
        onTrailer = null,
        onOpenItem = { id -> calls += "open $id" },
        onOpenPerson = { name, _ -> calls += "person $name" },
        onOpenDescription = onOpen,
        onCloseDescription = onClose,
        onSelectSeason = { number -> calls += "season $number" },
    )

    private fun focusedMatcher(text: String): SemanticsMatcher =
        isFocused() and (hasText(text, substring = true) or hasAnyDescendant(hasText(text, substring = true)))

    private fun focusedHas(text: String): Boolean =
        compose.onAllNodes(focusedMatcher(text)).fetchSemanticsNodes().isNotEmpty()

    private fun assertFocusedHas(text: String) {
        assertTrue(focusedHas(text), "в фокусе не «$text», а ${focusedTexts()}")
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
        const val RESUME_TITLE = "7. Корона без короля"
        const val READ_MORE = "Читать полностью"

        /** Потолок нажатий «вверх» до ряда чипов — с запасом на шесть серий над стартовой. */
        const val MAX_PRESSES = 12
    }
}
