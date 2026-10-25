// Hero Главной: карусель «Выбора редакции» — кадр с медленным «Кен Бёрнсом», подсветка палитры,
// надзаголовок, название, мета и кнопки. Лента — в HomeScreen.

package com.filmax.feature.home.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.radialWash
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.presentation.labels.durationLabel
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxOverline
import com.filmax.core.ui.components.FilmaxPageDots
import com.filmax.core.ui.components.FilmaxRatingPill
import com.filmax.core.ui.components.KenBurnsImage
import com.filmax.feature.home.common.HomeEvent
import kotlinx.coroutines.delay

/**
 * Карусель hero. Листается пальцем и сама — раз в [HERO_INTERVAL_MILLIS], пока её не трогают;
 * «Кен Бёрнс» идёт только у кадра на экране. Тап по кадру — детали тайтла.
 *
 * Страница — состояние пейджера, а не модели: на телефоне видны все hero, и автолистание — это
 * анимация пейджера; на ТВ, где hero один, его индексом и таймером владеет модель.
 */
@Composable
internal fun HomeHero(heroes: List<Item>, watchLaterIds: Set<Int>, height: Dp, onEvent: (HomeEvent) -> Unit) {
    val pager = rememberPagerState { heroes.size }
    AutoAdvance(pager)
    Box(Modifier.fillMaxWidth().height(height)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { heroes[it].id }) { page ->
            val hero = heroes[page]
            HeroPage(
                item = hero,
                running = page == pager.settledPage,
                inWatchLater = hero.id in watchLaterIds,
                onEvent = onEvent,
            )
        }
        FilmaxPageDots(
            count = heroes.size,
            current = pager.currentPage,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
        )
    }
}

/** Следующий тайтл через паузу; палец на карусели — пауза начинается заново после отпускания. */
@Composable
private fun AutoAdvance(pager: PagerState) {
    LaunchedEffect(pager, pager.settledPage, pager.pageCount) {
        if (pager.pageCount < 2) return@LaunchedEffect
        delay(HERO_INTERVAL_MILLIS)
        if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.settledPage + 1) % pager.pageCount)
    }
}

@Composable
private fun HeroPage(item: Item, running: Boolean, inWatchLater: Boolean, onEvent: (HomeEvent) -> Unit) {
    val palette = FilmaxTheme.palette
    Box(
        Modifier
            .fillMaxSize()
            .clickable(role = Role.Button) { onEvent(HomeEvent.OpenItem(item.id)) },
    ) {
        // Вертикальный постер, а не широкий кадр: hero на телефоне выше, чем шире, и кадр 16:9
        // резался бы по центру до неузнаваемости.
        KenBurnsImage(
            url = item.posters.big.ifBlank { item.posters.wide.orEmpty() },
            contentDescription = item.title,
            running = running,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .matchParentSize()
                .radialWash(palette.wash, centerX = 1f, centerY = 0f, radiusX = 1f, radiusY = 0.6f)
                .background(heroFade(palette.background)),
        )
        HeroContent(
            item = item,
            inWatchLater = inWatchLater,
            onEvent = onEvent,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Низ кадра уходит в фон экрана: на нём лежит текст, а лента под hero продолжает его без шва. */
private fun heroFade(background: Color) = Brush.verticalGradient(
    0f to Color.Transparent,
    HERO_OPEN_UNTIL to Color.Transparent,
    HERO_FADE_MIDDLE to background.copy(alpha = HERO_FADE_MIDDLE_ALPHA),
    1f to background,
)

private const val HERO_OPEN_UNTIL = 0.38f
private const val HERO_FADE_MIDDLE = 0.72f
private const val HERO_FADE_MIDDLE_ALPHA = 0.78f

@Composable
private fun HeroContent(
    item: Item,
    inWatchLater: Boolean,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FilmaxMetrics.ScreenPadding)
            .padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilmaxOverline("Выбор редакции")
        Text(
            item.title,
            style = FilmaxType.hero,
            color = FilmaxColors.Text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        HeroMeta(item = item, modifier = Modifier.padding(top = 12.dp))
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilmaxButton(
                text = "Смотреть",
                onClick = { onEvent(HomeEvent.Play(item.id)) },
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.weight(1f),
            )
            FilmaxIconButton(
                if (inWatchLater) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = if (inWatchLater) "Убрать из «Буду смотреть»" else "Буду смотреть",
                onClick = { onEvent(HomeEvent.ToggleWatchLater(item)) },
                size = FilmaxMetrics.IconButtonLarge,
            )
            FilmaxIconButton(
                Icons.Outlined.Info,
                contentDescription = "О тайтле",
                onClick = { onEvent(HomeEvent.OpenItem(item.id)) },
                size = FilmaxMetrics.IconButtonLarge,
            )
        }
    }
}

/** Мета hero: пилюля рейтинга и «Фантастика · 2024 · 2 ч 46 мин». */
@Composable
private fun HeroMeta(item: Item, modifier: Modifier = Modifier) {
    val parts = buildList {
        item.genres.firstOrNull()?.title?.let(::add)
        if (item.year > 0) add(item.year.toString())
        item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let { add(durationLabel(it)) }
    }
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ratingLabel(item.rating.kinopoisk ?: item.rating.imdb)?.let { FilmaxRatingPill(it) }
        if (parts.isNotEmpty()) {
            Text(
                parts.joinToString(" · "),
                style = FilmaxType.meta,
                color = FilmaxColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Высота hero: 61% экрана, как в макете (560 из 915), но не ниже 440 — иначе кнопки съедают кадр. */
@Composable
internal fun rememberHeroHeight(): Dp {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    return (screenHeight * HERO_SHARE).coerceIn(HeroMinHeight, FilmaxMetrics.HomeHeroHeight)
}

private val HeroMinHeight = 440.dp
private const val HERO_SHARE = 0.61f
private const val HERO_INTERVAL_MILLIS = 8_000L
