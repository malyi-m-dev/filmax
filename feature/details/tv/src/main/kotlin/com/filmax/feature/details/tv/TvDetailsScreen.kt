// Экран деталей составной: hero с действиями, сезоны с эпизодами и ряд похожих. Каждая часть —
// свой composable, и это правильное дробление; растаскивать их по файлам значило бы разорвать
// один экран на куски, которые читаются только вместе.
@file:Suppress("TooManyFunctions")
// BringIntoViewSpec: единственный способ выключить фокус-прокрутку полотна в hero-стейте.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.details.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.person.CastMember
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvCardSize
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvMetaRow
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvRail
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.tv.designsystem.posterMeta
import com.filmax.core.tv.designsystem.ratingLabel
import com.filmax.core.tv.designsystem.rememberDimAlpha
import com.filmax.core.ui.components.HeroBackdrop
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** Ширина текстового блока в hero (макет: 600dp из 960) — правее лежит открытый бэкдроп. */
private val HeroTextWidth = 600.dp

/** Максимальная ширина описания и строки состава: длинная строка на 3 метрах не читается. */
private val ReadableTextWidth = 760.dp

/** Отступ снизу единого полотна: рамке фокуса последнего ряда нужно место. */
private val ContentBottomPadding = 70.dp

/** Индекс элемента «описание» в полотне: сюда полотно едет, когда фокус уходит с кнопок вниз. */
private const val CONTENT_START_INDEX = 1

/** Сколько кадров пропустить перед прокруткой к стейту (см. [rememberHeroFocusScroller]). */
private const val FRAMES_BEFORE_STATE_SCROLL = 2

/** Ширина карточки актёра и диаметр круглого аватара в ряду «Актёры» (крупнее мобильных — 10-foot UI). */
private val TvActorCardWidth = 120.dp
private val TvActorAvatarSize = 104.dp

private const val EPISODES_TITLE = "Эпизоды"

/** Фильм играется целиком, без выбора дорожки: плеер ждёт videoId = -1. */
private const val MOVIE_VIDEO_ID = -1

/** «Сезона нет» — фильм или сезон неизвестен (PlayerRoute.season = -1). */
private const val NO_SEASON = -1

private const val MAX_META_GENRES = 2
private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_MINUTE = 60

/**
 * Сериал определяем по ТИПУ тайтла, а не по числу дорожек: у фильма с двумя озвучками
 * `tracklist.size > 1`, и он получал селектор сезонов из одного бессмысленного сезона.
 */
private fun Item.isSeries(): Boolean =
    type == ItemType.SERIES || type == ItemType.ANIME || type == ItemType.DOCUMENTARY

/**
 * TV-Детали. Фильм и сериал — один вертикальный поток: hero, описание, эпизоды (сериал),
 * «Похожее». Поверх общего [DetailsScreenModel] (itemId берётся из маршрута через SavedStateHandle).
 */
@Composable
fun TvDetailsScreen(
    nav: TvDetailsNav,
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val item = state.item

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.loading -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )

            item != null -> DetailsContent(
                item = item,
                similar = state.similar,
                cast = state.cast,
                isFav = state.isFav,
                actions = DetailsActions(
                    onPlay = { season, videoId -> nav.onPlay(item.id, season, videoId) },
                    onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                    onOpenItem = nav.onOpenItem,
                    onOpenPerson = nav.onOpenPerson,
                    onPlayTrailer = nav.onPlayTrailer,
                ),
            )
        }
    }
}

/**
 * Навигация TV-деталей — группой (detekt LongParameterList): входной composable иначе набирает
 * больше шести параметров.
 */
data class TvDetailsNav(
    val onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
    val onOpenItem: (Int) -> Unit,
    /** Тап по актёру/режиссёру -> его фильмография (isDirector различает запрос к API). */
    val onOpenPerson: (name: String, isDirector: Boolean) -> Unit,
    /** Играть трейлер: прямой HLS-url и заголовок. */
    val onPlayTrailer: (url: String, title: String) -> Unit,
)

