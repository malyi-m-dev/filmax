// Шапка деталей: кадр с медленным «Кен Бёрнсом», надзаголовок, название, мета с оценками и
// плитки действий; стеклянные кнопки поверх кадра и плавающая панель «Смотреть» внизу экрана.

package com.filmax.feature.details.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassPanel
import com.filmax.core.designsystem.GlassState
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.designsystem.radialWash
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.ui.components.FilmaxActionTile
import com.filmax.core.ui.components.FilmaxBadge
import com.filmax.core.ui.components.FilmaxGlassHeader
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxOverline
import com.filmax.core.ui.components.FilmaxPlayButton
import com.filmax.core.ui.components.FilmaxRatingPill
import com.filmax.core.ui.components.KenBurnsImage
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.FolderPickerEvent
import com.filmax.feature.details.common.SeriesData
import com.filmax.feature.details.common.hasUhd
import com.filmax.feature.details.common.volumeLabel

/** Кадр и заголовок: текст начинается на затемнённом низе кадра, а не под ним. */
@Composable
internal fun DetailsHead(item: Item, state: DetailsState, inAnyFolder: Boolean, actions: DetailsActions) {
    val series = state.series
    Box(Modifier.fillMaxWidth()) {
        DetailsBackdropImage(item)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = if (series == null) FilmaxMetrics.MovieContentTop else FilmaxMetrics.SeriesContentTop)
                .padding(horizontal = FilmaxMetrics.ScreenPadding),
        ) {
            FilmaxOverline(detailsOverline(item))
            Text(
                item.title,
                style = FilmaxType.title,
                color = FilmaxColors.Text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            originLine(item)?.let {
                Text(it, style = FilmaxType.meta, color = FilmaxColors.TextDim, modifier = Modifier.padding(top = 6.dp))
            }
            DetailsMeta(item = item, series = series, modifier = Modifier.padding(top = 12.dp))
            ActionTiles(item = item, isFav = state.isFav, inAnyFolder = inAnyFolder, actions = actions)
        }
    }
}

/** Кадр шапки уходит в фон экрана снизу, сверху — подсветка палитры и тень под статус-баром. */
@Composable
private fun DetailsBackdropImage(item: Item) {
    val palette = FilmaxTheme.palette
    Box(Modifier.fillMaxWidth().height(FilmaxMetrics.DetailsBackdropHeight)) {
        KenBurnsImage(
            url = item.posters.big.ifBlank { item.posters.wide.orEmpty() },
            contentDescription = item.title,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier
                .matchParentSize()
                .radialWash(palette.wash, centerX = 1f, centerY = 0f, radiusX = 1f, radiusY = 0.55f)
                .background(backdropFade(palette.background)),
        )
    }
}

/** Снизу — фон экрана, к 72% высоты кадр открыт; сверху — лёгкая тень под статус-бар. */
private fun backdropFade(background: Color) = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = STATUS_SHADOW_ALPHA),
    STATUS_SHADOW_END to Color.Transparent,
    OPEN_UNTIL to Color.Transparent,
    FADE_MIDDLE to background.copy(alpha = FADE_MIDDLE_ALPHA),
    FADE_END to background,
)

private const val STATUS_SHADOW_ALPHA = 0.45f
private const val STATUS_SHADOW_END = 0.18f
private const val OPEN_UNTIL = 0.28f
private const val FADE_MIDDLE = 0.62f
private const val FADE_MIDDLE_ALPHA = 0.6f
private const val FADE_END = 0.97f

/** «2021 · США» — год и страны под названием: оригинального названия kino.pub не отдаёт. */
private fun originLine(item: Item): String? = listOfNotNull(
    item.year.takeIf { it > 0 }?.toString(),
    item.country.trim().takeIf { it.isNotBlank() },
).joinToString(" · ").ifBlank { null }

