package com.filmax.feature.home.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvMetaRow
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvRail
import com.filmax.core.tv.designsystem.TvReturnFocus
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.posterMeta
import com.filmax.core.tv.designsystem.ratingLabel
import com.filmax.core.tv.designsystem.rememberTvReturnFocus
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeScreenModel
import com.filmax.feature.home.common.HomeState
import org.koin.androidx.compose.koinViewModel

/**
 * TV-Главная: hero «выбор редакции» и горизонтальные ряды. Поверх общего [HomeScreenModel] —
 * данные те же, что и на телефоне. Верхний таб-бар рисует общий TV-скаффолд в `:app`.
 *
 * Плоской ленты «Все» здесь намеренно нет: это работа Каталога с его фильтрами и сортировкой.
 * Бесконечный ряд на пульте — сотни нажатий вправо и ни одного способа найти в нём конкретное.
 */
@Composable
fun TvHomeScreen(
    onOpenItem: (Int) -> Unit,
    onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
    onOpenCollection: (id: Int, title: String) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: HomeScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val offline by screenModel.collectOfflineBannerAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface),
    ) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TvAccent)
            }

            else -> TvHomeContent(
                state = state,
                offline = offline,
                actions = TvHomeActions(
                    onOpenItem = onOpenItem,
                    onPlay = onPlay,
                    onOpenCollection = onOpenCollection,
                    onReload = { screenModel.dispatch(HomeEvent.Load) },
                ),
            )
        }
    }
}

/** Действия главной одним объектом — как MovieActions в TV-деталях. */
private data class TvHomeActions(
    val onOpenItem: (Int) -> Unit,
    val onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
    val onOpenCollection: (id: Int, title: String) -> Unit,
    val onReload: () -> Unit,
)

@Composable
private fun TvHomeContent(
    state: HomeState,
    offline: Boolean,
    actions: TvHomeActions,
) {
    val listState = rememberLazyListState()
    ScrollToTopOnNavFocus(listState)
    val returnFocus = rememberTvReturnFocus()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = TvMetrics.ContentTop,
            bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset,
        ),
        verticalArrangement = Arrangement.spacedBy(TvMetrics.RowGap),
    ) {
        // Офлайн-деградация (issue #42): кэшированный контент + баннер «нет сети» вместо ошибки.
        if (offline) {
            item(key = "offline") { TvOfflineBanner(onReload = actions.onReload) }
        }
        state.hero?.let { hero ->
            item(key = "hero") {
                TvHero(
                    item = hero,
                    // Фильм — единственный трек, эпизод выбирать не из чего: PlayerRoute.videoId = -1.
                    onPlay = { actions.onPlay(hero.id, NO_SEASON, NO_VIDEO_ID) },
                    onDetails = { actions.onOpenItem(hero.id) },
                )
            }
        }
        tvRails(state = state, actions = actions, returnFocus = returnFocus)
    }
}

private fun LazyListScope.tvRails(state: HomeState, actions: TvHomeActions, returnFocus: TvReturnFocus) {
    tvContinueRail(history = state.continueWatching, onPlay = actions.onPlay, returnFocus = returnFocus)
    tvPosterRail(
        key = "trending",
        title = "В тренде",
        railItems = state.trending,
        onOpenItem = actions.onOpenItem,
        returnFocus = returnFocus,
    )
    // Заголовок честный: `forYou` — это топ сериалов по рейтингу, персонализации в фиде нет.
    tvPosterRail(
        key = "forYou",
        title = "Сериалы с высоким рейтингом",
        railItems = state.forYou,
        onOpenItem = actions.onOpenItem,
        returnFocus = returnFocus,
    )
    tvCollectionsRail(
        collections = state.collections,
        onOpenCollection = actions.onOpenCollection,
        returnFocus = returnFocus,
    )
}

private fun LazyListScope.tvContinueRail(
    history: List<WatchHistory>,
    onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
    returnFocus: TvReturnFocus,
) {
    if (history.isEmpty()) return
    item(key = "continue") {
        TvRail(title = "Продолжить просмотр") { firstItemFocus ->
            itemsIndexed(history, key = { _, entry -> entry.itemId }) { index, entry ->
                // Ряд продолжения ведёт сразу в плеер — на недосмотренный эпизод (videoId+сезон
                // из истории), позицию внутри трека восстановит PlayerScreenModel.
                TvContinueCard(
                    history = entry,
                    onClick = {
                        returnFocus.onOpen("continue:${entry.itemId}")
                        onPlay(
                            entry.itemId,
                            entry.progress?.season ?: NO_SEASON,
                            entry.progress?.videoId ?: NO_VIDEO_ID,
                        )
                    },
                    focusRequester = returnFocus.target("continue:${entry.itemId}")
                        ?: firstItemFocus.takeIf { index == 0 },
                )
            }
        }
    }
}

