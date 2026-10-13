package com.filmax.feature.library.tv

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.domain.usecase.watching.RemoveFromWatchLaterUseCase
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeFolderAppearanceRepository
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import com.filmax.feature.library.common.MineScreenModel
import com.filmax.feature.library.common.MineSegment
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
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * «Моё» с пульта на настоящей модели и фейковом бэкенде: старт на первой карточке «Продолжить»,
 * вверх — на сегменты, сегмент меняется фокусом, вниз — в сетку; долгое «ОК» убирает постер
 * и отдаёт фокус соседу; «Создать папку» открывает панель с фокусом на клавиатуре, «Назад»
 * возвращает фокус на плитку; «Очистить историю» спрашивает с фокусом на «Отмена».
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
// Нативная графика меряет текст и раскладку по-настоящему: фокус ищется по геометрии.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvMineFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val images = FakeImagesRule()

    private val remote = TvRemote(compose)
    private val calls = mutableListOf<String>()
    private val backend = FakeBackend(latency = FakeLatency.None)
    private val watching = FakeWatchingRepository(backend)
    private val favorites = FakeFavoritesRepository()

    @Before
    fun leanbackDevice() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    @Test
    fun `screen opens on the first card to continue and ok plays it with its season`() {
        showMine()
        remote.assertFocusedHas(CONTINUE_FIRST)

        remote.press(Key.DirectionCenter)

        assertEquals("play 2003 4/7", calls.last())
    }

    @Test
    fun `up goes to the segments and the segment follows focus`() {
        val model = showMine()

        remote.press(Key.DirectionUp)
        remote.assertFocusedHas("Продолжить")
        remote.press(Key.DirectionRight)

        remote.assertFocusedHas("Буду смотреть")
        assertEquals(MineSegment.WATCHLIST, model.states.value.segment)
    }

    @Test
    fun `down from a segment enters its grid`() {
        addToWatchlist(FIRST_POSTER_ID to FIRST_POSTER)
        showMine()

        selectSegment(WATCHLIST_TAB)
        remote.press(Key.DirectionDown)

        remote.assertFocusedHas(FIRST_POSTER)
        remote.press(Key.DirectionCenter)
        assertEquals("open $FIRST_POSTER_ID", calls.last())
    }

    @Test
    fun `long ok removes the poster and focus moves to its neighbour`() {
        addToWatchlist(FIRST_POSTER_ID to FIRST_POSTER, SECOND_POSTER_ID to SECOND_POSTER)
        val model = showMine()
        selectSegment(WATCHLIST_TAB)
        // Вниз — на постер под вкладкой (второй), влево — на первый.
        remote.press(Key.DirectionDown)
        remote.press(Key.DirectionLeft)
        remote.assertFocusedHas(FIRST_POSTER)

        remote.longPressOk()

        assertEquals(listOf(SECOND_POSTER_ID), model.states.value.watchlist.map { it.id })
        assertTrue(calls.none { it.startsWith("open") }, "долгое «ОК» ещё и открыло тайтл: $calls")
        remote.assertFocusedHas(SECOND_POSTER)
    }

    @Test
    fun `create folder opens the editor on the keyboard and back returns to the tile`() {
        val model = showMine()
        selectSegment(FOLDERS_TAB)
        // Вниз с «Папок» — на карточку под вкладкой; «Создать папку» — первая, левее неё.
        remote.press(Key.DirectionDown)
        remote.press(Key.DirectionLeft)
        remote.assertFocusedHas("Создать папку")

        remote.press(Key.DirectionCenter)
        assertNotNull(model.states.value.draft, "панель создания не открылась")
        remote.assertFocusedHas(FIRST_KEY)

        // «ОК» на первой клавише — первая буква названия, заглавная.
        remote.press(Key.DirectionCenter)
        assertEquals("А", model.states.value.draft?.title)

        remote.back()
        assertNull(model.states.value.draft, "«Назад» не закрыл панель")
        remote.assertFocusedHas("Создать папку")
    }

    @Test
    fun `clear history asks with focus on cancel and back keeps the history`() {
        val model = showMine()
        selectSegment(HISTORY_TAB)
        // Вправо с сегментов — действие сегмента.
        remote.press(Key.DirectionRight)
        remote.assertFocusedHas("Очистить историю")

        remote.press(Key.DirectionCenter)
        assertTrue(model.states.value.confirmClearHistory)
        remote.assertFocusedHas("Отмена")

        remote.back()
        assertTrue(!model.states.value.confirmClearHistory, "«Назад» не закрыл диалог")
        assertTrue(model.states.value.history.isNotEmpty())
        remote.assertFocusedHas("Очистить историю")
    }

    /** С карточки вверх на сегменты и вправо до нужного: сегмент выбирается фокусом. */
    private fun selectSegment(index: Int) {
        remote.press(Key.DirectionUp)
        repeat(index) { remote.press(Key.DirectionRight) }
    }

    private fun addToWatchlist(vararg titles: Pair<Int, String>) = runBlocking {
        titles.forEach { (id, title) -> favorites.add(FavoriteItem(id, title, "", 2021, 100, rating = "8.1")) }
    }

    private fun showMine(): MineScreenModel {
        val user = FakeUserRepository(backend)
        val model = MineScreenModel(
            watching = watching,
            favorites = favorites,
            folders = BookmarkFolders(user, FakeFolderAppearanceRepository()),
            removeFromWatchLater = RemoveFromWatchLaterUseCase(favorites, watching),
            clock = Clock.systemDefaultZone(),
        )
        compose.setContent {
            FilmaxTvTheme {
                val state by model.collectAsState()
                MineContent(state, actions(model))
            }
        }
        compose.waitForIdle()
        return model
    }

    private fun actions(model: MineScreenModel) = MineActions(
        onEvent = model::dispatch,
        onOpenItem = { id -> calls += "open $id" },
        onPlay = { itemId, season, videoId -> calls += "play $itemId $season/$videoId" },
        onOpenFolder = { id -> calls += "folder $id" },
    )

    private companion object {
        /** Первая карточка «Продолжить» в фикстуре — «Хроники Пограничья», начатая S4E7. */
        const val CONTINUE_FIRST = "Хроники Пограничья"
        const val FIRST_POSTER_ID = 1012
        const val FIRST_POSTER = "Первый отложенный"
        const val SECOND_POSTER_ID = 2007
        const val SECOND_POSTER = "Второй отложенный"
        const val FIRST_KEY = "а"
        const val WATCHLIST_TAB = 1
        const val FOLDERS_TAB = 2
        const val HISTORY_TAB = 3
    }
}
