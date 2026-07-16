package com.filmax.feature.home.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxProgressCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeScreenModel
import com.filmax.feature.home.common.HomeState
import org.koin.androidx.compose.koinViewModel

/**
 * Колбэки навигации главной. Собраны в один тип: пять отдельных параметров плюс `modifier`
 * и `screenModel` выводят сигнатуру за порог detekt (LongParameterList = 6).
 */
data class HomeActions(
    val onOpenItem: (Int) -> Unit,
    val onPlay: (itemId: Int, videoId: Int) -> Unit,
    val onOpenCollection: (id: Int, title: String) -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenProfile: () -> Unit,
)

@Composable
fun HomeScreen(
    actions: HomeActions,
    modifier: Modifier = Modifier,
    screenModel: HomeScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val offline by screenModel.collectOfflineBannerAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Шапка вне ленты: она не скроллится, поэтому и не лежит в LazyColumn.
            HomeTopBar(
                initials = state.initials,
                onOpenSearch = actions.onOpenSearch,
                onOpenProfile = actions.onOpenProfile,
            )
            if (state.loading) {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                HomeFeed(
                    state = state,
                    offline = offline,
                    actions = actions,
                    onReload = { screenModel.dispatch(HomeEvent.Load) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        appError?.let { error ->
            FilmaxErrorModal(
                error = error,
                onDismiss = screenModel::dismissError,
                onPrimary = screenModel::retry,
            )
        }
    }
}

@Composable
private fun HomeFeed(
    state: HomeState,
    offline: Boolean,
    actions: HomeActions,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize()) {
        // Офлайн-деградация (issue #42): контент из кэша + ненавязчивый баннер вместо модалки.
        if (offline) {
            item(key = "offline") { OfflineBanner(onReload = onReload) }
        }
        state.hero?.let { hero ->
            item(key = "hero") {
                HomeHero(
                    item = hero,
                    // Фильм — единственный трек, эпизод выбирать не из чего.
                    onPlay = { actions.onPlay(hero.id, NO_VIDEO_ID) },
                    onOpenItem = { actions.onOpenItem(hero.id) },
                )
            }
        }
        homeRows(state = state, actions = actions)
    }
}

// ── Ряды ──────────────────────────────────────────────────────────────────

private fun LazyListScope.homeRows(state: HomeState, actions: HomeActions) {
    continueRow(history = state.continueWatching, onPlay = actions.onPlay)
    posterRow(
        key = "trending",
        title = "В тренде",
        rowItems = state.trending,
        onOpenItem = actions.onOpenItem,
    )
    // Заголовок честный: `forYou` — это топ сериалов по рейтингу, персонализации в фиде нет.
    posterRow(
        key = "forYou",
        title = "Сериалы с высоким рейтингом",
        rowItems = state.forYou,
        onOpenItem = actions.onOpenItem,
    )
    collectionsRow(collections = state.collections, onOpenCollection = actions.onOpenCollection)
}

private fun LazyListScope.continueRow(
    history: List<WatchHistory>,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
) {
    if (history.isEmpty()) return
    item(key = "continue") {
        HomeRow(title = "Продолжить") {
            items(history, key = { it.itemId }) { entry ->
                // Ряд продолжения ведёт сразу в плеер, минуя детали: недосмотренный эпизод —
                // videoId из истории, позицию внутри трека восстановит PlayerScreenModel.
                FilmaxProgressCard(
                    title = entry.title,
                    meta = continueMeta(entry.progress),
                    posterUrl = entry.wideOrPoster,
                    progress = entry.progress?.fraction ?: 0f,
                    onClick = { onPlay(entry.itemId, entry.progress?.videoId ?: NO_VIDEO_ID) },
                )
            }
        }
    }
}

private fun LazyListScope.posterRow(
    key: String,
    title: String,
    rowItems: List<Item>,
    onOpenItem: (Int) -> Unit,
) {
    if (rowItems.isEmpty()) return
    item(key = key) {
        HomeRow(title = title) {
            items(rowItems, key = { it.id }) { rowItem ->
                FilmaxPosterCard(
                    title = rowItem.title,
                    posterUrl = rowItem.posters.medium,
                    onClick = { onOpenItem(rowItem.id) },
                    rating = ratingLabel(rowItem.rating.kinopoisk),
                )
            }
        }
    }
}

private fun LazyListScope.collectionsRow(
    collections: List<Collection>,
    onOpenCollection: (id: Int, title: String) -> Unit,
) {
    // Подборка без постера — пустая плашка: в монохроме карточку держит только картинка.
    val withPoster = collections.mapNotNull { collection ->
        collection.posterUrl()?.let { poster -> collection to poster }
    }
    if (withPoster.isEmpty()) return
    item(key = "collections") {
        HomeRow(title = "Подборки") {
            items(withPoster, key = { it.first.id }) { (collection, poster) ->
                FilmaxPosterCard(
                    title = collection.title,
                    posterUrl = poster,
                    onClick = { onOpenCollection(collection.id, collection.title) },
                )
            }
        }
    }
}

/** Заголовок ряда без стрелки: ряд листается пальцем, а вести ей было некуда. */
@Composable
private fun HomeRow(title: String, content: LazyListScope.() -> Unit) {
    Column(Modifier.padding(top = FilmaxMetrics.RowGap)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = FilmaxMetrics.ScreenPadding),
        )
        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
            content = content,
        )
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────

