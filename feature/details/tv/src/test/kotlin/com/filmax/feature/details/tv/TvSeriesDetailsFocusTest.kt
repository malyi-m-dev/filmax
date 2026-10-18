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
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.WATCH_STATUS_FINISHED
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
 * Фокус в деталях сериала с пульта: старт на серии «продолжить», «←/→» с серии листают сезоны,
 * «←» с первого сезона — на главную кнопку, сезон — и фокусом на чипе, «ОК» на серии — плеер с
 * сезоном и номером серии. Сериал — «Хроники Пограничья» из фейкового бэкенда: 7 сезонов,
 * S1–S3 и S4E1–E6 досмотрены, S4E7 начата.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
// Нативная графика меряет текст по-настоящему: описание фокусируется, только если не влезло.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvSeriesDetailsFocusTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val images = FakeImagesRule()

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
    fun `right on an episode opens the next season on its first episode`() {
        showSwitchableSeries()

        pressKey(Key.DirectionRight)

        assertEquals("switch 5", calls.last())
        // Фокус перешёл в новый список — на серию, с которой открылся сезон, а не повис.
        pressKey(Key.DirectionCenter)
        assertEquals("play 5/1", calls.last())
    }

    @Test
    fun `left on an episode goes back through the seasons, from the first one to the main button`() {
        showSwitchableSeries()

        repeat(RESUME_SEASON - 1) { pressKey(Key.DirectionLeft) }
        assertEquals(listOf("switch 3", "switch 2", "switch 1"), calls.takeLast(RESUME_SEASON - 1))

        // Листать дальше некуда — «←» уводит на главную кнопку, а не на описание.
        pressKey(Key.DirectionLeft)
        assertFocusedHas(CONTINUE)
    }

    @Test
    fun `right on the last season keeps focus on the episode`() {
        showSwitchableSeries()

        repeat(LAST_SEASON - RESUME_SEASON) { pressKey(Key.DirectionRight) }
        assertEquals("switch $LAST_SEASON", calls.last())

        pressKey(Key.DirectionRight)
        pressKey(Key.DirectionCenter)
        assertEquals("play $LAST_SEASON/1", calls.last())
    }

    @Test
    fun `up after an arrow switch enters the chip of the new season, not the one left before`() {
        showSwitchableSeries()

        // Побывать на чипах до смены сезона: возврат на чип четвёртого сезона, с которого ушли,
        // переключил бы сезон фокусом обратно. Первый сезон — с края ряда: ближайший к серии чип
        // другой, так что попасть на него можно только как на выбранный.
        repeat(MAX_PRESSES) { if (!focusedHas("Сезон 4")) pressKey(Key.DirectionUp) }
        pressKey(Key.DirectionDown)
        repeat(RESUME_SEASON - 1) { pressKey(Key.DirectionLeft) }
        assertEquals("switch 1", calls.last())

        repeat(MAX_PRESSES) { if (!focusedHas("Сезон")) pressKey(Key.DirectionUp) }

        assertFocusedHas("Сезон 1")
        assertEquals("season 1", calls.last())
    }

    @Test
    fun `long series keeps the season picked by arrows in view, and up enters its chip`() {
        showSwitchableSeries(manySeasons(resumeSeason = 1))

        // Ряд из двадцати чипов в панель не влезает: выбранный стрелками сезон уехал бы за край,
        // и войти в ряд на него было бы нельзя — фокус принимает только размещённый чип.
        repeat(MANY_SEASONS - 1) { pressKey(Key.DirectionRight) }
        assertEquals("switch $MANY_SEASONS", calls.last())

        repeat(MAX_PRESSES) { if (!focusedHas("Сезон")) pressKey(Key.DirectionUp) }

        assertFocusedHas("Сезон $MANY_SEASONS")
    }

    @Test
    fun `long series opened on a far season enters the chip of that season`() {
        showSwitchableSeries(manySeasons(resumeSeason = FAR_SEASON))
        assertFocusedHas(RESUME_TITLE)

        // Чип пятнадцатого сезона при открытии за краем ряда: ряд докручивает до него сам.
        repeat(MAX_PRESSES) { if (!focusedHas("Сезон")) pressKey(Key.DirectionUp) }

        assertFocusedHas("Сезон $FAR_SEASON")
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
    fun `after a season change down leads into the new season's episodes`() {
        val item = longSeries()
        var state by mutableStateOf(seriesState(item))
        compose.setContent {
            FilmaxTvTheme {
                DetailsWithSheet(state, item, actions(onSeason = { state = state.copy(selectedSeason = it) }))
            }
        }
        compose.waitForIdle()

        // Смена сезона анимирована: старый список гаснет, новый проявляется. Фокус остаётся на
        // чипе, а «вниз» ведёт уже в серии нового сезона, а не в гаснущий список.
        repeat(MAX_PRESSES) { if (!focusedHas("Сезон 4")) pressKey(Key.DirectionUp) }
        pressKey(Key.DirectionRight)
        assertFocusedHas("Сезон 5")
        pressKey(Key.DirectionDown)
        pressKey(Key.DirectionCenter)

        assertTrue(calls.last().startsWith("play 5/"), "запущена не серия пятого сезона: ${calls.last()}")
    }

    @Test
    fun `left from the last season chip goes back through the seasons`() {
        val item = longSeries()
        var state by mutableStateOf(seriesState(item))
        compose.setContent {
            FilmaxTvTheme {
                DetailsWithSheet(state, item, actions(onSeason = { state = state.copy(selectedSeason = it) }))
            }
        }
        compose.waitForIdle()

        // До последнего сезона и обратно: «←» с чипа листает сезоны, на кнопки уводит только с первого.
        repeat(MAX_PRESSES) { if (!focusedHas("Сезон 4")) pressKey(Key.DirectionUp) }
        repeat(LAST_SEASON - RESUME_SEASON) { pressKey(Key.DirectionRight) }
        assertFocusedHas("Сезон $LAST_SEASON")
        repeat(LAST_SEASON - 1) { pressKey(Key.DirectionLeft) }
        assertFocusedHas("Сезон 1")

        pressKey(Key.DirectionLeft)

        assertFocusedHas(CONTINUE)
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
                        onSeason = { state = state.copy(selectedSeason = it) },
                    ),
                )
            }
        }
        compose.waitForIdle()

        // С серии «←» листает сезоны до первого и уводит на главную кнопку, выше — описание:
        // «ОК» открывает панель полного текста.
        repeat(MAX_PRESSES) { if (!focusedHas(CONTINUE)) pressKey(Key.DirectionLeft) }
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

    /** Сериал, у которого сезон меняется, как в модели: и фокусом на чипе, и стрелкой с серии. */
    private fun showSwitchableSeries(item: Item = longSeries()) {
        var state by mutableStateOf(seriesState(item))
        compose.setContent {
            FilmaxTvTheme {
                DetailsWithSheet(state, item, actions(onSeason = { state = state.copy(selectedSeason = it) }))
            }
        }
        compose.waitForIdle()
    }

    private fun longSeries(): Item = runBlocking {
        val catalog = FakeCatalogRepository(FakeBackend(latency = FakeLatency.None))
        checkNotNull(catalog.getItemDetails(FakeIds.LONG_SERIES).getOrNull())
    }

    /**
     * «Хроники Пограничья», растянутые до [MANY_SEASONS] сезонов копиями четвёртого — ряд чипов не
     * влезает в панель. Сезоны до [resumeSeason] досмотрены, «продолжить» — его седьмая серия.
     */
    private fun manySeasons(resumeSeason: Int): Item {
        val item = longSeries()
        val template = item.tracklist.filter { it.seasonNumber == RESUME_SEASON }
        val tracks = (1..MANY_SEASONS).flatMap { season ->
            template.map { episode ->
                val status = if (season < resumeSeason) WATCH_STATUS_FINISHED else episode.watchStatus
                episode.copy(id = season * SEASON_ID_STEP + episode.number, seasonNumber = season, watchStatus = status)
            }
        }
        return item.copy(tracklist = tracks)
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

    private fun actions(
        onOpen: () -> Unit = {},
        onClose: () -> Unit = {},
        onSeason: (Int) -> Unit = {},
    ) = DetailsActions(
        onPlay = { season, videoId -> calls += "play $season/$videoId" },
        onToggleWatchLater = { calls += "watch later" },
        onTrailer = null,
        onOpenItem = { id -> calls += "open $id" },
        onOpenPerson = { name, _ -> calls += "person $name" },
        onOpenDescription = onOpen,
        onCloseDescription = onClose,
        onSelectSeason = { number ->
            calls += "season $number"
            onSeason(number)
        },
        onSwitchSeason = { number ->
            calls += "switch $number"
            onSeason(number)
        },
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
        const val CONTINUE = "Продолжить · S4E7"

        /** Сезон серии «продолжить» и всего сезонов у «Хроник Пограничья». */
        const val RESUME_SEASON = 4
        const val LAST_SEASON = 7

        /** Сезонов у растянутого сериала — столько чипов в панель не влезает. */
        const val MANY_SEASONS = 20
        const val FAR_SEASON = 15
        const val SEASON_ID_STEP = 1000

        /** Потолок нажатий «вверх» до ряда чипов — с запасом на шесть серий над стартовой. */
        const val MAX_PRESSES = 12
    }
}
