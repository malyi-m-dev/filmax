package com.filmax.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeCard
import com.filmax.core.designsystem.ShapePoster
import com.filmax.core.designsystem.glassFill

/** Постер 2:3 — ширину задаёт место вызова, высота следует из пропорции. */
const val POSTER_ASPECT = 2f / 3f

/** Кадр 16:9: «Продолжить», кадры фильма, превью серии. */
const val FRAME_ASPECT = 16f / 9f

/**
 * Карточка-постер: ряд «В тренде», сетка каталога, «Похожие». Ширину задаёт [modifier]
 * (`width(128.dp)` в ряду, `weight(1f)` в сетке), высота — пропорция 2:3. Рейтинг — пилюлей в
 * углу постера, название и мета — под ним: накрывать постер скримом значит гасить кадр.
 * [onLongClick] — убрать из «Буду смотреть» или папки.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilmaxPosterCard(
    title: String,
    posterUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: String? = null,
    meta: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(POSTER_ASPECT)
                .clip(ShapePoster),
        ) {
            PosterImage(url = posterUrl, contentDescription = title, modifier = Modifier.fillMaxSize())
            rating?.let { FilmaxRatingPill(it, Modifier.align(Alignment.TopStart).padding(8.dp)) }
        }
        CardCaption(title = title, meta = meta, titleStyle = FilmaxType.cardTitle)
    }
}

/**
 * Карточка «Продолжить»: кадр 16:9 с полосой прогресса и стеклянной кнопкой «▶» поверх затемнённого
 * низа. [meta] несёт то, ради чего карточка существует: «S2 · осталось 18 мин».
 *
 * [placeholder] рисуется вместо кадра, когда ссылки нет вовсе: у kino.pub кадр серии часто
 * пустой, и ряд одинаковых заглушек не отличить друг от друга, а крупный номер серии отличает.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilmaxContinueCard(
    title: String,
    meta: String?,
    imageUrl: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(FRAME_ASPECT)
                .clip(ShapeCard)
                .background(FilmaxColors.Card),
        ) {
            if (imageUrl.isBlank() && placeholder != null) {
                Text(
                    placeholder,
                    style = FilmaxType.sectionTitle,
                    color = FilmaxColors.TextDim,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                PosterImage(
                    url = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    shape = ShapeCard
                )
            }
            Box(Modifier.matchParentSize().background(FrameScrim))
            PlayMark(Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 12.dp))
            FilmaxProgressBar(progress = progress, modifier = Modifier.align(Alignment.BottomStart))
        }
        CardCaption(title = title, meta = meta, titleStyle = FilmaxType.chip)
    }
}

/** Название и мета под карточкой — одна строка каждая, длинное ужимается многоточием. */
@Composable
private fun CardCaption(title: String, meta: String?, titleStyle: TextStyle) {
    Text(
        title,
        style = titleStyle,
        color = FilmaxColors.Text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 8.dp),
    )
    if (!meta.isNullOrBlank()) {
        Text(
            meta,
            style = FilmaxType.metaSmall,
            color = FilmaxColors.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Стеклянный «▶» на кадре: карточка ведёт сразу в плеер, а не в детали. */
@Composable
private fun PlayMark(modifier: Modifier = Modifier) {
    Box(modifier.size(32.dp).glassFill(CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Полоса прогресса: заливка акцентом палитры по полупрозрачному треку. */
@Composable
fun FilmaxProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    thickness: Dp = FilmaxMetrics.ProgressBarHeight,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(FilmaxColors.Track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(thickness)
                .background(FilmaxTheme.palette.accent),
        )
    }
}

/** Затемнение низа кадра под кнопкой и полосой: на светлом кадре они иначе пропадают. */
private val FrameScrim = Brush.verticalGradient(
    0.45f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.6f),
)