@Composable
private fun HomeHero(item: Item, onPlay: () -> Unit, onOpenItem: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.HomeHeroHeight)
            .clickable(onClick = onOpenItem),
    ) {
        PosterImage(
            // Широкий кадр — если он есть: вертикальную обложку на 412dp во всю ширину режет по центру.
            url = item.posters.wide?.takeIf { it.isNotBlank() } ?: item.posters.big,
            contentDescription = item.title,
            modifier = Modifier.matchParentSize(),
            shape = RectangleShape,
            accentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Box(Modifier.matchParentSize().background(heroScrim()))
        HeroContent(item = item, onPlay = onPlay, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Скрим hero: снизу плотный — на нём лежит текст, сверху лёгкий — постер должен читаться.
 *
 * Плотнее макета в нижней трети. В макете кадр Дюны тёмный сам по себе, и лёгкого затемнения
 * хватало; на реальном каталоге постер бывает каким угодно — на светлом мультике надзаголовок
 * и мета пропадали. Скрим обязан держать текст на ЛЮБОЙ обложке, а не на удачной.
 *
 * Прозрачность набирается из surface, а не из [androidx.compose.ui.graphics.Color.Transparent]:
 * у последнего нулевой RGB, и переход к нему уводит градиент через чёрный, давая грязь.
 */
@Composable
private fun heroScrim(): Brush {
    val base = MaterialTheme.colorScheme.surface
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0f to base.copy(alpha = 0.55f),
            0.45f to base.copy(alpha = 0.35f),
            0.62f to base.copy(alpha = 0.72f),
            0.82f to base.copy(alpha = 0.95f),
            1f to base,
        ),
    )
}

@Composable
private fun HeroContent(item: Item, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "ВЫБОР РЕДАКЦИИ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            item.title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        HeroMeta(item = item)
        // Кнопки «Буду смотреть» рядом нет намеренно: HomeEvent не умеет переключать watchlist,
        // а рисовать кнопку, которая ничего не делает, — хуже, чем не рисовать её вовсе.
        HeroPlayButton(onClick = onPlay)
    }
}

/** Мета hero: «8.3 КП · Фантастика · Драма · 2024» — оценка весом, остальное вторичным. */
@Composable
private fun HeroMeta(item: Item) {
    val rating = ratingLabel(item.rating.kinopoisk)
    val genres = item.genres.take(MAX_HERO_GENRES).joinToString(" · ") { it.title }
    Row(
        modifier = Modifier.padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rating != null) {
            Text(
                "$rating КП",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (genres.isNotBlank()) {
            Text(
                genres,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Длинные жанры ужимаются с многоточием, а не выдавливают год за край экрана.
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (item.year > 0) {
            Text(
                item.year.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Главное действие экрана — единственная белая заливка в монохроме. */
@Composable
private fun HeroPlayButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 15.dp)
            .fillMaxWidth()
            .height(HeroButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            "Смотреть",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

// ── Шапка ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(initials: String, onOpenSearch: () -> Unit, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = FilmaxMetrics.ScreenPadding)
            .padding(top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeaderActionGap),
    ) {
        Text(
            "FILMAX",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(SearchTapSize)
                .clip(CircleShape)
                .clickable(onClick = onOpenSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Поиск",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
        HomeAvatar(initials = initials, onClick = onOpenProfile)
    }
}

@Composable
private fun HomeAvatar(initials: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isNotBlank()) {
            Text(
                initials,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Профиль",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Ненавязчивый баннер «нет сети» над кэшированным контентом; тап — повторить загрузку (issue #42). */
@Composable
private fun OfflineBanner(onReload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilmaxMetrics.ScreenPadding, vertical = 8.dp)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onReload)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "Нет сети — показаны сохранённые данные. Нажмите, чтобы повторить",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Размеры и форматирование ──────────────────────────────────────────────

/** Высота кнопки hero — из макета; общий [FilmaxMetrics.PrimaryButtonHeight] выше на 2dp. */
private val HeroButtonHeight = 48.dp

private val AvatarSize = 31.dp

/**
 * Тап-цель поиска: иконка в макете 21dp — этого мало для пальца, поэтому вокруг неё прозрачный
 * круг 38dp. Зазор в ряду ужат до 5.5dp, чтобы иконка при этом осталась там же, где в макете
 * (14dp до аватара по видимым краям).
 */
private val SearchTapSize = 38.dp
private val HeaderActionGap = 5.5.dp

/** `PlayerRoute.videoId` для фильма/неизвестного эпизода — плеер возьмёт первый трек. */
private const val NO_VIDEO_ID = -1

/** Больше двух жанров мета-строка hero не вмещает: на 360dp рядом ещё оценка и год. */
private const val MAX_HERO_GENRES = 2

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

/** Постер подборки; null — картинки нет, такую подборку не показываем. */
private fun Collection.posterUrl(): String? =
    posters?.let { it.medium.ifEmpty { it.big } }?.takeIf { it.isNotBlank() }
