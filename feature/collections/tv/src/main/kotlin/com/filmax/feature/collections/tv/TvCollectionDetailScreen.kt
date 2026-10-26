package com.filmax.feature.collections.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvCollectionTints
import com.filmax.core.tv.designsystem.TvErrorState
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.appErrorText
import com.filmax.feature.collections.common.CollectionDetailEvent
import com.filmax.feature.collections.common.CollectionDetailScreenModel
import com.filmax.feature.collections.common.CollectionDetailState
import org.koin.androidx.compose.koinViewModel

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
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
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

            else -> CollectionContent(state, screenModel::dispatch)
        }
    }
}

/**
 * Сетка и шапка над ней. internal — UI-тест фокуса собирает экран из состояния, без Koin и
 * ScreenModel. Стартовый фокус — первый постер; возврат из деталей — на тот, с которого ушли.
 * Цвет полосы — по id подборки: у одной подборки он одинаковый при каждом входе.
 */
@Composable
internal fun CollectionContent(state: CollectionDetailState, onEvent: (CollectionDetailEvent) -> Unit) {
    val focus = rememberTvScreenFocus()
    val tint = TvCollectionTints[state.collectionId.mod(TvCollectionTints.size)]
    Box(Modifier.fillMaxSize()) {
        // Сетка — между слоями шапки: раскрытая лежит под постерами, свёрнутая полоса — над ними.
        CollectionGrid(state.items, state.headerCollapsed, focus, onEvent, Modifier.zIndex(GRID_LAYER))
        CollectionHeader(state = state, tint = tint, onEvent = onEvent)
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
