package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.filmax.core.ui.components.PosterImage

private val SpotlightHeight = 196.dp
private val SpotlightShape = RoundedCornerShape(16.dp)

/** Крупная карточка выделяется сама по себе: увеличение в фокусе скромнее, чем у постера. */
private const val SPOTLIGHT_FOCUS_SCALE = 1.03f
private val SpotlightFocus = TvFocusStyle(SpotlightShape, glowRadius = 16.dp, focusedScale = SPOTLIGHT_FOCUS_SCALE)

private const val META_ALPHA = 0.8f
private val SpotlightScrim = Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))
private val SpotlightTitleStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.01).em,
)

/**
 * Что показывает крупная карточка. [title] — со стилями: поиск подсвечивает в нём совпадение с
 * запросом. [rating] — готовая строка оценки, null — без пилюли.
 */
@Immutable
data class TvSpotlight(
    val title: AnnotatedString,
    val meta: String?,
    val imageUrl: String,
    val rating: String? = null,
    val ratingHighlighted: Boolean = false,
)

/**
 * Крупная карточка 16:9 на всю ширину колонки — «Лучшее совпадение» поиска: кадр, затемнение
 * снизу и подпись на кадре — название, пилюля оценки и мета. Карточка одна на экран, поэтому
 * название крупное и в фокусе не бежит.
 */
@Composable
fun TvSpotlightCard(spotlight: TvSpotlight, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(SpotlightHeight),
        style = SpotlightFocus,
    ) {
        PosterImage(
            url = spotlight.imageUrl,
            contentDescription = spotlight.title.text,
            modifier = Modifier.fillMaxSize(),
            shape = SpotlightShape,
            accentColor = TvSurfaceContainerHigh,
        )
        Box(Modifier.matchParentSize().background(SpotlightScrim, SpotlightShape))
        SpotlightCaption(
            spotlight,
            Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun SpotlightCaption(spotlight: TvSpotlight, modifier: Modifier) {
    val colors = LocalFxColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            spotlight.title,
            style = SpotlightTitleStyle,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (spotlight.rating != null) TvRatingPill(spotlight.rating, highlighted = spotlight.ratingHighlighted)
            if (!spotlight.meta.isNullOrBlank()) {
                Text(spotlight.meta, style = FxType.meta, color = colors.text.copy(alpha = META_ALPHA), maxLines = 1)
            }
        }
    }
}