/** Действия экрана — группой, чтобы не раздувать списки параметров у вложенных секций. */
private data class DetailsActions(
    /** [season] ≤ 0 — фильм/сезон неизвестен; номер видео уникален только внутри сезона. */
    val onPlay: (season: Int, videoId: Int) -> Unit,
    val onToggleFav: () -> Unit,
    val onOpenItem: (Int) -> Unit,
    val onOpenPerson: (name: String, isDirector: Boolean) -> Unit,
    val onPlayTrailer: (url: String, title: String) -> Unit,
)

@Composable
private fun DetailsContent(
    item: Item,
    similar: List<Item>,
    cast: List<CastMember>,
    isFav: Boolean,
    actions: DetailsActions,
) {
    val series = remember(item) { if (item.isSeries()) calculateSeriesData(item.tracklist) else null }
    // Селектор стартует на сезоне недосмотренной серии, а не на первом: продолжают чаще, чем
    // начинают заново.
    var selectedSeason by remember(item.id) { mutableIntStateOf(series?.resumeSeasonIndex ?: 0) }
    val episodes = series?.seasons?.getOrNull(selectedSeason)?.second.orEmpty()

    val playFocus = remember { FocusRequester() }
    // Стартовый фокус — на «Смотреть»: экран открывается в стейте hero.
    LaunchedEffect(item.id) { runCatching { playFocus.requestFocus() } }

    // Кнопка играет недосмотренную серию, иначе первую серию ВЫБРАННОГО сезона (у фильма дорожка
    // не выбирается вовсе).
    val target = series?.let { it.resume ?: episodes.firstOrNull() ?: item.tracklist.firstOrNull() }
    // Трейлер показываем, только если url — играбельный http(s) (kino.pub отдаёт прямой HLS).
    val trailerUrl = item.trailer?.url?.takeIf { it.startsWith("http") }
    // Актёры карточками: фото из TMDB, если доехали; иначе — имена из строки kino.pub.
    val people = remember(cast, item.cast) { resolveCast(cast, item.cast) }

    val listState = rememberLazyListState()
    // false = стейт hero (открытие экрана), true = фокус ушёл в контент. Пока полотно в стейте
    // hero, фокус-прокрутка (bringIntoView) выключена ПОЛНОСТЬЮ: именно она давала подскролл к
    // середине при открытии — стартовый requestFocus на «Смотреть» уезжал раньше раскладки.
    val contentFocused = remember { mutableStateOf(false) }
    val onHeroFocusChanged = rememberHeroFocusScroller(listState, contentFocused)

    // Локальная функция вместо лямбды-в-лямбде (ktlint Wrapping): у тайтла без трейлера кнопки нет.
    fun playTrailer() {
        trailerUrl?.let { url -> actions.onPlayTrailer(url, "Трейлер · ${item.title}") }
    }

    CompositionLocalProvider(
        LocalBringIntoViewSpec provides
            if (contentFocused.value) LocalBringIntoViewSpec.current else NoFocusScroll,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = TvMetrics.SafeVertical, bottom = ContentBottomPadding),
        ) {
            item(key = "hero") {
                DetailsHero(
                    item = item,
                    series = series,
                    isFav = isFav,
                    playback = HeroPlayback(
                        playFocus = playFocus,
                        // Фильм играется целиком (videoId = -1), сериал — конкретной серией. Сериал
                        // без серий играть нечем — кнопка молчит. В плеер уходят НОМЕР серии и
                        // СЕЗОН: номер уникален только внутри сезона.
                        onPlay = {
                            if (series == null) {
                                actions.onPlay(NO_SEASON, MOVIE_VIDEO_ID)
                            } else {
                                target?.let { actions.onPlay(it.seasonNumber, it.number) }
                            }
                        },
                        onToggleFav = actions.onToggleFav,
                        onHeroFocusChanged = onHeroFocusChanged,
                        onTrailer = trailerUrl?.let { ::playTrailer },
                    ),
                )
            }
            detailsSections(
                data = DetailsSectionsData(item, similar, people, series, episodes, selectedSeason),
                actions = actions,
                onSelectSeason = { selectedSeason = it },
            )
        }
    }
}

