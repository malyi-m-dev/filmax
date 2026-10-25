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
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsHeading
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.FolderPickerEvent
import com.filmax.feature.details.common.PlayButton

/** Кадр и заголовок: текст начинается на затемнённом низе кадра, а не под ним. */
@Composable
internal fun DetailsHead(
    item: Item,
    state: DetailsState,
    inAnyFolder: Boolean,
    onEvent: (DetailsEvent) -> Unit,
    onFolders: (FolderPickerEvent) -> Unit,
) {
    val heading = state.heading
    val contentTop = if (state.series == null) FilmaxMetrics.MovieContentTop else FilmaxMetrics.SeriesContentTop
    Box(Modifier.fillMaxWidth()) {
        DetailsBackdropImage(item)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = contentTop)
                .padding(horizontal = FilmaxMetrics.ScreenPadding),
        ) {
            FilmaxOverline(heading.typeAndGenre)
            Text(
                item.title,
                style = FilmaxType.title,
                color = FilmaxColors.Text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            heading.originLine?.let {
                Text(it, style = FilmaxType.meta, color = FilmaxColors.TextDim, modifier = Modifier.padding(top = 6.dp))
            }
            DetailsMeta(heading = heading, modifier = Modifier.padding(top = 12.dp))
            ActionTiles(
                trailerUrl = state.trailerUrl,
                isFav = state.isFav,
                inAnyFolder = inAnyFolder,
                onEvent = onEvent,
                onFolders = onFolders,
            )
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

/** Оценки и объём: пилюля главной оценки, КП и IMDb, длительность или сезоны, бейдж 4K. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsMeta(heading: DetailsHeading, modifier: Modifier = Modifier) {
    val kinopoisk = heading.ratingBadges.firstOrNull { it.highlighted }?.value
    val imdb = heading.ratingBadges.firstOrNull { !it.highlighted }?.value
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        heading.ratingBadges.firstOrNull()?.let { FilmaxRatingPill(it.value) }
        kinopoisk?.let { MetaText("КП $it") }
        imdb?.let { MetaText("IMDb $it") }
        heading.volume?.let { MetaText(it) }
        if (heading.hasUhd) FilmaxBadge("4K")
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
private fun ActionTiles(
    trailerUrl: String?,
    isFav: Boolean,
    inAnyFolder: Boolean,
    onEvent: (DetailsEvent) -> Unit,
    onFolders: (FolderPickerEvent) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (trailerUrl != null) {
            FilmaxActionTile(
                icon = Icons.Filled.Movie,
                label = "Трейлер",
                onClick = { onEvent(DetailsEvent.PlayTrailer) },
                modifier = Modifier.weight(1f),
            )
        }
        FilmaxActionTile(
            icon = if (isFav) Icons.Filled.BookmarkAdded else Icons.Filled.BookmarkAdd,
            label = if (isFav) "В списке" else "Буду смотреть",
            onClick = { onEvent(DetailsEvent.ToggleFav) },
            active = isFav,
            modifier = Modifier.weight(1f),
        )
        FilmaxActionTile(
            icon = if (inAnyFolder) Icons.Filled.Folder else Icons.Filled.CreateNewFolder,
            label = "В папку",
            onClick = { onFolders(FolderPickerEvent.Open) },
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
internal fun DetailsTopBar(title: String, collapsed: Boolean, glass: GlassState, onEvent: (DetailsEvent) -> Unit) {
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
            FilmaxIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                onClick = { onEvent(DetailsEvent.Back) },
            )
            val titleAlpha by animateFloatAsState(if (collapsed) 1f else 0f, label = "detailsTitle")
            Text(
                title,
                style = FilmaxType.playerTitle,
                color = FilmaxColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = titleAlpha },
            )
            FilmaxIconButton(
                Icons.Filled.Share,
                contentDescription = "Поделиться",
                onClick = { onEvent(DetailsEvent.Share) },
            )
        }
    }
}

/**
 * Плавающая панель «Смотреть»: главная кнопка с подписью, что именно сыграет, и «на телевизоре».
 * Сериал без серий играть нечем ([play] = null) — панели нет вовсе.
 */
@Composable
internal fun DetailsPlayPanel(
    play: PlayButton?,
    glass: GlassState,
    onEvent: (DetailsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (play == null) return
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
                title = play.title,
                subtitle = play.subtitle,
                icon = Icons.Filled.PlayArrow,
                onClick = { onEvent(DetailsEvent.Play) },
                progress = play.progress,
                modifier = Modifier.weight(1f),
            )
            FilmaxIconButton(
                Icons.Filled.Cast,
                contentDescription = "Смотреть на телевизоре",
                onClick = { onEvent(DetailsEvent.PlayOnTv) },
                size = FilmaxMetrics.IconButtonLarge,
            )
        }
    }
}
