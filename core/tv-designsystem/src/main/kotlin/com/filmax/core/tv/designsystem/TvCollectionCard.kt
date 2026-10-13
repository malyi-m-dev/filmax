package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.ui.components.PosterImage

/**
 * Цвета плашек подборок — одна яркость, разные оттенки (oklch 0.42 / 0.12 из макета), чтобы
 * ряд подборок читался пёстрым, но ровным. Подборке цвет достаётся по её месту в ряду.
 */
val TvCollectionTints = listOf(
    Color(0xFF004F8B),
    Color(0xFF822D22),
    Color(0xFF005E26),
    Color(0xFF70306C),
    Color(0xFF753D00),
    Color(0xFF005F67),
)

private val CardShape = RoundedCornerShape(14.dp)
private val PosterShape = RoundedCornerShape(7.dp)
private val TextColumnWidth = 120.dp

/** Задний и передний постеры веера: размер, отступ от правого и верхнего края, наклон. */
private data class FanPoster(val width: Dp, val height: Dp, val end: Dp, val top: Dp, val degrees: Float)

private val BackPoster = FanPoster(70.dp, 105.dp, 70.dp, 28.dp, -8f)
private val FrontPoster = FanPoster(76.dp, 114.dp, 16.dp, 18.dp, 6f)

/**
 * Карточка подборки: цветная плашка 260×140, название и подпись слева, справа — веер из двух
 * постеров подборки. [caption] — число тайтлов («24 фильма»), null — если оно неизвестно.
 */
// Компонент дизайн-системы: параметры — его публичный API (содержимое, цвет, хвост опций).
@Suppress("LongParameterList")
@Composable
fun TvCollectionCard(
    title: String,
    caption: String?,
    posters: List<String>,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier
            .size(TvMetrics.CollectionCardWidth, TvMetrics.CollectionCardHeight)
            .tvNeighborDim(focused),
        style = TvFocusStyle(CardShape, glowRadius = 14.dp, focusedScale = TvMetrics.FocusScale, pop = true),
        colors = TvSurfaceColors(container = tint, content = Color.White),
        interactionSource = interaction,
    ) {
        Column(Modifier.padding(16.dp).width(TextColumnWidth)) {
            Text(title, style = FxType.collectionTitle, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (caption != null) {
                Text(
                    caption,
                    style = FxType.metaOnImage,
                    color = Color.White.copy(alpha = CAPTION_ALPHA),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        posters.getOrNull(1)?.let { FannedPoster(it, BackPoster) }
        posters.getOrNull(0)?.let { FannedPoster(it, FrontPoster) }
    }
}

private const val CAPTION_ALPHA = 0.75f

@Composable
private fun BoxScope.FannedPoster(url: String, poster: FanPoster) {
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .offset(x = -poster.end, y = poster.top)
            .size(poster.width, poster.height)
            .rotate(poster.degrees)
            .shadow(PosterShadow, PosterShape)
            .background(TvSurfaceContainerHigh, PosterShape),
    ) {
        PosterImage(url = url, contentDescription = null, modifier = Modifier.fillMaxSize(), shape = PosterShape)
    }
}

private val PosterShadow = 8.dp
