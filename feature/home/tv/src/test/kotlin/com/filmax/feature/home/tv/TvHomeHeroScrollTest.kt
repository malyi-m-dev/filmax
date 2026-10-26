package com.filmax.feature.home.tv

import android.content.pm.PackageManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
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
import kotlin.test.assertTrue

/**
 * Баннер «Выбор редакции» на ТВ-Главной не должен оставаться срезанным сверху. Ряды встают на
 * опорную точку, а hero — высокий блок — прижимается к самому верху ([PivotBringIntoViewSpec]):
 * раньше фокус-прокрутка поднимала в кадр только кнопку, и hero над ней уезжал за край.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class TvHomeHeroScrollTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val images = FakeImagesRule()

    /** ТВ-устройство: на нём Compose включает фокус-прокрутку с «опорной точкой», как на боксе. */
    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `hero is fully visible again when focus comes back to it from the rows`() {
        compose.setContent {
            FilmaxTvTheme { TvHomeFeed(state = homeState(), offline = false, onEvent = {}) }
        }
        compose.waitForIdle()

        repeat(ROWS_TO_DIVE) { pressKey(Key.DirectionDown) }
        repeat(ROWS_TO_DIVE) { pressKey(Key.DirectionUp) }

        compose.onNode(isFocused()).assertExists()
        val heroTop = heroTop()
        assertTrue(heroTop >= -ROUNDING_PX, "баннер срезан сверху на ${-heroTop}px")
    }

    @Test
    fun `up from the first row returns focus to the hero`() {
        var played = false
        compose.setContent {
            FilmaxTvTheme {
                TvHomeFeed(state = homeState(), offline = false, onEvent = { if (it is HomeEvent.Play) played = true })
            }
        }
        compose.waitForIdle()

        // «Вверх» с ряда — обратно на ту кнопку hero, с которой ушли, а не на ближайшую к
        // карточке («Подробнее» под широкой карточкой ближе).
        pressKey(Key.DirectionDown)
        pressKey(Key.DirectionUp)
        pressKey(Key.DirectionCenter)

        assertTrue(played, "«вверх» с первого ряда не вернул фокус на «Смотреть» в hero")
    }

    @Test
    fun `hero is fully visible on the first open`() {
        compose.setContent {
            FilmaxTvTheme { TvHomeFeed(state = homeState(), offline = false, onEvent = {}) }
        }
        compose.waitForIdle()

        val heroTop = heroTop()
        assertTrue(heroTop >= -ROUNDING_PX, "баннер срезан сверху на ${-heroTop}px")
    }

    /**
     * Верх баннера без обрезки: boundsInRoot режется краем экрана и ниже нуля не уходит никогда —
     * на нём тест проходил бы и со срезанным баннером.
     */
    private fun heroTop(): Float = compose.onNodeWithTag(HERO_TEST_TAG).fetchSemanticsNode().positionInRoot.y

    @OptIn(ExperimentalTestApi::class)
    private fun pressKey(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private companion object {
        /**
         * Допуск на округление прокрутки до пикселя (параллакс hero добавляет половину). Ловим
         * срезанный баннер — десятки и сотни пикселей, а не субпиксельный шум.
         */
        const val ROUNDING_PX = 1f

        /** Сколько рядов вниз уйти от hero: глубже экрана, чтобы лента прокрутилась. */
        const val ROWS_TO_DIVE = 3

        fun homeState(): HomeState {
            val hero = item(id = 1, title = "Дом на краю карты")
            val trending = RowPaging((10..17).map { item(id = it, title = "Тайтл $it") })
            val forYou = RowPaging((20..27).map { item(id = it, title = "Сериал $it") })
            return HomeState(
                loading = false,
                heroes = listOf(hero),
                hero = hero,
                rows = listOf(
                    HomeRow.Continue((40..45).map(::started)),
                    HomeRow.Titles(HomeRowId.TRENDING, "В тренде", "на этой неделе", trending),
                    HomeRow.Titles(HomeRowId.FOR_YOU, "Сериалы с высоким рейтингом", null, forYou),
                ),
            )
        }

        fun started(id: Int) = WatchHistory(itemId = id, title = "Начато $id", posterSmall = "", progress = null)

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