/**
 * Переключатель двух стейтов полотна по фокусу кнопок hero. Стейт 1: фокус на кнопках —
 * полотно к началу, hero виден целиком (плюс описание под ним). Стейт 2: фокус ушёл с кнопок
 * вниз — полотно едет к описанию, hero скрывается прокруткой. Всё это ОДИН LazyColumn:
 * ничего не накладывается и не режется. Начальная композиция (фокуса ещё не было) — не выход.
 */
@Composable
private fun rememberHeroFocusScroller(
    listState: LazyListState,
    contentFocused: MutableState<Boolean>,
): (Boolean) -> Unit {
    val scope = rememberCoroutineScope()
    var heroHadFocus by remember { mutableStateOf(false) }

    // Прокрутка к стейту — через кадр: смена фокуса в этом же кадре запускает системный
    // bringIntoView, и без паузы он перехватывал бы нашу прокрутку (полотно застревало на
    // полпути, верх постера оставался срезанным). Более поздний вызов забирает scroll-мьютекс
    // списка себе — поэтому пропускаем кадры и едем к цели последними.
    fun scrollAfterFrame(targetIndex: Int) {
        scope.launch {
            repeat(FRAMES_BEFORE_STATE_SCROLL) { withFrameNanos { } }
            listState.animateScrollToItem(targetIndex)
        }
    }

    return { focused ->
        if (focused) {
            heroHadFocus = true
            contentFocused.value = false
            scrollAfterFrame(0)
        } else if (heroHadFocus) {
            heroHadFocus = false
            contentFocused.value = true
            scrollAfterFrame(CONTENT_START_INDEX)
        }
    }
}

/**
 * Спека «не скроллить»: пока полотно в стейте hero, любой bringIntoView от фокуса гасится —
 * позицией полотна управляет только [rememberHeroFocusScroller]. Включается обратно, когда
 * фокус уходит в контент: там штатная фокус-прокрутка нужна для глубоких рядов.
 */
private val NoFocusScroll = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

/** Данные секций полотна под hero — группой (detekt LongParameterList). */
private class DetailsSectionsData(
    val item: Item,
    val similar: List<Item>,
    val people: List<CastMember>,
    val series: SeriesData?,
    val episodes: List<MediaTrack>,
    val selectedSeason: Int,
)

/** Секции полотна под hero: описание, актёры, режиссёр, эпизоды, «Похожее». */
private fun LazyListScope.detailsSections(
    data: DetailsSectionsData,
    actions: DetailsActions,
    onSelectSeason: (Int) -> Unit,
) {
    item(key = "about") { DetailsAbout(data.item) }
    if (data.people.isNotEmpty()) {
        castRail(people = data.people, onOpenPerson = actions.onOpenPerson)
    }
    if (data.item.director.isNotBlank()) {
        directorSection(director = data.item.director, onOpenPerson = actions.onOpenPerson)
    }
    if (data.episodes.isNotEmpty()) {
        episodesSection(
            EpisodesSection(
                seasons = data.series?.seasons.orEmpty(),
                episodes = data.episodes,
                resumeId = data.series?.resume?.id,
                selectedSeason = data.selectedSeason,
                onSelectSeason = onSelectSeason,
                onPlayEpisode = actions.onPlay,
            )
        )
    }
    if (data.similar.isNotEmpty()) {
        similarRail(similar = data.similar, onOpenItem = actions.onOpenItem)
    }
}

// ─────────────────────────────────── Hero ───────────────────────────────────

/** Фокус и действия кнопок hero — группой (detekt LongParameterList). */
private data class HeroPlayback(
    val playFocus: FocusRequester,
    val onPlay: () -> Unit,
    val onToggleFav: () -> Unit,
    /** Фокус зашёл на кнопки hero или ушёл с них — экран переключает стейт полотна. */
    val onHeroFocusChanged: (Boolean) -> Unit,
    /** null — у тайтла нет играбельного трейлера, кнопки нет. */
    val onTrailer: (() -> Unit)? = null,
)

/**
 * Hero: бэкдроп во всю ширину, текстовый блок прижат к низу слева (вариант A макета).
 *
 * Высота фиксированная: hero — первый элемент единого полотна и скрывается обычной прокруткой,
 * когда фокус уходит в контент, а не сжимается поверх него. Так постер всегда либо виден
 * целиком, либо честно уезжает вверх — ничего не режется.
 */
