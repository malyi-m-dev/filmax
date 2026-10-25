// Hero Главной: карусель «Выбор редакции» — фон с ken-burns, текст тайтла и действия.
package com.filmax.feature.home.tv

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvButtonStyle
import com.filmax.core.tv.designsystem.TvIconButton
import com.filmax.core.tv.designsystem.TvMetaRow
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPageIndicator
import com.filmax.core.tv.designsystem.TvRatingPill
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.fxStaticOverlay
import com.filmax.core.tv.designsystem.kenBurns
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeState

/** Тег hero для UI-тестов: по нему проверяют, что баннер виден целиком. */
internal const val HERO_TEST_TAG = "home:hero"

/** Ключи кнопок hero для памяти фокуса: hero на экране один, поэтому без id. */
internal const val HERO_PLAY_KEY = "hero:play"
private const val HERO_DETAILS_KEY = "hero:details"
private const val HERO_WATCH_LATER_KEY = "hero:watchLater"

private const val BACKDROP_CROSSFADE_MS = 600

/**
 * Кадр hero чуть выше места, которое hero занимает в ленте (там — [TvMetrics.HeroHeight]):
 * нижняя кромка кадра уходит под заголовок первого ряда, и между ними нет шва.
 */
private val HeroImageOverhang = 6.dp
private val HeroImageHeight = TvMetrics.HeroHeight + HeroImageOverhang

/** Описание — 14/1.45: на ступень мельче описания деталей, hero делит экран с рядом. */
private val DescriptionStyle = FxType.body.copy(fontSize = 14.sp, lineHeight = 20.3.sp)

private val TextBlockWidth = 480.dp
private val DescriptionWidth = 420.dp
private val TextBlockTop = 72.dp

/**
 * Hero Главной. Фон — кадр тайтла с ken-burns, цветной подсветкой палитры сверху справа и
 * затемнением слева и снизу; смена тайтла — кросс-фейд кадра, текст появляется заново по
 * очереди ([fxEnter]). Слева — надзаголовок, название, мета, описание и действия; справа внизу —
 * индикатор страниц. Какой тайтл показан и когда сменить его, решает модель: hero сообщает ей
 * только о фокусе, а ken-burns идёт, пока фокус здесь.
 */
@Composable
internal fun TvHomeHero(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    focus: TvScreenFocus,
    modifier: Modifier = Modifier,
) {
    val item = state.hero ?: return
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxWidth()
            .height(HeroImageHeight)
            .clipToBounds()
            .onFocusChanged {
                focused = it.hasFocus
                onEvent(HomeEvent.HeroFocused(it.hasFocus))
            }
            .testTag(HERO_TEST_TAG),
    ) {
        HeroBackdrop(item, running = focused)
        HeroText(item, state.heroInWatchLater, onEvent, focus)
        if (state.heroes.size > 1) {
            TvPageIndicator(
                count = state.heroes.size,
                current = state.heroIndex,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = TvMetrics.SectionEnd, bottom = 34.dp),
            )
        }
    }
}

/**
 * Кадр тайтла: кросс-фейд при смене, ken-burns, подсветка палитры и два затемнения. Ken-burns
 * идёт, только пока фокус на hero ([running]): в рядах ниже hero почти не виден, а бесконечная
 * анимация фона не давала ленте простаивать — каждый кадр перекомпоновывался весь экран.
 */
@Composable
private fun BoxScope.HeroBackdrop(item: Item, running: Boolean) {
    Crossfade(targetState = item, animationSpec = tween(BACKDROP_CROSSFADE_MS), label = "heroBackdrop") { shown ->
        PosterImage(
            url = shown.posters.wide ?: shown.posters.big,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().kenBurns(running),
            shape = RectangleShape,
            accentColor = TvSurfaceContainerHigh,
        )
    }
    val colors = LocalFxColors.current
    // Подсветка и затемнения неподвижны — готовой текстурой, а не тремя градиентами на кадр.
    Box(
        Modifier
            .matchParentSize()
            .fxStaticOverlay {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(colors.wash, Color.Transparent),
                        center = Offset(size.width * WASH_CENTER_X, 0f),
                        radius = size.width * WASH_RADIUS,
                    ),
                )
                drawRect(
                    Brush.horizontalGradient(
                        SCRIM_LEFT_SOLID to colors.background,
                        SCRIM_LEFT_CLEAR to Color.Transparent
                    )
                )
                drawRect(Brush.verticalGradient(SCRIM_BOTTOM_CLEAR to Color.Transparent, 1f to colors.background))
            },
    )
}

