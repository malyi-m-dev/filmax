package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Группа карточек, где соседи карточки в фокусе приглушаются — сетка каталога, подборки, «Моё»,
 * ряд серий. Пока фокус внутри группы, карточки с [tvNeighborDim] без фокуса уходят в
 * [TvMetrics.DimmedAlpha]; фокус ушёл из группы — все снова яркие. Ряды [TvRail] — та же группа.
 */
@Composable
fun TvDimGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.onFocusChanged { focused = it.hasFocus }) {
        CompositionLocalProvider(LocalRailFocused provides focused, content = content)
    }
}

/**
 * Приглушение карточки, когда фокус у соседа в той же группе ([TvDimGroup], [TvRail]).
 * [focused] — фокус на самой карточке. Прозрачность читается в слое: смена фокуса в группе не
 * перекомпонует карточки.
 */
@Composable
fun Modifier.tvNeighborDim(focused: Boolean): Modifier {
    val groupFocused = LocalRailFocused.current
    val alpha by animateFloatAsState(
        targetValue = if (groupFocused && !focused) TvMetrics.DimmedAlpha else 1f,
        animationSpec = FxMotion.focus(),
        label = "neighborDim",
    )
    return graphicsLayer { this.alpha = alpha }
}
