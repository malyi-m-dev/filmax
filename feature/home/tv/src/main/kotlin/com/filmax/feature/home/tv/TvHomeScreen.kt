package com.filmax.feature.home.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvPosterTitle
import com.filmax.core.tv.designsystem.posterSubtitle
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeScreenModel
import com.filmax.feature.home.common.HomeState
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)

/**
 * TV-Главная (экран 01 макета): hero-бэкдроп с действиями и горизонтальные рельсы постеров.
 * Поверх общего [HomeScreenModel] — данные те же, что и на телефоне. Верхний таб-бар рисует
 * общий TV-скаффолд в `:app`. Фокус/скролл — нативные (tv-material3 + Lazy bring-into-view).
 */
@Composable
fun TvHomeScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: HomeScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val offline by screenModel.collectOfflineBannerAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            else -> TvHomeContent(
                state = state,
                offline = offline,
                onOpenItem = onOpenItem,
                onLoadMore = { screenModel.dispatch(HomeEvent.LoadMoreAll) },
                onReload = { screenModel.dispatch(HomeEvent.Load) },
            )
        }
    }
}

@Composable
private fun TvHomeContent(
    state: HomeState,
    offline: Boolean,
    onOpenItem: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onReload: () -> Unit,
) {
    val listState = rememberLazyListState()
    ScrollToTopOnNavFocus(listState)
    // Триггер догрузки «Все»: когда фокус/скролл подводит к последним строкам списка.
    val loadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(loadMore) { if (loadMore) onLoadMore() }

    // «Все» бьём на ряды по ALL_COLUMNS заранее и кэшируем: иначе chunked() пересоздавал бы
    // список рядов на каждой рекомпозиции (а он растёт с каждой подгруженной страницей).
    val allRows = remember(state.all) { state.all.chunked(ALL_COLUMNS) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        // Офлайн-деградация (issue #42): кэшированный контент + баннер «нет сети» вместо ошибки.
        if (offline) {
            item(key = "offline") { TvOfflineBanner(onReload = onReload) }
        }
        state.hero?.let { hero ->
            item(key = "hero") {
                TvHero(
                    item = hero,
                    onPlay = { onOpenItem(hero.id) },
                    onDetails = { onOpenItem(hero.id) },
                )
            }
        }

        tvRails(state = state, onOpenItem = onOpenItem)

        // ── Все — постранично подгружаемая сетка (6 в ряд) ──────────────────────
        if (state.all.isNotEmpty()) {
            tvAllGrid(
                allRows = allRows,
                allLoadingMore = state.allLoadingMore,
                onOpenItem = onOpenItem,
            )
        }
    }
}

// Рельсы «Продолжить/В тренде/Для вас» — те же item-блоки, вынесены из TvHomeContent.
private fun LazyListScope.tvRails(state: HomeState, onOpenItem: (Int) -> Unit) {
    if (state.continueWatching.isNotEmpty()) {
        item(key = "continue") {
            TvRail(title = "Продолжить просмотр") {
                items(state.continueWatching, key = { it.itemId }) { history ->
                    TvContinueCard(history = history, onClick = { onOpenItem(history.itemId) })
                }
            }
        }
    }

    if (state.trending.isNotEmpty()) {
        item(key = "trending") {
            TvRail(title = "В тренде") {
                items(state.trending, key = { it.id }) { itm ->
                    TvPosterCard(item = itm, onClick = { onOpenItem(itm.id) })
                }
            }
        }
    }

    if (state.forYou.isNotEmpty()) {
        item(key = "forYou") {
            TvRail(title = "Для вас") {
                items(state.forYou, key = { it.id }) { itm ->
                    TvPosterCard(item = itm, onClick = { onOpenItem(itm.id) })
                }
            }
        }
    }
}

// Сетка «Все» + индикатор догрузки — вынесены из TvHomeContent без изменений раскладки.
private fun LazyListScope.tvAllGrid(
    allRows: List<List<Item>>,
    allLoadingMore: Boolean,
    onOpenItem: (Int) -> Unit,
) {
    item(key = "all_title") { TvSectionTitle("Все") }
    itemsIndexed(allRows, key = { index, _ -> "all_row_$index" }) { _, rowItems ->
        TvAllRow(rowItems = rowItems, onOpenItem = onOpenItem)
    }
    // Индикатор догрузки «Все» — отдельным элементом после сетки (без вложенного if).
    if (allLoadingMore) {
        item(key = "all_loading") { TvAllLoadingRow() }
    }
}

