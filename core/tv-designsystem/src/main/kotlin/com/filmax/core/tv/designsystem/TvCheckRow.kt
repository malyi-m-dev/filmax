// Строки списка с отметками в боковой панели («В папку»): плитка-иконка, название, подпись и
// чекбокс справа; последней строкой — «Создать …» с пунктирной плиткой.
package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

private val RowHeight = 56.dp
private val TileSize = 34.dp
private val TileShape = RoundedCornerShape(10.dp)
private val BoxSize = 24.dp
private val BoxShape = RoundedCornerShape(7.dp)

/** Строка — стекло 5%, в фокусе светлее: фокус держат рамка и подъём фона, увеличение лишь 1.02. */
private const val ROW_ALPHA = 0.05f
private const val ROW_FOCUSED_ALPHA = 0.14f
private const val BOX_BORDER_ALPHA = 0.28f

/** Что показывает строка: название, подпись под ним, иконка на плитке цвета [tile]. */
@Immutable
data class TvCheckRowSpec(val title: String, val subtitle: String?, val icon: ImageVector, val tile: Color)

/**
 * Строка с отметкой: «ОК» переключает [checked] сразу, без подтверждения, — галочка выпрыгивает
 * с перелётом ([FxMotion.pop]).
 */
@Composable
fun TvCheckRow(spec: TvCheckRowSpec, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PanelRow(onClick, modifier) {
        Box(Modifier.size(TileSize).background(spec.tile, TileShape), contentAlignment = Alignment.Center) {
            Icon(spec.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(spec.title, style = FxType.titleOnImage, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (spec.subtitle != null) {
                Text(spec.subtitle, style = FxType.metaOnImage, color = LocalFxColors.current.textSecondary)
            }
        }
        CheckBox(checked)
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
    val colors = LocalFxColors.current
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (LocalReducedMotion.current) snap() else FxMotion.pop(),
        label = "checkPop",
    )
    Box(
        Modifier
            .size(BoxSize)
            .then(
                if (checked) {
                    Modifier.background(colors.accent, BoxShape)
                } else {
                    Modifier.border(2.dp, Color.White.copy(alpha = BOX_BORDER_ALPHA), BoxShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Отмечено",
                tint = colors.onAccent,
                modifier = Modifier.size(17.dp).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            )
        }
    }
}

/** Последняя строка списка — «Создать папку»: пунктирная плитка с «＋» и подпись акцентом. */
@Composable
fun TvAddRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    PanelRow(onClick, modifier) {
        Box(
            Modifier.size(TileSize).border(2.dp, colors.accentText, TileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(label, style = FxType.titleOnImage, color = colors.accentText)
    }
}

@Composable
private fun PanelRow(onClick: () -> Unit, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(RowHeight),
        style = TvFocusStyle.Row,
        colors = TvSurfaceColors(
            container = Color.White.copy(alpha = ROW_ALPHA),
            content = colors.text,
            focusedContainer = Color.White.copy(alpha = ROW_FOCUSED_ALPHA),
        ),
    ) {
        Row(
            Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}
