package com.filmax.feature.collections.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.filmax.core.domain.error.AppError
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvCollectionTints
import com.filmax.core.tv.designsystem.TvErrorState
import com.filmax.core.tv.designsystem.preview.TvPreviews
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.appErrorText
import com.filmax.feature.collections.common.CollectionDetailEvent
import com.filmax.feature.collections.common.CollectionDetailScreenModel
import com.filmax.feature.collections.common.CollectionDetailState
import com.filmax.feature.collections.common.CollectionSamples
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Затухание сетки у нижнего края экрана. */
private val BottomFadeHeight = 70.dp

/** Слои экрана: шапка ходит под сеткой (0) и над ней (2), затухание внизу — поверх всего. */
private const val GRID_LAYER = 1f
private const val BOTTOM_FADE_LAYER = 3f

/**
 * ТВ-экран подборки: шапка с названием, числом тайтлов и «Смотреть», под ней — сетка постеров в
 * шесть колонок. Описания у подборки на ТВ нет (так в макете). Шапка сворачивается в полосу,
 * когда фокус уходит ниже первой строки, — решение принимает [CollectionDetailScreenModel].
 */
@Composable
fun TvCollectionDetailScreen(
    modifier: Modifier = Modifier,
    screenModel: CollectionDetailScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val actions = remember(screenModel, navigator) { collectionActions(screenModel, navigator) }
    TvCollectionDetailScreen(state = state, actions = actions, modifier = modifier, error = appError)
}

/**
 * Подборка из состояния — без Koin и навигатора: её рисуют превью и UI-тест фокуса. Стартовый
 * фокус — первый постер; возврат из деталей — на тот, с которого ушли.
 */
@Composable
internal fun TvCollectionDetailScreen(
    state: CollectionDetailState,
    actions: CollectionActions,
    modifier: Modifier = Modifier,
    error: AppError? = null,
) {
    // Своей заливки нет: фон — у окна (FilmaxTvTheme), под шапкой и сеткой он и так этого цвета.
    Box(modifier.fillMaxSize()) {
        when {
            state.loading -> CircularProgressIndicator(
                color = LocalFxColors.current.accent,
                modifier = Modifier.align(Alignment.Center),
            )

            // Показывать нечего и есть ошибка — объясняемся и даём «Повторить» (фокусу нужна цель).
            error != null && state.items.isEmpty() -> {
                val text = appErrorText(error)
                TvErrorState(title = text.title, message = text.message, onRetry = actions.onRetry)
            }

            else -> CollectionContent(state, actions)
        }
    }
}

/** Действия экрана одним объектом — шапка и сетка не тащат длинные списки параметров. */
internal data class CollectionActions(
    val onOpenItem: (Int) -> Unit,
    val onPlay: (Int) -> Unit,
    val onLoadMore: () -> Unit,
    /** Строка сетки с фокусом; null — фокус в шапке. */
    val onFocusRow: (Int?) -> Unit,
    val onRetry: () -> Unit,
) {
    companion object {
        /** Ничего не делает — для превью. */
        val None = CollectionActions(onOpenItem = {}, onPlay = {}, onLoadMore = {}, onFocusRow = {}, onRetry = {})
    }
}

private fun collectionActions(screenModel: CollectionDetailScreenModel, navigator: Navigator) = CollectionActions(
    onOpenItem = { itemId -> navigator.open(Destination.Details(itemId)) },
    onPlay = { itemId -> navigator.open(Destination.Player(itemId = itemId)) },
    onLoadMore = { screenModel.dispatch(CollectionDetailEvent.LoadMore) },
    onFocusRow = { row -> screenModel.dispatch(CollectionDetailEvent.FocusRow(row)) },
    onRetry = screenModel::retry,
)

/** Цвет полосы — по id подборки: у одной подборки он одинаковый при каждом входе. */
private fun headerModel(state: CollectionDetailState) = CollectionHeaderModel(
    title = state.title,
    caption = state.filmsCaption,
    cover = state.cover,
    tint = TvCollectionTints[state.collectionId.mod(TvCollectionTints.size)],
)

/** Сетка и шапка над ней: шапка ходит слоями вокруг сетки и сворачивается по решению модели. */
@Composable
private fun CollectionContent(state: CollectionDetailState, actions: CollectionActions) {
    val focus = rememberTvScreenFocus()
    val gridActions =
        remember(actions) { CollectionGridActions(actions.onOpenItem, actions.onLoadMore, actions.onFocusRow) }
    Box(Modifier.fillMaxSize()) {
        // Сетка — между слоями шапки: раскрытая лежит под постерами, свёрнутая полоса — над ними.
        CollectionGrid(state.items, state.headerCollapsed, focus, gridActions, Modifier.zIndex(GRID_LAYER))
        CollectionHeader(
            model = headerModel(state),
            collapsed = state.headerCollapsed,
            onPlay = state.startItemId?.let { itemId -> { actions.onPlay(itemId) } },
            onFocused = { actions.onFocusRow(null) },
        )
        BottomFade(Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BottomFade(modifier: Modifier) {
    val background = LocalFxColors.current.background
    Box(
        modifier
            .zIndex(BOTTOM_FADE_LAYER)
            .fillMaxWidth()
            .height(BottomFadeHeight)
            .background(Brush.verticalGradient(listOf(Color.Transparent, background))),
    )
}

private class CollectionStatePreviews : PreviewParameterProvider<CollectionDetailState> {
    override val values = CollectionSamples.all.asSequence()
}

@TvPreviews
@Composable
private fun TvCollectionDetailScreenPreview(
    @PreviewParameter(CollectionStatePreviews::class) state: CollectionDetailState,
) {
    FilmaxTvTheme { TvCollectionDetailScreen(state = state, actions = CollectionActions.None) }
}
