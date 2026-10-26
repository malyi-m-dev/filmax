package com.filmax.feature.home.tv

import android.content.pm.PackageManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeRow
import com.filmax.feature.home.common.HomeRowId
import com.filmax.feature.home.common.HomeState
import com.filmax.feature.home.common.RowPaging
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
 * ТВ-лента целиком — все четырнадцать рядов, с тематическими. Ряды идут обычным путём (TvRail +
 * tvRailEntry), а возврат на экран — rememberTvScreenFocus: в каждом ряду одни и те же id, как у
 * живой ленты, где хит стоит и в «Тренде», и в «Комедиях», — и фокус должен вернуться в тот ряд,
 * откуда ушли, а не в первый с таким id.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvHomeThemeRowsFocusTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val images = FakeImagesRule()

    private val restoration = StateRestorationTester(compose)
    private var played = false

    @Before
    fun setUp() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        restoration.setContent {
            FilmaxTvTheme {
                TvHomeFeed(state = fullFeed(), offline = false, onEvent = { if (it is HomeEvent.Play) played = true })
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `long feed still opens with focus on the hero`() {
        pressKey(Key.DirectionCenter)

        assertTrue(played, "стартовый фокус не на «Смотреть» в hero")
    }

    @Test
    fun `down walks row by row to the last theme row`() {
        repeat(ROW_IDS.size) { pressKey(Key.DirectionDown) }
        pressKey(Key.DirectionRight)

        assertFocusedHas(cardTitle(HomeRowId.CONCERTS, SECOND_CARD_ID))
    }

    @Test
    fun `after leaving the screen focus comes back to the card it left`() {
        repeat(ROW_IDS.size) { pressKey(Key.DirectionDown) }
        pressKey(Key.DirectionRight)

        // Уход в детали и возврат: композиция снята и собрана заново из сохранённого состояния.
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        // Ряд находят ключи «ряд:id» (id в рядах одни и те же), карточку в ряду — память TvRail:
        // она переживает пересоздание экрана, иначе возврат вёл бы на первую карточку ряда.
        assertFocusedHas(cardTitle(HomeRowId.CONCERTS, SECOND_CARD_ID))
    }

    /** Карточка в фокусе — поверхность с кадром: название у неё в описании кадра, подпись — соседний узел. */
    private fun assertFocusedHas(text: String, substring: Boolean = false) {
        val labelled = hasText(text, substring) or hasContentDescription(text, substring)
        val focused = isFocused() and (labelled or hasAnyDescendant(labelled))
        assertTrue(
            compose.onAllNodes(focused).fetchSemanticsNodes().isNotEmpty(),
            "в фокусе не «$text», а ${focusedTexts()}",
        )
    }

    /** Тексты и описания узла в фокусе и его потомков — для сообщения о провале: где фокус на самом деле. */
    private fun focusedTexts(): List<String> = compose.onAllNodes(isFocused() or hasFocusedAncestor())
        .fetchSemanticsNodes()
        .flatMap { node ->
            node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } +
                node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
        }

    private fun hasFocusedAncestor(): SemanticsMatcher =
        SemanticsMatcher("предок в фокусе") { node ->
            generateSequence(node.parent) { it.parent }
                .any { it.config.getOrElse(SemanticsProperties.Focused) { false } }
        }

    @OptIn(ExperimentalTestApi::class)
    private fun pressKey(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private companion object {
        /** Ряды ленты в порядке модели: пять из фида и девять тематических. */
        val ROW_IDS = HomeRowId.entries

        /** Id карточек — одни и те же в каждом ряду. */
        val CARD_IDS = 10..15
        const val SECOND_CARD_ID = 11

        fun cardTitle(row: HomeRowId, id: Int) = "${row.name} $id"

        fun fullFeed(): HomeState {
            val hero = item(id = 1, title = "Дом на краю карты")
            return HomeState(loading = false, heroes = listOf(hero), hero = hero, rows = ROW_IDS.map(::row))
        }

        fun row(id: HomeRowId): HomeRow = when (id) {
            HomeRowId.CONTINUE -> HomeRow.Continue(CARD_IDS.map(::started))
            HomeRowId.COLLECTIONS -> HomeRow.Collections(RowPaging(CARD_IDS.map(::collection)))
            else -> {
                val cards = CARD_IDS.map { item(id = it, title = cardTitle(id, it)) }
                HomeRow.Titles(id, title = id.name, subtitle = null, paging = RowPaging(cards))
            }
        }

        fun started(id: Int) = WatchHistory(itemId = id, title = "Начато $id", posterSmall = "", progress = null)

        fun collection(id: Int) = Collection(id = id, title = "Подборка $id", description = null, posters = null)

        fun item(id: Int, title: String) = Item(
            id = id,
            title = title,
            type = ItemType.MOVIE,
            year = 2024,
            plot = "",
            director = "",
            cast = "",
            country = "",
            genres = emptyList(),
            rating = ItemRating(filmax = 0, filmaxPercentage = "", imdb = "7.5", kinopoisk = "7.9"),
            posters = Posters(small = "", medium = "", big = "", wide = null),
            duration = Duration(averageMinutes = 100.0, totalMinutes = null),
            tracklist = emptyList(),
            trailer = null,
            inWatchlist = false,
            finished = false,
        )
    }
}
