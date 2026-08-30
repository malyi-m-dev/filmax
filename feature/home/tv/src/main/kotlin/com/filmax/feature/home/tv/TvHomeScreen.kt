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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvErrorState
import com.filmax.core.tv.designsystem.TvMetaRow
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvRail
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.appErrorText
import com.filmax.core.ui.components.continueMeta
import com.filmax.core.ui.components.durationLabel
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.posterUrl
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeRow
import com.filmax.feature.home.common.HomeRowId
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
    val appError by screenModel.collectErrorAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface),
    ) {
        val error = appError
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TvAccent)
            }

            // Показывать нечего и есть ошибка — объясняемся и даём «Повторить». Модалки, как на
            // телефоне, тут нет: на пульте перекрывать ей пустой экран незачем, а фокусу нужна
            // хоть одна цель. Пришли данные (пусть и из кэша) — ошибку снимает баннер «нет сети».
            error != null && state.isEmpty -> {
                val text = appErrorText(error)
                TvErrorState(
                    title = text.title,
                    message = text.message,
                    onRetry = screenModel::retry,
                )
            }

            else -> TvHomeContent(
                state = state,
                offline = offline,
                actions = TvHomeActions(
                    onOpenItem = onOpenItem,
                    onPlay = onPlay,
                    onOpenCollection = onOpenCollection,
                    onReload = { screenModel.dispatch(HomeEvent.Load) },
                    onLoadMoreRow = { id -> screenModel.dispatch(HomeEvent.LoadMoreRow(id)) },
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
    val onLoadMoreRow: (HomeRowId) -> Unit,
)

@Composable
private fun TvHomeContent(
    state: HomeState,
    offline: Boolean,
    actions: TvHomeActions,
) {
    val listState = rememberLazyListState()
    ScrollToTopOnNavFocus(listState)
    val focus = rememberTvScreenFocus()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().then(focus.containerModifier),
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
                    focus = focus,
                )
            }
        }
        tvRails(state = state, actions = actions, focus = focus)
    }
}

/** Лента — это [HomeState.rows]: экран идёт по ним и рисует, состав и порядок задаёт модель. */
private fun LazyListScope.tvRails(state: HomeState, actions: TvHomeActions, focus: TvScreenFocus) {
    state.rows.forEach { row ->
        if (row.isEmpty) return@forEach
        when (row) {
            is HomeRow.Continue -> tvContinueRail(row, actions, focus)
            is HomeRow.Titles -> tvPosterRail(row, actions, focus)
            is HomeRow.Collections -> tvCollectionsRail(row, actions, focus)
        }
    }
}

/**
 * Заголовки рядов живут в экране, а не в модели: на десяти футах «Продолжить просмотр»
 * читается лучше, чем телефонное «Продолжить».
 */
private val HomeRowId.title: String
    get() = when (this) {
        HomeRowId.CONTINUE -> "Продолжить просмотр"
        HomeRowId.TRENDING -> "В тренде"
        // Заголовок честный: forYou — это топ сериалов по рейтингу, персонализации в фиде нет.
        HomeRowId.FOR_YOU -> "Сериалы с высоким рейтингом"
        HomeRowId.COLLECTIONS -> "Подборки"
    }

private fun LazyListScope.tvContinueRail(row: HomeRow.Continue, actions: TvHomeActions, focus: TvScreenFocus) {
    val onPlay = actions.onPlay
    item(key = row.id.name) {
        TvRail(title = row.id.title) {
            items(row.entries, key = { entry -> entry.itemId }) { entry ->
                // Ряд продолжения ведёт сразу в плеер — на недосмотренный эпизод (videoId+сезон
                // из истории), позицию внутри трека восстановит PlayerScreenModel.
                TvContinueCard(
                    history = entry,
                    modifier = focus.item(returnKey(row.id, entry.itemId)),
                    onClick = {
                        onPlay(
                            entry.itemId,
                            entry.progress?.season ?: NO_SEASON,
                            entry.progress?.videoId ?: NO_VIDEO_ID,
                        )
                    },
                )
            }
        }
    }
}