@Composable
private fun TvAllRow(rowItems: List<Item>, onOpenItem: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 72.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        rowItems.forEach { itm ->
            TvAllPosterCell(item = itm, onClick = { onOpenItem(itm.id) })
        }
        // Добиваем неполную строку, чтобы колонки не растягивались.
        repeat(ALL_COLUMNS - rowItems.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun TvAllLoadingRow() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

private const val ALL_COLUMNS = 6

@Composable
private fun TvSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 72.dp),
    )
}

@Composable
private fun RowScope.TvAllPosterCell(item: Item, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.weight(1f)) {
        TvFocusCard(
            onClick = onClick,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
        ) {
            Box(Modifier.fillMaxSize()) {
                PosterImage(
                    url = item.posters.medium.ifEmpty { item.posters.big },
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
                    accentColor = Accent,
                )
                RatingPill(
                    rating = item.rating.external,
                    compact = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
                TvPosterTitle(
                    title = item.title,
                    subtitle = posterSubtitle(item.year, item.genres.firstOrNull()?.title),
                )
            }
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────
@Composable
private fun TvHero(
    item: Item,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp),
    ) {
        PosterImage(
            url = item.posters.wide ?: item.posters.big,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            accentColor = Accent,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        0.45f to MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        0.8f to Color.Transparent,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.surface,
                    )
                )
        )

        TvHeroOverlay(item = item, onPlay = onPlay, onDetails = onDetails)
    }
}

// Текстовый оверлей hero (название/мета/описание/кнопки) — вынесен из TvHero без изменений.
@Composable
private fun BoxScope.TvHeroOverlay(item: Item, onPlay: () -> Unit, onDetails: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 72.dp, end = 72.dp, top = 96.dp),
    ) {
        EditorsChoicePill()
        Spacer(Modifier.height(16.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(0.6f),
        )
        Spacer(Modifier.height(14.dp))
        HeroMeta(item)
        Spacer(Modifier.height(14.dp))
        Text(
            item.plot,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(0.56f),
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TvButton("Смотреть", onClick = onPlay, leadingIcon = Icons.Filled.PlayArrow)
            TvButton("В избранное", onClick = {}, primary = false, leadingIcon = Icons.Filled.FavoriteBorder)
            TvButton("Подробнее", onClick = onDetails, primary = false, leadingIcon = Icons.Filled.Info)
        }
    }
}

@Composable
private fun HeroMeta(item: Item) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        RatingPill(rating = item.rating.external)
        MetaText("${item.year}")
        MetaDot()
        item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let {
            MetaText(formatDuration(it))
            MetaDot()
        }
        MetaText(item.genres.take(3).joinToString(" · ") { it.title })
    }
}

@Composable
private fun MetaText(text: String) =
    Text(text, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))

@Composable
private fun MetaDot() = Box(
    Modifier
        .size(4.dp)
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.5f))
)

@Composable
private fun EditorsChoicePill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Accent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            "⚡ ВЫБОР РЕДАКЦИИ",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
        )
    }
}

// ── Rails ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TvRail(title: String, content: LazyListScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 72.dp, bottom = 16.dp),
        )
        // focusRestorer: «вниз» в ряд ведёт на первый элемент, дальше запоминает позицию —
        // иначе D-pad ищет пространственно-ближайшую карточку и после скролла мажет мимо первой.
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
    }
}

@Composable
private fun TvPosterCard(item: Item, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.size(width = 200.dp, height = 300.dp)) {
        Box(Modifier.fillMaxSize()) {
            PosterImage(
                url = item.posters.medium.ifEmpty { item.posters.big },
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                shape = shape,
                accentColor = Accent,
            )
            RatingPill(
                rating = item.rating.external,
                compact = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            )
            TvPosterTitle(
                title = item.title,
                subtitle = posterSubtitle(item.year, item.genres.firstOrNull()?.title),
            )
        }
    }
}

@Composable
private fun TvContinueCard(history: WatchHistory, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier = Modifier.width(320.dp)) {
        TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.fillMaxWidth().height(180.dp)) {
            Box(Modifier.fillMaxSize()) {
                PosterImage(
                    url = history.posterSmall.orEmpty(),
                    contentDescription = history.title,
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
                    accentColor = Accent,
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(history.progress?.fraction ?: 0f)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            history.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun formatDuration(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h > 0 && m > 0 -> "${h}ч ${m}м"
        h > 0 -> "${h}ч"
        else -> "${m}м"
    }
}

/** Баннер «нет сети» над кэшированным контентом на Apple TV; фокус+OK — повторить (issue #42). */
@Composable
private fun TvOfflineBanner(onReload: () -> Unit) {
    TvButton(
        text = "Нет сети — показаны сохранённые данные. Нажмите, чтобы повторить",
        onClick = onReload,
        primary = false,
        leadingIcon = Icons.Filled.CloudOff,
        modifier = Modifier.padding(horizontal = 56.dp),
    )
}
