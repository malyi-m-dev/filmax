package com.filmax.feature.collections.tv

import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.feature.collections.common.CollectionDetailEvent
import com.filmax.feature.collections.common.CollectionDetailState
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
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Фокус подборки с пульта: старт на первом постере, шапка сворачивается при уходе на вторую строку
 * и раскрывается на первой, «вверх» с первой строки — на «Смотреть». Строку с фокусом тест
 * принимает так же, как модель, — переписывает её в состоянии. Подборка — «Зрители рекомендуют»
 * из фейкового бэкенда, первая страница (20 тайтлов).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvCollectionFocusTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val images = FakeImagesRule()

    private val calls = mutableListOf<String>()
    private lateinit var items: List<Item>
    private var state by mutableStateOf(CollectionDetailState())

    @Before
    fun setUp() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        items = runBlocking {
            val catalog = FakeCatalogRepository(FakeBackend(latency = FakeLatency.None))
            checkNotNull(catalog.getCollectionItems(FakeIds.LARGE_COLLECTION).getOrNull()).items
        }
        state = CollectionDetailState(collectionId = FakeIds.LARGE_COLLECTION, title = TITLE)
            .withFirstPage(items, hasNextPage = true, cover = null)
        compose.setContent {
            FilmaxTvTheme { CollectionContent(state, ::record) }
        }
        compose.waitForIdle()
    }

    @Test
    fun `screen opens with focus on the first poster and the header expanded`() {
        assertFocusedHas(items[0].title)
        assertFalse(state.headerCollapsed)
    }

    @Test
    fun `header collapses on the second row and expands back on the first`() {
        pressKey(Key.DirectionDown)

        assertFocusedHas(items[COLLECTION_COLUMNS].title)
        assertEquals(1, state.focusedRow)
        assertTrue(state.headerCollapsed, "шапка не свернулась на второй строке")
        assertFocusedTopNear(COLLAPSED_PIVOT)

        pressKey(Key.DirectionUp)

        assertFocusedHas(items[0].title)
        assertFalse(state.headerCollapsed, "шапка не раскрылась на первой строке")
        assertFocusedTopNear(GridTop)
    }

    /** Строка с фокусом стоит на опорной точке: верх постера — у [expected] (с поправкой на масштаб фокуса). */
    private fun assertFocusedTopNear(expected: Dp) {
        val top = compose.onNode(isFocused()).getBoundsInRoot().top
        assertTrue(
            abs((top - expected).value) <= PIVOT_TOLERANCE.value,
            "верх строки с фокусом на $top, а не у $expected"
        )
    }

    @Test
    fun `up from the first row goes to the play button and keeps the header expanded`() {
        pressKey(Key.DirectionUp)

        assertFocusedHas(PLAY)
        assertEquals(null, state.focusedRow)
        pressKey(Key.DirectionCenter)
        assertEquals("play", calls.last())
    }

    @Test
    fun `ok on a poster opens its title`() {
        pressKey(Key.DirectionRight)
        pressKey(Key.DirectionCenter)

        assertEquals("open ${items[1].id}", calls.last())
    }

    /** Строку с фокусом тест принимает так же, как модель; остальные события только записывает. */
    private fun record(event: CollectionDetailEvent) {
        when (event) {
            is CollectionDetailEvent.FocusRow -> state = state.withFocusedRow(event.row)
            is CollectionDetailEvent.OpenItem -> calls += "open ${event.itemId}"
            CollectionDetailEvent.PlayAll -> calls += "play"
            CollectionDetailEvent.LoadMore -> calls += "load more"
            CollectionDetailEvent.Back -> calls += "back"
        }
    }

    private fun focusedMatcher(text: String): SemanticsMatcher {
        val labelled = hasText(text, substring = true) or hasContentDescription(text, substring = true)
        return isFocused() and (labelled or hasAnyDescendant(labelled))
    }

    private fun focusedHas(text: String): Boolean =
        compose.onAllNodes(focusedMatcher(text)).fetchSemanticsNodes().isNotEmpty()

    private fun assertFocusedHas(text: String) {
        assertTrue(focusedHas(text), "в фокусе не «$text», а ${focusedTexts()}")
    }

    /** Тексты узла в фокусе — для сообщения о провале: куда фокус ушёл на самом деле. */
    private fun focusedTexts(): List<String> = compose.onAllNodes(isFocused(), useUnmergedTree = false)
        .fetchSemanticsNodes()
        .flatMap { node ->
            node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } +
                node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
        }

    @OptIn(ExperimentalTestApi::class)
    private fun pressKey(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private companion object {
        const val PLAY = "Смотреть"
        val COLLAPSED_PIVOT = 112.dp

        /** Масштаб 1.06 поднимает верх постера на ~6dp, остальное — округления раскладки. */
        val PIVOT_TOLERANCE = 10.dp
        const val TITLE = "Зрители рекомендуют"
    }
}