private fun LazyListScope.tvPosterRail(row: HomeRow.Titles, actions: TvHomeActions, focus: TvScreenFocus) {
    val railItems = row.paging.items
    item(key = row.id.name) {
        TvRail(title = row.id.title) {
            itemsIndexed(railItems, key = { _, catalogItem -> catalogItem.id }) { index, catalogItem ->
                // Хвостовая карточка скомпонована — зритель долистал ряд почти до конца:
                // просим следующую страницу. Ленивый ряд композит только видимое (+префетч),
                // поэтому это дешёвый триггер без слежения за скроллом; повторные вызовы
                // гасит идемпотентность модели.
                if (index == railItems.lastIndex) {
                    LaunchedEffect(railItems.size) { actions.onLoadMoreRow(row.id) }
                }
                TvHomePosterCard(
                    item = catalogItem,
                    onClick = { actions.onOpenItem(catalogItem.id) },
                    modifier = focus.item(returnKey(row.id, catalogItem.id)),
                )
            }
        }
    }
}

private fun LazyListScope.tvCollectionsRail(row: HomeRow.Collections, actions: TvHomeActions, focus: TvScreenFocus) {
    // Подборка без постера — пустая плашка: в монохроме карточку держит только картинка.
    val withPoster = row.paging.items.filter { it.posterUrl() != null }
    if (withPoster.isEmpty()) return
    item(key = row.id.name) {
        TvRail(title = row.id.title) {
            itemsIndexed(withPoster, key = { _, collection -> collection.id }) { index, collection ->
                // Хвостовая карточка скомпонована — просим следующую страницу (как у постер-рядов).
                if (index == withPoster.lastIndex) {
                    LaunchedEffect(withPoster.size) { actions.onLoadMoreRow(row.id) }
                }
                TvCollectionCard(
                    collection = collection,
                    onClick = { actions.onOpenCollection(collection.id, collection.title) },
                    modifier = focus.item(returnKey(row.id, collection.id)),
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
    focus: TvScreenFocus,
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

        TvHeroOverlay(item = item, onPlay = onPlay, onDetails = onDetails, focus = focus)
    }
}

@Composable
private fun BoxScope.TvHeroOverlay(
    item: Item,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    focus: TvScreenFocus,
) {
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
            TvButton("Смотреть", onClick = onPlay, modifier = focus.item(HERO_PLAY_KEY))
            TvButton(
                "Подробнее",
                onClick = onDetails,
                modifier = focus.item(HERO_DETAILS_KEY),
                primary = false,
            )
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
            item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let { add(durationLabel(it)) }
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
private fun TvHomePosterCard(item: Item, onClick: () -> Unit, modifier: Modifier) {
    TvPosterCard(
        modifier = modifier,
        title = item.title,
        meta = posterMeta(type = typeLabel(item.type), year = item.year),
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        rating = ratingLabel(item.rating.kinopoisk),
        onClick = onClick,
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
private fun TvCollectionCard(collection: Collection, onClick: () -> Unit, modifier: Modifier) {
    TvPosterCard(
        modifier = modifier,
        title = collection.title,
        meta = null,
        posterUrl = collection.posterUrl().orEmpty(),
        onClick = onClick,
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
private fun TvContinueCard(history: WatchHistory, onClick: () -> Unit, modifier: Modifier) {
    TvProgressCard(
        modifier = modifier,
        title = history.title,
        meta = continueMeta(history.progress),
        // Карточка 16:9 — берём кадр, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = history.wideOrPoster,
        progress = history.progress?.fraction ?: 0f,
        onClick = onClick,
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

/**
 * Ключ возврата фокуса: «ряд:id». Ряд в префиксе обязателен — один тайтл встречается сразу
 * в нескольких рядах, а ключ должен быть уникален в пределах экрана.
 */
private fun returnKey(row: HomeRowId, itemId: Int): String = "$row:$itemId"

/** Ключи кнопок hero: hero на экране один, поэтому без id. */
private const val HERO_PLAY_KEY = "hero:play"
private const val HERO_DETAILS_KEY = "hero:details"

/** `PlayerRoute.videoId` для фильма/неизвестного эпизода — плеер возьмёт первый трек. */
private const val NO_VIDEO_ID = -1

/** `PlayerRoute.season` для фильма/неизвестного сезона. */
private const val NO_SEASON = -1

/** Больше трёх жанров мета-строка hero не вмещает по ширине [HeroContentWidth]. */
private const val MAX_HERO_GENRES = 3