@Composable
private fun DetailsHero(
    item: Item,
    series: SeriesData?,
    isFav: Boolean,
    playback: HeroPlayback,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(TvMetrics.DetailsHeroHeight),
    ) {
        HeroBackdrop(
            item = item,
            scrims = heroScrims(),
            modifier = Modifier.fillMaxSize(),
            posterUrl = item.posters.wide ?: item.posters.big,
            // Заглушка постера — нейтральная поверхность: цвет на экране только у самого кадра.
            accentColor = TvSurfaceContainerHigh,
        )

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = TvMetrics.SafeHorizontal, bottom = 22.dp)
                .width(HeroTextWidth),
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.displaySmall,
                color = TvOnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TvMetaRow(
                parts = remember(item, series) { metaParts(item, series) },
                modifier = Modifier.padding(top = 11.dp),
            )
            RatingsRow(rating = item.rating, modifier = Modifier.padding(top = 9.dp))
            HeroButtons(
                isFav = isFav,
                resume = series?.resume,
                playback = playback,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

/**
 * Скримы hero. Стопы длинные и с промежуточными точками: в монохроме переход серого в серый
 * на коротком отрезке полосит (бандинг), а уход в прозрачность берём как `TvSurface` с нулевой
 * альфой — интерполяция в `Color.Transparent` тянет RGB к чёрному и даёт грязный «хвост».
 */
@Composable
private fun heroScrims(): List<Brush> = remember {
    listOf(
        Brush.horizontalGradient(
            0f to TvSurface.copy(alpha = 0.95f),
            0.40f to TvSurface.copy(alpha = 0.72f),
            0.72f to TvSurface.copy(alpha = 0.20f),
            1f to TvSurface.copy(alpha = 0f),
        ),
        Brush.verticalGradient(
            0f to TvSurface.copy(alpha = 0f),
            0.22f to TvSurface.copy(alpha = 0f),
            0.60f to TvSurface.copy(alpha = 0.35f),
            1f to TvSurface.copy(alpha = 0.98f),
        ),
    )
}

@Composable
private fun HeroButtons(
    isFav: Boolean,
    resume: MediaTrack?,
    playback: HeroPlayback,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.onFocusChanged { playback.onHeroFocusChanged(it.hasFocus) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TvButton(
            text = playLabel(resume),
            onClick = playback.onPlay,
            leadingIcon = Icons.Filled.PlayArrow,
            focusRequester = playback.playFocus,
        )
        TvButton(
            text = if (isFav) "В списке" else "Буду смотреть",
            onClick = playback.onToggleFav,
            primary = false,
            leadingIcon = if (isFav) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
        )
        playback.onTrailer?.let { onTrailer ->
            TvButton(
                text = "Трейлер",
                onClick = onTrailer,
                primary = false,
                leadingIcon = Icons.Filled.Movie,
            )
        }
    }
}

/**
 * КП и IMDb показываем РАЗДЕЛЬНО: `rating.external` усредняет их, а расхождение оценок — это
 * и есть причина смотреть обе. Цветового кодирования нет: в монохроме оценку несёт число.
 */
@Composable
private fun RatingsRow(rating: ItemRating, modifier: Modifier = Modifier) {
    // ratingLabel режет «0» (у kino.pub это «оценки нет») и приводит «8.312» к одному знаку.
    val sources = remember(rating) {
        buildList {
            ratingLabel(rating.kinopoisk)?.let { add(it to "КиноПоиск") }
            ratingLabel(rating.imdb)?.let { add(it to "IMDb") }
        }
    }
    if (sources.isEmpty()) return

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        sources.forEachIndexed { index, (value, source) ->
            if (index > 0) {
                Box(
                    Modifier
                        .size(width = 1.dp, height = 14.dp)
                        .background(TvSurfaceContainerHighest),
                )
            }
            RatingValue(value = value, source = source)
        }
    }
}

