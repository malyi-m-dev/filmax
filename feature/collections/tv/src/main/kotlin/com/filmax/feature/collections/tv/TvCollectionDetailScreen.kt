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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvCollectionTints
import com.filmax.core.tv.designsystem.TvErrorState
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.appErrorText
import com.filmax.feature.collections.common.CollectionDetailEvent
import com.filmax.feature.collections.common.CollectionDetailScreenModel
import com.filmax.feature.collections.common.CollectionDetailState
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
    collectionId: Int,
    title: String,
    modifier: Modifier = Modifier,
    screenModel: CollectionDetailScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val actions = remember(screenModel, navigator) { collectionActions(screenModel, navigator) }
    // Своей заливки нет: фон — у окна (FilmaxTvTheme), под шапкой и сеткой он и так этого цвета.
    Box(modifier.fillMaxSize()) {
        val error = appError
        when {
            state.loading -> CircularProgressIndicator(
                color = LocalFxColors.current.accent,
                modifier = Modifier.align(Alignment.Center),
            )

            // Показывать нечего и есть ошибка — объясняемся и даём «Повторить» (фокусу нужна цель).
            error != null && state.items.isEmpty() -> {
                val text = appErrorText(error)
                TvErrorState(title = text.title, message = text.message, onRetry = screenModel::retry)
            }

            else -> CollectionContent(state, headerModel(collectionId, title, state), actions)
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
)

private fun collectionActions(screenModel: CollectionDetailScreenModel, navigator: Navigator) = CollectionActions(
    onOpenItem = { itemId -> navigator.open(Destination.Details(itemId)) },
    onPlay = { itemId -> navigator.open(Destination.Player(itemId = itemId)) },
    onLoadMore = { screenModel.dispatch(CollectionDetailEvent.LoadMore) },
    onFocusRow = { row -> screenModel.dispatch(CollectionDetailEvent.FocusRow(row)) },
)

/** Цвет полосы — по id подборки: у одной подборки он одинаковый при каждом входе. */
private fun headerModel(collectionId: Int, title: String, state: CollectionDetailState) = CollectionHeaderModel(
    title = title,
    caption = titlesCaption(state.titlesCount),
    cover = state.cover,
    tint = TvCollectionTints[collectionId.mod(TvCollectionTints.size)],
)

/**
 * Сетка и шапка над ней. internal — UI-тест фокуса собирает экран из состояния, без Koin и
 * ScreenModel. Стартовый фокус — первый постер; возврат из деталей — на тот, с которого ушли.
 */
@Composable
internal fun CollectionContent(
    state: CollectionDetailState,
    header: CollectionHeaderModel,
    actions: CollectionActions
) {
    val focus = rememberTvScreenFocus()
    val gridActions =
        remember(actions) { CollectionGridActions(actions.onOpenItem, actions.onLoadMore, actions.onFocusRow) }
    Box(Modifier.fillMaxSize()) {
        // Сетка — между слоями шапки: раскрытая лежит под постерами, свёрнутая полоса — над ними.
        CollectionGrid(state.items, state.headerCollapsed, focus, gridActions, Modifier.zIndex(GRID_LAYER))
        CollectionHeader(
            model = header,
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