private fun LazyListScope.tvPosterRail(
    key: String,
    title: String,
    railItems: List<Item>,
    onOpenItem: (Int) -> Unit,
    returnFocus: TvReturnFocus,
) {
    if (railItems.isEmpty()) return
    item(key = key) {
        TvRail(title = title) { firstItemFocus ->
            itemsIndexed(railItems, key = { _, catalogItem -> catalogItem.id }) { index, catalogItem ->
                TvHomePosterCard(
                    item = catalogItem,
                    onClick = {
                        returnFocus.onOpen("$key:${catalogItem.id}")
                        onOpenItem(catalogItem.id)
                    },
                    focusRequester = returnFocus.target("$key:${catalogItem.id}")
                        ?: firstItemFocus.takeIf { index == 0 },
                )
            }
        }
    }
}

private fun LazyListScope.tvCollectionsRail(
    collections: List<Collection>,
    onOpenCollection: (id: Int, title: String) -> Unit,
    returnFocus: TvReturnFocus,
) {
    // Подборка без постера — пустая плашка: в монохроме карточку держит только картинка.
    val withPoster = collections.filter { it.posterUrl() != null }
    if (withPoster.isEmpty()) return
    item(key = "collections") {
        TvRail(title = "Подборки") { firstItemFocus ->
            itemsIndexed(withPoster, key = { _, collection -> collection.id }) { index, collection ->
                TvCollectionCard(
                    collection = collection,
                    onClick = {
                        returnFocus.onOpen("collections:${collection.id}")
                        onOpenCollection(collection.id, collection.title)
                    },
                    focusRequester = returnFocus.target("collections:${collection.id}")
                        ?: firstItemFocus.takeIf { index == 0 },
                )
            }
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────

/** Ширина текстового блока hero: дальше название на 44sp наезжает на светлую часть кадра. */
private val HeroContentWidth = 520.dp
private val HeroContentBottom = 26.dp

/**
 * Скрим hero, горизонтальный. Опорные точки — из макета (.94 → .82 на 34% → .35 на 62% → .05),
 * между ними добавлены промежуточные: CSS интерполирует градиент сам, а на 8-битной панели
 * телевизора серый-в-серый идёт видимыми ступенями — лишние стопы разбивают полосы.
 */
private val HeroScrimHorizontal = Brush.horizontalGradient(
    0.00f to TvSurface.copy(alpha = 0.94f),
    0.17f to TvSurface.copy(alpha = 0.89f),
    0.34f to TvSurface.copy(alpha = 0.82f),
    0.48f to TvSurface.copy(alpha = 0.60f),
    0.62f to TvSurface.copy(alpha = 0.35f),
    0.80f to TvSurface.copy(alpha = 0.16f),
    1.00f to TvSurface.copy(alpha = 0.05f),
)

/**
 * Скрим hero, вертикальный: сажает кадр на подложку экрана. Прозрачный конец — это TvSurface
 * с alpha 0, а не [androidx.compose.ui.graphics.Color.Transparent]: у Transparent RGB нулевые,
 * и интерполяция уводила бы градиент через чёрный.
 */
private val HeroScrimVertical = Brush.verticalGradient(
    0.00f to TvSurface.copy(alpha = 0f),
    0.60f to TvSurface.copy(alpha = 0f),
    0.72f to TvSurface.copy(alpha = 0.22f),
    0.84f to TvSurface.copy(alpha = 0.52f),
    0.92f to TvSurface.copy(alpha = 0.74f),
    1.00f to TvSurface.copy(alpha = 0.90f),
)

@Composable
private fun TvHero(
    item: Item,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TvMetrics.HeroHeight),
    ) {
        PosterImage(
            url = item.posters.wide ?: item.posters.big,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            accentColor = TvSurfaceContainerHigh,
        )
        Box(Modifier.fillMaxSize().background(HeroScrimHorizontal))
        Box(Modifier.fillMaxSize().background(HeroScrimVertical))

        TvHeroOverlay(item = item, onPlay = onPlay, onDetails = onDetails)
    }
}

@Composable
private fun BoxScope.TvHeroOverlay(item: Item, onPlay: () -> Unit, onDetails: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = TvMetrics.SafeHorizontal, bottom = HeroContentBottom)
            .width(HeroContentWidth),
    ) {
        TvOverline("Выбор редакции")
        Spacer(Modifier.height(10.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.displayMedium,
            color = TvOnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        TvHeroMeta(item)
        Spacer(Modifier.height(20.dp))
        // «Буду смотреть» из макета не выводим: события watchlist в HomeEvent нет, а кнопка,
        // которая ничего не делает, хуже отсутствующей.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton("Смотреть", onClick = onPlay)
            TvButton("Подробнее", onClick = onDetails, primary = false)
        }
    }
}