@Composable
private fun RatingValue(value: String, source: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TvOnSurface)
        Text(source, style = MaterialTheme.typography.bodyLarge, color = TvOnSurfaceVariant)
    }
}

// ─────────────────────────── Описание и состав ──────────────────────────────

@Composable
private fun DetailsAbout(item: Item) {
    if (item.plot.isNotBlank()) {
        Text(
            item.plot,
            style = MaterialTheme.typography.bodyLarge,
            color = TvOnSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = TvMetrics.SafeHorizontal, end = TvMetrics.SafeHorizontal, top = 22.dp)
                .widthIn(max = ReadableTextWidth),
        )
    }
}

// ─────────────────────────────── Актёры и режиссёр ────────────────────────────

/**
 * Ряд актёров карточками с круглым аватаром. Фото приходят из TMDB ([DetailsState.cast]); пока
 * их нет — те же карточки с инициалами (имена всегда есть от kino.pub). Каждая карточка ведёт в
 * фильмографию человека, поэтому каст на TV наконец фокусируемый и кликабельный, а не мёртвая строка.
 */
private fun LazyListScope.castRail(people: List<CastMember>, onOpenPerson: (String, Boolean) -> Unit) {
    item(key = "cast") {
        TvRail(title = "Актёры", modifier = Modifier.padding(top = 24.dp)) { firstItemFocus ->
            // Без key: имена в составе могут повторяться, позиционного ключа достаточно.
            itemsIndexed(people) { index, member ->
                TvActorCard(
                    member = member,
                    onClick = { onOpenPerson(member.name, false) },
                    focusRequester = firstItemFocus.takeIf { index == 0 },
                )
            }
        }
    }
}

/** Режиссёр отдельным фокусируемым чипом под рядом актёров — ведёт в его фильмографию. */
private fun LazyListScope.directorSection(director: String, onOpenPerson: (String, Boolean) -> Unit) {
    item(key = "director") {
        Column(
            Modifier.padding(start = TvMetrics.SafeHorizontal, top = 22.dp),
        ) {
            TvOverline("Режиссёр", Modifier.padding(bottom = 8.dp))
            Row(Modifier.padding(vertical = TvMetrics.FocusInset)) {
                TvChip(
                    label = director,
                    selected = false,
                    // По запятой — только первый режиссёр: kino.pub ищет по одному имени.
                    onClick = { onOpenPerson(director.substringBefore(",").trim(), true) },
                )
            }
        }
    }
}

/** Карточка актёра: круглый аватар (фото TMDB или инициалы) + имя. Фокус/скейл — как у медиа-карточек. */
@Composable
private fun TvActorCard(member: CastMember, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    var focused by remember { mutableStateOf(false) }
    val dim = rememberDimAlpha(focused)
    Column(
        modifier = Modifier
            .width(TvActorCardWidth)
            .onFocusChanged { focused = it.hasFocus }
            .alpha(dim),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TvFocusCard(
            onClick = onClick,
            shape = CircleShape,
            focusRequester = focusRequester,
            modifier = Modifier.size(TvActorAvatarSize),
        ) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape).background(TvSurfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                val photo = member.photoUrl
                if (photo != null) {
                    PosterImage(
                        url = photo,
                        contentDescription = member.name,
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        accentColor = TvSurfaceContainerHigh,
                    )
                } else {
                    Text(
                        initials(member.name),
                        style = MaterialTheme.typography.titleMedium,
                        color = TvOnSurfaceVariant,
                    )
                }
            }
        }
        Text(
            member.name,
            style = MaterialTheme.typography.bodyMedium,
            color = TvOnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

// ────────────────────────────── Эпизоды сериала ──────────────────────────────

/** Данные и действия секции эпизодов — группой (detekt LongParameterList). */
private data class EpisodesSection(
    val seasons: List<Pair<Int, List<MediaTrack>>>,
    val episodes: List<MediaTrack>,
    val resumeId: Int?,
    val selectedSeason: Int,
    val onSelectSeason: (Int) -> Unit,
    val onPlayEpisode: (season: Int, videoId: Int) -> Unit,
)

/**
 * Секция эпизодов: заголовок → чипы сезонов → ряд карточек серий.
 *
 * Чипы — горизонтальный ряд, а не FlowRow с переносом: у сериала на 8+ сезонов перенос забирал
 * под чипы половину экрана. Чипы и карточки — разные ряды LazyColumn, поэтому «вниз» с чипов
 * ведёт в серии, а не прыгает через них.
 */
private fun LazyListScope.episodesSection(section: EpisodesSection) {
    if (section.seasons.size > 1) {
        item(key = "seasons") {
            TvRail(title = EPISODES_TITLE, modifier = Modifier.padding(top = 24.dp)) { firstItemFocus ->
                itemsIndexed(section.seasons, key = { _, season -> season.first }) { index, season ->
                    val number = season.first
                    TvChip(
                        label = if (number > 0) "Сезон $number" else "Серии",
                        selected = index == section.selectedSeason,
                        onClick = { section.onSelectSeason(index) },
                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier,
                    )
                }
            }
        }
    } else {
        // Один сезон — селектор не нужен, но заголовок секции остаётся.
        item(key = "episodes-title") {
            SectionTitle(EPISODES_TITLE, Modifier.padding(top = 24.dp))
        }
    }

    item(key = "episodes") {
        EpisodesRow(
            episodes = section.episodes,
            resumeId = section.resumeId,
            selectedSeason = section.selectedSeason,
            onPlay = section.onPlayEpisode,
        )
    }
}

/** Заголовок секции, когда над рядом нет чипов (TvRail рисует заголовок вплотную к своему ряду). */
@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = TvOnSurface,
        modifier = modifier.padding(start = TvMetrics.SafeHorizontal, bottom = 12.dp),
    )
}