/** Подсветка палитры — эллипс в правом верхнем углу (radial-gradient 70% 90% at 90% 0%). */
private const val WASH_CENTER_X = 0.9f
private const val WASH_RADIUS = 0.55f

/** Затемнение слева: глухое до 6% ширины, прозрачное с 68%; снизу — с середины высоты. */
private const val SCRIM_LEFT_SOLID = 0.06f
private const val SCRIM_LEFT_CLEAR = 0.68f
private const val SCRIM_BOTTOM_CLEAR = 0.5f

/**
 * Текст и действия hero. Текст пересоздаётся на каждый тайтл — так он появляется заново по
 * очереди; кнопки — нет: карусель листается и под фокусом, и пересозданная кнопка унесла бы
 * фокус с собой (он ушёл бы в меню).
 */
@Composable
private fun HeroText(item: Item, inWatchLater: Boolean, onEvent: (HomeEvent) -> Unit, focus: TvScreenFocus) {
    val colors = LocalFxColors.current
    Column(
        Modifier.padding(start = TvMetrics.SectionStart, top = TextBlockTop).widthIn(max = TextBlockWidth),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        key(item.id) {
            TvOverline("Выбор редакции", Modifier.fxEnter(0))
            Text(
                item.title,
                style = FxType.hero,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fxEnter(1),
            )
            HeroMeta(item, Modifier.fxEnter(2))
            if (item.plot.isNotBlank()) {
                Text(
                    item.plot,
                    style = DescriptionStyle,
                    color = colors.textSecondary,
                    maxLines = DESCRIPTION_LINES,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = DescriptionWidth).fxEnter(3),
                )
            }
        }
        HeroButtons(item, inWatchLater, onEvent, focus, Modifier.padding(top = 6.dp))
    }
}

private const val DESCRIPTION_LINES = 3

/** Пилюля оценки Кинопоиска и мета через отступы: жанры, год, длительность. */
@Composable
private fun HeroMeta(item: Item, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ratingLabel(item.rating.kinopoisk)?.let { TvRatingPill(it) }
        TvMetaRow(heroMetaParts(item))
    }
}

@Composable
private fun HeroButtons(
    item: Item,
    inWatchLater: Boolean,
    onEvent: (HomeEvent) -> Unit,
    focus: TvScreenFocus,
    modifier: Modifier
) {
    val play = remember { FocusRequester() }
    // Группа с памятью: «вверх» из рядов возвращает на ту кнопку, с которой ушли вниз, а не на
    // ближайшую к карточке; первый вход — на «Смотреть».
    Row(
        modifier.tvFocusGroup(fallback = play),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvButton(
            "Смотреть",
            onClick = { onEvent(HomeEvent.Play(item.id)) },
            leadingIcon = Icons.Rounded.PlayArrow,
            focusRequester = play,
            modifier = focus.item(HERO_PLAY_KEY),
        )
        TvButton(
            "Подробнее",
            onClick = { onEvent(HomeEvent.OpenItem(item.id)) },
            style = TvButtonStyle.Secondary,
            modifier = focus.item(HERO_DETAILS_KEY)
        )
        TvIconButton(
            icon = if (inWatchLater) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            contentDescription = if (inWatchLater) "Убрать из «Буду смотреть»" else "Буду смотреть",
            onClick = { onEvent(HomeEvent.ToggleWatchLater(item)) },
            modifier = focus.item(HERO_WATCH_LATER_KEY),
        )
    }
}