@Composable
private fun TvHeroMeta(item: Item) {
    val parts = remember(item) {
        buildList {
            item.genres.take(MAX_HERO_GENRES).joinToString(" · ") { it.title }
                .takeIf { it.isNotBlank() }
                ?.let { add(it) }
            if (item.year > 0) add(item.year.toString())
            item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let { add(formatDuration(it)) }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Рейтинг — единственная часть меты в полный контраст: в монохроме вес и яркость
        // делают то, что на цветном макете делала бы акцентная пилюля.
        ratingLabel(item.rating.kinopoisk)?.let { rating ->
            Text(
                "$rating КП",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TvOnSurface,
            )
        }
        if (parts.isNotEmpty()) TvMetaRow(parts)
    }
}

// ── Карточки рядов ────────────────────────────────────────────────────────

@Composable
private fun TvHomePosterCard(item: Item, onClick: () -> Unit, focusRequester: FocusRequester?) {
    TvPosterCard(
        title = item.title,
        meta = posterMeta(type = item.type.label(), year = item.year),
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        rating = ratingLabel(item.rating.kinopoisk),
        onClick = onClick,
        focusRequester = focusRequester,
        posterContent = { url, posterModifier ->
            PosterImage(
                url = url,
                contentDescription = item.title,
                modifier = posterModifier,
                shape = TvMetrics.PosterShape,
                accentColor = TvSurfaceContainerHigh,
            )
        },
    )
}

@Composable
private fun TvCollectionCard(collection: Collection, onClick: () -> Unit, focusRequester: FocusRequester?) {
    TvPosterCard(
        title = collection.title,
        meta = null,
        posterUrl = collection.posterUrl().orEmpty(),
        onClick = onClick,
        focusRequester = focusRequester,
        posterContent = { url, posterModifier ->
            PosterImage(
                url = url,
                contentDescription = collection.title,
                modifier = posterModifier,
                shape = TvMetrics.PosterShape,
                accentColor = TvSurfaceContainerHigh,
            )
        },
    )
}

@Composable
private fun TvContinueCard(history: WatchHistory, onClick: () -> Unit, focusRequester: FocusRequester?) {
    TvProgressCard(
        title = history.title,
        meta = continueMeta(history.progress),
        // Карточка 16:9 — берём кадр, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = history.wideOrPoster,
        progress = history.progress?.fraction ?: 0f,
        onClick = onClick,
        focusRequester = focusRequester,
        posterContent = { url, posterModifier ->
            PosterImage(
                url = url,
                contentDescription = history.title,
                modifier = posterModifier,
                shape = TvMetrics.CardShape,
                accentColor = TvSurfaceContainerHigh,
            )
        },
    )
}

/** Баннер «нет сети» над кэшированным контентом; фокус+OK — повторить (issue #42). */
@Composable
private fun TvOfflineBanner(onReload: () -> Unit) {
    TvButton(
        text = "Нет сети — показаны сохранённые данные. Нажмите, чтобы повторить",
        onClick = onReload,
        primary = false,
        leadingIcon = Icons.Filled.CloudOff,
        modifier = Modifier.padding(horizontal = TvMetrics.SafeHorizontal),
    )
}

// ── Форматирование ────────────────────────────────────────────────────────

/** `PlayerRoute.videoId` для фильма/неизвестного эпизода — плеер возьмёт первый трек. */
private const val NO_VIDEO_ID = -1

/** `PlayerRoute.season` для фильма/неизвестного сезона. */
private const val NO_SEASON = -1

/** Больше трёх жанров мета-строка hero не вмещает по ширине [HeroContentWidth]. */
private const val MAX_HERO_GENRES = 3

private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_MINUTE = 60

/**
 * Подпись карточки «продолжить»: «S2 · осталось 18 мин». Номер эпизода макета («E5») не
 * выводим: в [WatchProgress] его нет — `videoId` это идентификатор трека, а не порядковый
 * номер серии (`PlayerScreenModel` матчит им `MediaTrack.id`).
 */
private fun continueMeta(progress: WatchProgress?): String? {
    if (progress == null) return null
    val parts = buildList {
        progress.season?.takeIf { it > 0 }?.let { add("S$it") }
        remainingMinutes(progress)?.let { add("осталось ${formatDuration(it)}") }
    }
    return parts.joinToString(" · ").ifBlank { null }
}

/** Сколько минут осталось до конца трека; null — прогресса нет или уже досмотрено. */
private fun remainingMinutes(progress: WatchProgress): Int? {
    val watched = progress.timeSeconds
    val total = progress.durationSeconds?.takeIf { it > 0 }
    if (watched == null || total == null) return null
    return ((total - watched) / SECONDS_IN_MINUTE).takeIf { it > 0 }
}

private fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / MINUTES_IN_HOUR
    val minutes = totalMinutes % MINUTES_IN_HOUR
    return when {
        hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
        hours > 0 -> "$hours ч"
        else -> "$minutes мин"
    }
}

/** Русское название типа для меты карточки — [ItemType] хранит только API-значения. */
private fun ItemType.label(): String = when (this) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY -> "Документальный"
    ItemType.TV -> "ТВ"
}

/** Постер подборки; null — картинки нет, такую подборку не показываем. */
private fun Collection.posterUrl(): String? =
    posters?.let { it.medium.ifEmpty { it.big } }?.takeIf { it.isNotBlank() }