/**
 * Ряд серий. Свой LazyRow, а не [TvRail]: заголовок «Эпизоды» стоит над чипами сезонов, а
 * TvRail жёстко ставит заголовок над своим рядом. Отступы и focusRestorer — как у TvRail.
 *
 * Выбор другого сезона пересоздаёт карточки (новые key), но LazyRow хранит прежний скролл,
 * а focusRestorer — уже не существующего ребёнка: вход в ряд отдавался D-pad-поиску и сажал
 * фокус на серию под чипом сезона, а не на первую. Поэтому смена сезона возвращает скролл
 * к началу, а fallback-фокус закреплён за первой серией.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EpisodesRow(
    episodes: List<MediaTrack>,
    resumeId: Int?,
    selectedSeason: Int,
    onPlay: (season: Int, videoId: Int) -> Unit,
) {
    val firstItemFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    LaunchedEffect(selectedSeason) { listState.scrollToItem(0) }
    LazyRow(
        state = listState,
        modifier = Modifier.focusRestorer(firstItemFocus),
        contentPadding = PaddingValues(
            start = TvMetrics.SafeHorizontal,
            end = TvMetrics.SafeHorizontal,
            top = TvMetrics.FocusInset,
            bottom = TvMetrics.FocusInset,
        ),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
    ) {
        itemsIndexed(episodes, key = { _, episode -> episode.id }) { index, episode ->
            EpisodeCard(
                episode = episode,
                isResume = episode.id == resumeId,
                focusRequester = if (index == 0) firstItemFocus else null,
                // Плееру нужны номер серии (API `video`) и сезон, а не id трека.
                onClick = { onPlay(episode.seasonNumber, episode.number) },
            )
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: MediaTrack,
    isResume: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    val progress = if (episode.durationSeconds > 0) {
        episode.watchedSeconds.toFloat() / episode.durationSeconds
    } else {
        0f
    }
    TvProgressCard(
        title = episode.title.ifBlank { "Серия ${episode.number}" },
        meta = episodeMeta(episode),
        posterUrl = episode.thumbnail,
        progress = progress,
        onClick = onClick,
        size = TvCardSize.Episode,
        focusRequester = focusRequester,
    ) { url, modifier ->
        EpisodeThumb(url = url, episode = episode, isResume = isResume, modifier = modifier)
    }
}

/**
 * Превью серии: кадр, а если его нет — крупный номер серии (у kino.pub thumbnail часто пустой,
 * и пустая плитка не отличима от соседней).
 */