/** Оценки и объём: пилюля главной оценки, КП и IMDb, длительность или сезоны, бейдж 4K. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsMeta(item: Item, series: SeriesData?, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        ratingLabel(item.rating.kinopoisk ?: item.rating.imdb)?.let { FilmaxRatingPill(it) }
        ratingLabel(item.rating.kinopoisk)?.let { MetaText("КП $it") }
        ratingLabel(item.rating.imdb)?.let { MetaText("IMDb $it") }
        volumeLabel(item, series)?.let { MetaText(it) }
        if (hasUhd(item)) FilmaxBadge("4K")
    }
}

@Composable
private fun MetaText(text: String) {
    Text(text, style = FilmaxType.meta, color = FilmaxColors.TextSecondary, maxLines = 1)
}

/**
 * Плитки действий под заголовком. «Скачать» макета не рисуем: kino.pub не отдаёт файлы для
 * офлайна, а плитка, которая ничего не скачивает, — обман.
 */
@Composable
private fun ActionTiles(item: Item, isFav: Boolean, inAnyFolder: Boolean, actions: DetailsActions) {
    // Трейлер показываем, только если url — играбельный http(s) (kino.pub отдаёт прямой HLS).
    val trailerUrl = item.trailer?.url?.takeIf { it.startsWith("http") }
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        trailerUrl?.let { url ->
            FilmaxActionTile(
                icon = Icons.Filled.Movie,
                label = "Трейлер",
                onClick = { actions.onPlayTrailer(url, "Трейлер · ${item.title}") },
                modifier = Modifier.weight(1f),
            )
        }
        FilmaxActionTile(
            icon = if (isFav) Icons.Filled.BookmarkAdded else Icons.Filled.BookmarkAdd,
            label = if (isFav) "В списке" else "Буду смотреть",
            onClick = { actions.onDetails(DetailsEvent.ToggleFav) },
            active = isFav,
            modifier = Modifier.weight(1f),
        )
        FilmaxActionTile(
            icon = if (inAnyFolder) Icons.Filled.Folder else Icons.Filled.CreateNewFolder,
            label = "В папку",
            onClick = { actions.onFolders(FolderPickerEvent.Open) },
            active = inAnyFolder,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Кнопки поверх кадра: «назад» и «поделиться». Когда заголовок уехал под статус-бар ([collapsed]),
 * под кнопками проявляется стекло с названием — лента не читается сквозь часы и кнопки.
 */
@Composable
internal fun DetailsTopBar(
    title: String,
    collapsed: Boolean,
    glass: GlassState,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = collapsed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize()
        ) {
            FilmaxGlassHeader(glass = glass, modifier = Modifier.fillMaxSize()) {}
        }
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilmaxIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", onClick = onBack)
            val titleAlpha by animateFloatAsState(if (collapsed) 1f else 0f, label = "detailsTitle")
            Text(
                title,
                style = FilmaxType.playerTitle,
                color = FilmaxColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = titleAlpha },
            )
            FilmaxIconButton(Icons.Filled.Share, contentDescription = "Поделиться", onClick = onShare)
        }
    }
}

/**
 * Плавающая панель «Смотреть»: главная кнопка с подписью, что именно сыграет, и «на телевизоре».
 * Сериал без серий играть нечем ([onPlay] = null) — панели нет вовсе.
 */
@Composable
internal fun DetailsPlayPanel(
    label: PlayLabel,
    onPlay: (() -> Unit)?,
    onPlayOnTv: (() -> Unit)?,
    glass: GlassState,
    modifier: Modifier = Modifier,
) {
    if (onPlay == null) return
    GlassPanel(
        state = glass,
        shape = ShapeFull,
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = FilmaxMetrics.GridPadding, end = FilmaxMetrics.GridPadding, bottom = PanelBottom)
            .fillMaxWidth()
            .height(FilmaxMetrics.ActionPanelHeight),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilmaxPlayButton(
                title = label.title,
                subtitle = label.subtitle,
                icon = Icons.Filled.PlayArrow,
                onClick = onPlay,
                progress = label.progress,
                modifier = Modifier.weight(1f),
            )
            FilmaxIconButton(
                Icons.Filled.Cast,
                contentDescription = "Смотреть на телевизоре",
                onClick = onPlayOnTv,
                size = FilmaxMetrics.IconButtonLarge,
            )
        }
    }
}
