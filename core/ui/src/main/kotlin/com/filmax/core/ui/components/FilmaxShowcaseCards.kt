package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapePanel
import com.filmax.core.designsystem.ShapePoster
import com.filmax.core.designsystem.angledGradient
import com.filmax.core.designsystem.oklch

/**
 * Карточка «Топ-10 недели»: контурная цифра места позади постера. Цифра нарисована обводкой, а не
 * заливкой — она задаёт ритм ряда, но не спорит с постером, который и есть содержание карточки.
 */
@Composable
fun FilmaxTopTenCard(
    rank: Int,
    title: String,
    posterUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stroke = with(LocalDensity.current) { RankStroke.toPx() }
    Box(
        modifier
            .size(FilmaxMetrics.TopTenWidth, FilmaxMetrics.TopTenHeight)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            rank.toString(),
            style = FilmaxType.rank.copy(drawStyle = Stroke(width = stroke)),
            color = RankColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-6).dp, y = 30.dp),
        )
        PosterImage(
            url = posterUrl,
            contentDescription = "$rank. $title",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(FilmaxMetrics.PosterWidth, FilmaxMetrics.PosterHeight)
                .shadow(PosterShadow, ShapePoster),
        )
    }
}

/**
 * Карточка подборки: обложка под градиентом её оттенка [hue], название и число тайтлов внизу. Оттенок
 * у каждой подборки свой — ряд подборок читается как набор разных полок, а не как одна лента.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxCollectionCard(
    title: String,
    hue: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    posterUrl: String? = null,
) {
    Box(
        modifier
            .size(FilmaxMetrics.CollectionWidth, FilmaxMetrics.CollectionHeight)
            .clip(ShapePanel)
            .background(oklch(lightness = 0.3f, chroma = 0.1f, hue = hue))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (!posterUrl.isNullOrBlank()) {
            PosterImage(
                url = posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                shape = ShapePanel
            )
        }
        Box(
            Modifier.matchParentSize().background(
                angledGradient(
                    angle = 20f,
                    0f to oklch(lightness = 0.33f, chroma = 0.15f, hue = hue, alpha = 0.95f),
                    1f to oklch(lightness = 0.55f, chroma = 0.2f, hue = hue, alpha = 0.15f),
                ),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text(
                title,
                style = FilmaxType.cardHeading,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    it,
                    style = FilmaxType.metaSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Актёр: круглое фото с тонким кольцом, имя и роль. Без фото — силуэт: у kino.pub фото нет вовсе,
 * они приходят из TMDB, и не у каждого актёра совпадение находится.
 */
@Composable
fun FilmaxCastCard(
    name: String,
    photoUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: String? = null,
) {
    Column(
        modifier
            .width(CastWidth)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier
                .size(CastPhoto)
                .clip(CircleShape)
                .background(FilmaxColors.Field)
                .border(2.dp, FilmaxColors.Field, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUrl.isNullOrBlank()) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = FilmaxColors.TextDim,
                    modifier = Modifier.size(34.dp)
                )
            } else {
                PosterImage(
                    url = photoUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape
                )
            }
        }
        Text(
            name,
            style = FilmaxType.caption,
            color = FilmaxColors.Text,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        role?.let {
            Text(
                it,
                style = FilmaxType.metaSmall,
                color = FilmaxColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Плитка раздела в шторке «Разделы»: иконка в цветной плашке и подпись. Оттенок [hue] у каждого
 * раздела свой — сетка разделов узнаётся по цвету быстрее, чем по подписи.
 */
@Composable
fun FilmaxSectionTile(
    label: String,
    icon: ImageVector,
    hue: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .height(SectionTileHeight)
            .clip(ShapePanel)
            .background(oklch(lightness = 0.45f, chroma = 0.14f, hue = hue, alpha = 0.28f))
            .border(1.dp, oklch(lightness = 0.8f, chroma = 0.1f, hue = hue, alpha = 0.22f), ShapePanel)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(SectionIconShape)
                .background(oklch(lightness = 0.66f, chroma = 0.17f, hue = hue)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, style = FilmaxType.chip, color = FilmaxColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Оттенок подборки по её id: стабилен между запусками и разводит соседние подборки по кругу. */
fun collectionHue(id: Int): Float = (id * GOLDEN_ANGLE).mod(FULL_CIRCLE)

private val RankColor = Color.White.copy(alpha = 0.6f)
private val RankStroke = 2.dp
private val PosterShadow = 18.dp
private val CastWidth = 80.dp
private val CastPhoto = 72.dp
private val SectionTileHeight = 108.dp
private val SectionIconShape = RoundedCornerShape(14.dp)

private const val GOLDEN_ANGLE = 137.508f
private const val FULL_CIRCLE = 360f
