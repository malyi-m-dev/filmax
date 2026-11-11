package com.filmax.feature.details.mobile

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.RatingSource
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.testing.FakeImagesRule
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.feature.details.common.DetailsState
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

/**
 * Шапка деталей телефона: плитка «В папку» есть только у источника с папками, а оценки
 * подписаны своим источником — у домашнего сервера нет КиноПоиска, и экран его не выдумывает.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DetailsHeadTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val images = FakeImagesRule()

    private val calls = mutableListOf<String>()
    private val catalog = FakeCatalogRepository(FakeBackend(latency = FakeLatency.None))
    private val movie: Item = runBlocking { checkNotNull(catalog.getItemDetails(FakeIds.HERO_MOVIE).getOrNull()) }

    @Test
    fun `kinopub title offers the folder tile and both external ratings`() {
        show(movie, DetailsState(item = movie, foldersAvailable = true))

        compose.onNodeWithText("В папку").performClick()
        compose.onNodeWithText("Буду смотреть").performClick()

        assertEquals(listOf("folders Open", "details ToggleFav"), calls)
        compose.onAllNodesWithText("КП", substring = true).assertCountEquals(1)
    }

    @Test
    fun `home server title has no folder tile and shows only its own rating`() {
        val serverMovie = movie.copy(rating = ItemRating.of(RatingSource.COMMUNITY to "7.5"))

        show(serverMovie, DetailsState(item = serverMovie, foldersAvailable = false))

        compose.onAllNodesWithText("В папку").assertCountEquals(0)
        compose.onAllNodesWithText("КП", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Рейтинг 7.5").assertExists()
    }

    private fun show(item: Item, state: DetailsState) {
        compose.setContent {
            FilmaxTheme { DetailsHead(item = item, state = state, inAnyFolder = false, actions = actions()) }
        }
        compose.waitForIdle()
    }

    private fun actions() = DetailsActions(
        onBack = {},
        onPlay = {},
        onPlayOnTv = {},
        onShare = {},
        onOpenItem = {},
        onOpenPerson = { _, _ -> },
        onPlayTrailer = { _, _ -> },
        onDetails = { event -> calls += "details ${event.name()}" },
        onFolders = { event -> calls += "folders ${event.name()}" },
    )

    private fun Any.name(): String = this::class.simpleName.orEmpty()
}
