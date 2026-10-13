package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.ui.components.PosterImage

private val ThumbWidth = 112.dp
private val ThumbHeight = 63.dp
private val ThumbShape = RoundedCornerShape(9.dp)
private const val WATCHED_ALPHA = 0.6f

/** Что строка серии знает о серии — готовые строки и доли, считает их вызывающий. */
data class TvEpisodeRowModel(
    val title: String,
    val meta: String?,
    val imageUrl: String,
    /** Доля просмотренного 0..1, null — серию не начинали. */
    val progress: Float?,
    val watched: Boolean,
    /** Серия «продолжить»: мета акцентом. */
    val resume: Boolean,
)

/**
 * Строка серии в панели «Эпизоды»: кадр 112×63 с полосой прогресса, название и мета.
 * Досмотренная — приглушена и с галочкой в акцентном круге; серия «продолжить» — мета акцентом;
 * в фокусе на кадре играет эквалайзер. Фокус — как у широких строк: 1.02 и рамка со свечением.
 */
@Composable
fun TvEpisodeRow(model: TvEpisodeRowModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().alpha(if (model.watched && !focused) WATCHED_ALPHA else 1f),
        style = TvFocusStyle.Row,
        colors = TvSurfaceColors(
            container = Color.Transparent,
            content = colors.text,
            focusedContainer = colors.surfaceHigh
        ),
        interactionSource = interaction,
    ) {
        Row(
            Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EpisodeThumb(model, playing = focused)
            Column(Modifier.weight(1f)) {
                Text(model.title, style = FxType.titleOnImage, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (model.meta != null) {
                    Text(
                        model.meta,
                        style = FxType.metaOnImage,
                        color = if (model.resume) colors.accentText else colors.textSecondary,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            if (model.watched) WatchedMark()
        }
    }
}

@Composable
private fun EpisodeThumb(model: TvEpisodeRowModel, playing: Boolean) {
    Box(Modifier.size(ThumbWidth, ThumbHeight).background(TvSurfaceContainerHigh, ThumbShape)) {
        PosterImage(
            url = model.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            shape = ThumbShape
        )
        if (playing && !LocalReducedMotion.current) {
            Equalizer(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 9.dp))
        }
        model.progress?.let { TvProgressBar(it, Modifier.align(Alignment.BottomStart)) }
    }
}

@Composable
private fun WatchedMark() {
    val colors = LocalFxColors.current
    Box(Modifier.size(22.dp).background(colors.accent, CircleShape), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = "Просмотрено",
            tint = colors.onAccent,
            modifier = Modifier.size(14.dp)
        )
    }
}

/** Периоды полос эквалайзера — разные, чтобы полосы не прыгали в такт. */
private val EqualizerPeriods = listOf(420, 560, 490)
private const val EQUALIZER_MIN = 0.25f

/** Эквалайзер на кадре серии в фокусе: три полосы, «это включится по OK». */
@Composable
private fun Equalizer(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        EqualizerPeriods.forEach { period ->
            val scale by transition.animateFloat(
                initialValue = EQUALIZER_MIN,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(period), RepeatMode.Reverse),
                label = "equalizerBar",
            )
            Box(
                Modifier
                    .size(width = 3.dp, height = 12.dp)
                    .graphicsLayer {
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(Color.White),
            )
        }
    }
}
