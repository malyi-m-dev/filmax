// Шапка подборки телефона: обложка, размытая в цвет фона, веер постеров, название с числом тайтлов
// и «Смотреть подряд»; поверх ленты — «Назад» и стеклянная полоса с названием, когда шапка уехала.
package com.filmax.feature.collections.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassPanel
import com.filmax.core.designsystem.GlassState
import com.filmax.core.designsystem.oklch
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonSize
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxOverline
import com.filmax.core.ui.components.FilmaxPosterFan
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.bleed
import com.filmax.feature.collections.common.CollectionDetailState

/**
 * Шапка: обложка, размытая в цвет фона, поверх — градиент оттенка подборки и веер первых постеров.
 * Обложки нет — остаётся градиент, веер держит шапку сам.
 */
@Composable
internal fun CollectionHeader(hue: Float, state: CollectionDetailState, onPlay: (() -> Unit)?) {
    Box(Modifier.fillMaxWidth().bleed(FilmaxMetrics.GridPadding)) {
        CollectionBackdrop(cover = state.cover, hue = hue)
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 64.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FilmaxPosterFan(
                posters = state.fanPosters,
                centerWidth = 120.dp,
                sideWidth = 104.dp,
                angle = 9f,
                step = 74.dp,
                modifier = Modifier.height(FanHeight),
            )
            CollectionHeading(title = state.title, hue = hue, caption = state.titlesCaption)
            onPlay?.let {
                FilmaxButton(
                    text = "Смотреть подряд",
                    onClick = it,
                    icon = Icons.Filled.PlayArrow,
                    size = FilmaxButtonSize.Compact,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

/** Фон шапки: обложка, размытая до пятна, под градиентом оттенка подборки в фон экрана. */
@Composable
private fun BoxScope.CollectionBackdrop(cover: String?, hue: Float) {
    val background = FilmaxTheme.palette.background
    cover?.let {
        PosterImage(
            url = it,
            contentDescription = null,
            modifier = Modifier.matchParentSize().blur(CoverBlur),
            shape = RectangleShape
        )
    }
    Box(
        Modifier.matchParentSize().background(
            Brush.verticalGradient(
                listOf(
                    oklch(lightness = 0.42f, chroma = 0.18f, hue = hue, alpha = 0.55f),
                    background.copy(alpha = BACKDROP_BOTTOM_ALPHA),
                ),
            ),
        ),
    )
}

/** Надзаголовок оттенком подборки, название и — когда известно точно — число тайтлов. */
@Composable
private fun CollectionHeading(title: String, hue: Float, caption: String?) {
    FilmaxOverline(
        "Подборка редакции",
        color = oklch(lightness = 0.85f, chroma = 0.1f, hue = hue),
        modifier = Modifier.padding(top = 20.dp),
    )
    Text(
        title,
        style = FilmaxType.collectionTitle,
        color = FilmaxColors.Text,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
    )
    caption?.let {
        Text(
            it,
            style = FilmaxType.meta,
            color = FilmaxColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** «Назад» поверх шапки; шапка уехала — стеклянная полоса оттенка подборки с её названием. */
@Composable
internal fun CollectionTopBar(title: String, hue: Float, collapsed: Boolean, glass: GlassState, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = collapsed, enter = fadeIn(), exit = fadeOut()) {
            GlassPanel(state = glass, modifier = Modifier.fillMaxWidth(), border = Color.Transparent) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(oklch(lightness = 0.42f, chroma = 0.18f, hue = hue, alpha = 0.22f)),
                )
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().height(
                        FilmaxMetrics.HeaderRowHeight
                    ).padding(start = 68.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        style = FilmaxType.playerTitle,
                        color = FilmaxColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        FilmaxIconButton(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp),
        )
    }
}

private val CoverBlur = 40.dp
private val FanHeight = 196.dp
private const val BACKDROP_BOTTOM_ALPHA = 0.96f
