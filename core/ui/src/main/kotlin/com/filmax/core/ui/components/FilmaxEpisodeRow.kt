package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeThumb
import com.filmax.core.designsystem.ShapeTile
import com.filmax.core.designsystem.glassFill

/** Состояние серии в списке: как её рисовать. */
enum class EpisodeMark {
    /** Ещё не смотрели. */
    Normal,

    /** Серия «продолжить» или та, что играет: подсвечена, с кнопкой «▶» и остатком. */
    Current,

    /** Досмотрена: кадр приглушён, в углу галочка. */
    Watched,
}

/**
 * Серия: кадр 16:9 с полосой прогресса, «8. Название», мета «53 мин · 26 мар» и описание. Одна
 * строка на детали сериала и панель плеера — [compact] в панели: кадр меньше, без описания.
 * Пустой кадр — номер серии вместо заглушки: у kino.pub кадры серий часто не приходят.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxEpisodeRow(
    title: String,
    meta: String?,
    thumbUrl: String,
    mark: EpisodeMark,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    placeholder: String? = null,
    compact: Boolean = false,
) {
    val current = mark == EpisodeMark.Current
    Row(
        modifier
            .fillMaxWidth()
            .clip(ShapeTile)
            .then(if (current) Modifier.background(CurrentFill).border(1.dp, CurrentBorder, ShapeTile) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top,
    ) {
        EpisodeThumb(
            url = thumbUrl,
            mark = mark,
            progress = progress,
            placeholder = placeholder,
            width = if (compact) CompactThumbWidth else ThumbWidth,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                style = FilmaxType.itemTitle,
                color = if (current) FilmaxTheme.palette.accentText else FilmaxColors.Text,
                maxLines = if (compact) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            meta?.let {
                Text(
                    it,
                    style = FilmaxType.metaSmall,
                    color = FilmaxColors.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!compact && !description.isNullOrBlank()) {
                Text(
                    description,
                    style = FilmaxType.meta,
                    color = FilmaxColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun EpisodeThumb(url: String, mark: EpisodeMark, progress: Float, placeholder: String?, width: Dp) {
    Box(
        Modifier
            .size(width = width, height = width / FRAME_ASPECT)
            .clip(ShapeThumb)
            .background(FilmaxColors.Card),
    ) {
        val dimmed = Modifier.alpha(if (mark == EpisodeMark.Watched) WATCHED_ALPHA else 1f)
        if (url.isBlank() && placeholder != null) {
            Text(
                placeholder,
                style = FilmaxType.sectionTitle,
                color = FilmaxColors.TextDim,
                modifier = Modifier.align(Alignment.Center).then(dimmed),
            )
        } else {
            PosterImage(
                url = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().then(dimmed),
                shape = ShapeThumb
            )
        }
        ThumbMark(mark)
        if (progress > 0f) {
            FilmaxProgressBar(
                progress = progress,
                thickness = FilmaxMetrics.ProgressBarThin,
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            )
        }
    }
}

/** Отметка на кадре: досмотренная — галочка в углу, текущая — «▶» по центру. */
@Composable
private fun BoxScope.ThumbMark(mark: EpisodeMark) {
    when (mark) {
        EpisodeMark.Watched -> Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(CheckFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Просмотрено",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        EpisodeMark.Current -> Box(
            Modifier
                .align(Alignment.Center)
                .size(34.dp)
                .glassFill(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        EpisodeMark.Normal -> Unit
    }
}

private val ThumbWidth = 136.dp
private val CompactThumbWidth = 112.dp
private val CurrentFill = Color.White.copy(alpha = 0.09f)
private val CurrentBorder = Color.White.copy(alpha = 0.1f)
private val CheckFill = Color.Black.copy(alpha = 0.55f)
private const val WATCHED_ALPHA = 0.65f