@Composable
private fun EpisodeThumb(url: String, episode: MediaTrack, isResume: Boolean, modifier: Modifier) {
    Box(modifier.background(TvSurfaceContainer), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            PosterImage(
                url = url,
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
                shape = TvMetrics.CardShape,
                accentColor = TvSurfaceContainerHigh,
            )
        } else {
            Text(
                "${episode.number}",
                style = MaterialTheme.typography.headlineMedium,
                color = TvOnSurfaceVariant,
            )
        }
        if (isResume) {
            // Явный бейдж вместо слова «продолжить» в строке меты: в ряду из десятка одинаковых
            // плиток текстовый признак не находится взглядом.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(TvMetrics.PosterShape)
                    .background(TvSurface.copy(alpha = 0.78f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("Продолжить", style = MaterialTheme.typography.labelSmall, color = TvOnSurface)
            }
        }
    }
}

// ─────────────────────────────────── Похожее ─────────────────────────────────

private fun LazyListScope.similarRail(similar: List<Item>, onOpenItem: (Int) -> Unit) {
    item(key = "similar") {
        TvRail(title = "Похожее", modifier = Modifier.padding(top = 26.dp)) { firstItemFocus ->
            itemsIndexed(similar, key = { _, simItem -> simItem.id }) { index, simItem ->
                TvPosterCard(
                    title = simItem.title,
                    meta = posterMeta(typeLabel(simItem.type), simItem.year),
                    posterUrl = simItem.posters.medium.ifEmpty { simItem.posters.big },
                    onClick = { onOpenItem(simItem.id) },
                    rating = ratingLabel(simItem.rating.kinopoisk),
                    focusRequester = firstItemFocus.takeIf { index == 0 },
                ) { url, modifier ->
                    PosterImage(
                        url = url,
                        contentDescription = simItem.title,
                        modifier = modifier,
                        shape = TvMetrics.PosterShape,
                        accentColor = TvSurfaceContainerHigh,
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Производные данные ────────────────────────────

/**
 * Производные данные сериала для экрана: сезоны (сгруппированы и отсортированы) и точка «продолжить».
 * Отдельная чистая модель вместо переплетённых remember-блоков в Composable.
 */
private data class SeriesData(
    /** Пары «номер сезона → серии по порядку», отсортированные по номеру сезона. */
    val seasons: List<Pair<Int, List<MediaTrack>>>,
    /** Эпизод для «продолжить»: в процессе → следующая после досмотренной → иначе null. */
    val resume: MediaTrack?,
    /** Индекс сезона эпизода «продолжить» в [seasons] (0, если не определён). */
    val resumeSeasonIndex: Int,
)

/** `watching.status` из API: 0 — серия в процессе, 1 — досмотрена, -1 — не начата. */
private const val WATCH_STATUS_IN_PROGRESS = 0
private const val WATCH_STATUS_FINISHED = 1

/** Считает [SeriesData] из плейлиста серий — чистая функция, тестируемая отдельно от UI. */
private fun calculateSeriesData(tracklist: List<MediaTrack>): SeriesData {
    val seasons = tracklist
        .groupBy { it.seasonNumber }
        .toSortedMap()
        .map { (number, episodes) -> number to episodes.sortedBy { it.number } }
    val resume = resumeEpisode(seasons)
    val resumeSeasonIndex = resume
        ?.let { episode -> seasons.indexOfFirst { it.first == episode.seasonNumber }.takeIf { it >= 0 } }
        ?: 0
    return SeriesData(seasons = seasons, resume = resume, resumeSeasonIndex = resumeSeasonIndex)
}

/**
 * Точка «продолжить»: недосмотренная серия → СЛЕДУЮЩАЯ после последней досмотренной
 * («продолжить» — это смотреть дальше, а не пересматривать) → всё досмотрено — последняя
 * (пересмотр). Порядок — по отсортированным сезонам, а не по сырому tracklist.
 */
private fun resumeEpisode(seasons: List<Pair<Int, List<MediaTrack>>>): MediaTrack? {
    val ordered = seasons.flatMap { (_, episodes) -> episodes }
    val inProgress = ordered.firstOrNull { it.watchStatus == WATCH_STATUS_IN_PROGRESS }
    val lastWatched = ordered.indexOfLast { it.watchStatus == WATCH_STATUS_FINISHED }
    return when {
        inProgress != null -> inProgress
        lastWatched >= 0 -> ordered.getOrNull(lastWatched + 1) ?: ordered[lastWatched]
        else -> null
    }
}

/** Мета-строка hero: год · объём/длительность · страна · жанры. Пустые части выпадают. */
private fun metaParts(item: Item, series: SeriesData?): List<String> = buildList {
    if (item.year > 0) add(item.year.toString())
    volumeLabel(item, series)?.let { add(it) }
    if (item.country.isNotBlank()) add(item.country)
    if (item.genres.isNotEmpty()) {
        add(item.genres.take(MAX_META_GENRES).joinToString(", ") { it.title })
    }
}

/**
 * У фильма в мете длительность, у сериала — объём: «3 сезона», а у односезонного «12 серий»
 * («1 сезон» не сообщает ничего).
 */
private fun volumeLabel(item: Item, series: SeriesData?): String? = when {
    series == null -> item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let { durationLabel(it) }
    series.seasons.size > 1 -> "${series.seasons.size} ${seasonsWord(series.seasons.size)}"
    item.tracklist.isNotEmpty() -> "${item.tracklist.size} ${episodesWord(item.tracklist.size)}"
    else -> null
}

/** «2 ч 46 мин» / «46 мин» — часы опускаем, когда их нет. */
private fun durationLabel(minutes: Int): String {
    val hours = minutes / MINUTES_IN_HOUR
    val rest = minutes % MINUTES_IN_HOUR
    return if (hours > 0) "$hours ч $rest мин" else "$rest мин"
}

/** «Продолжить · S2E5» — сериал с недосмотренной серией; иначе «Смотреть». */
private fun playLabel(resume: MediaTrack?): String = when {
    resume == null -> "Смотреть"
    resume.seasonNumber > 0 -> "Продолжить · S${resume.seasonNumber}E${resume.number}"
    else -> "Продолжить · Серия ${resume.number}"
}

/**
 * Люди для ряда «Актёры»: фото из TMDB, если доехали; иначе — карточки из строки имён kino.pub
 * (`item.cast`, имена через запятую) без фото. Каст кликабелен всегда, фото — дополнение.
 */
private fun resolveCast(cast: List<CastMember>, rawCast: String): List<CastMember> =
    cast.ifEmpty {
        rawCast.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { name -> CastMember(name = name, character = null, photoUrl = null) }
    }

/** Инициалы для заглушки без фото: до двух заглавных букв из имени. */
private fun initials(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { word -> word.first().uppercaseChar() }
        .joinToString("")

/** Мета карточки серии: «Серия 3 · 45 мин». Номер опускаем, если он уже стал заголовком. */
private fun episodeMeta(episode: MediaTrack): String? = buildList {
    if (episode.title.isNotBlank()) add("Серия ${episode.number}")
    episode.durationSeconds.takeIf { it > 0 }?.let { add("${it / SECONDS_IN_MINUTE} мин") }
}.joinToString(" · ").ifBlank { null }

private fun typeLabel(type: ItemType): String = when (type) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY -> "Док. сериал"
    ItemType.TV -> "ТВ"
}

// Модули русских правил склонения по числу (последние две / одна цифра).
private const val PLURAL_MOD_HUNDRED = 100
private const val PLURAL_MOD_TEN = 10

private fun seasonsWord(count: Int): String = when {
    count % PLURAL_MOD_HUNDRED in 11..14 -> "сезонов"
    count % PLURAL_MOD_TEN == 1 -> "сезон"
    count % PLURAL_MOD_TEN in 2..4 -> "сезона"
    else -> "сезонов"
}

private fun episodesWord(count: Int): String = when {
    count % PLURAL_MOD_HUNDRED in 11..14 -> "серий"
    count % PLURAL_MOD_TEN == 1 -> "серия"
    count % PLURAL_MOD_TEN in 2..4 -> "серии"
    else -> "серий"
}
