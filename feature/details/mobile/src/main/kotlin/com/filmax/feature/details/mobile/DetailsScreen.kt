// Экран деталей — один вертикальный поток и для фильма, и для сериала: кадр, заголовок с метой и
// рейтингами, кнопки, описание, эпизоды (сериал) и «Похожее». Табы «О фильме»/«Актёры» убраны:
// делить между ними было нечего — описание и строка состава живут одним экраном.
//
// Секции — отдельные composable ОДНОГО экрана, а не переиспользуемые компоненты: по файлам их
// растаскивать нечего, читаются они только вместе. Suppress нужен из-за не-Composable хелперов
// (склонения, форматирование меты, производные сериала) — их набирается больше порога, а
// @Composable detekt уже не считает.
@file:Suppress("TooManyFunctions")

package com.filmax.feature.details.mobile

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.designsystem.ShapePoster
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.person.CastMember
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxProgressBar
import com.filmax.core.ui.components.HeroBackdrop
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import com.filmax.feature.details.common.SeriesData
import com.filmax.feature.details.common.WATCH_STATUS_FINISHED
import com.filmax.feature.details.common.WATCH_STATUS_IN_PROGRESS
import com.filmax.feature.details.common.calculateSeriesData
import com.filmax.feature.details.common.initials
import com.filmax.feature.details.common.isSeries
import com.filmax.feature.details.common.resolveCast
import com.filmax.feature.details.common.typeLabel
import com.filmax.feature.details.common.volumeLabel
import org.koin.androidx.compose.koinViewModel

/** Фильм играется целиком, без выбора дорожки: плеер ждёт videoId = -1. */
private const val MOVIE_VIDEO_ID = -1

/** «Сезона нет» — фильм или сезон неизвестен (PlayerRoute.season = -1). */
private const val NO_SEASON = -1

private const val SECONDS_IN_MINUTE = 60

/**
 * Отступ кнопки «назад» под статус-баром. В макете она в 44dp от верха кадра, но кадр макета
 * нарисован без статус-бара, а приложение живёт edge-to-edge — те же 44dp набираются
 * `statusBarsPadding()` плюс это поле.
 */
private val BackButtonInset = 8.dp

/** Ширина карточки актёра и диаметр круглого аватара в секции «Актёры». */
private val ActorCardWidth = 88.dp
private val ActorAvatarSize = 72.dp

private const val EPISODES_TITLE = "Эпизоды"

/**
 * Мобильные Детали поверх общего [DetailsScreenModel] (itemId берётся из маршрута через
 * SavedStateHandle).
 *
 * [onPlay] вторым аргументом принимает НОМЕР серии, а не её id: тем же числом kino.pub и
 * принимает, и отдаёт прогресс (`watching/marktime?video=`). Фильм играется целиком —
 * [MOVIE_VIDEO_ID].
 */
/**
 * Навигация экрана деталей — группой (detekt LongParameterList): входной composable иначе набирает
 * больше шести параметров. Так же сгруппированы колбэки у других экранов-входов (напр. HomeActions).
 */
data class DetailsNav(
    val onBack: () -> Unit,
    val onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
    val onOpenItem: (Int) -> Unit,
    /** Тап по актёру/режиссёру -> его фильмография (isDirector различает запрос к API). */
    val onOpenPerson: (name: String, isDirector: Boolean) -> Unit,
    /** Играть трейлер: прямой HLS-url и заголовок. */
    val onPlayTrailer: (url: String, title: String) -> Unit,
)

@Composable
fun DetailsScreen(
    nav: DetailsNav,
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val item = state.item
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when {
            state.loading -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )

            item != null -> DetailsContent(
                item = item,
                extras = DetailsExtras(similar = state.similar, cast = state.cast),
                isFav = state.isFav,
                actions = DetailsActions(
                    onBack = nav.onBack,
                    onPlay = { season, videoId -> nav.onPlay(item.id, season, videoId) },
                    onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                    onShare = { shareItem(context, item) },
                    onOpenItem = nav.onOpenItem,
                    onOpenPerson = nav.onOpenPerson,
                    onPlayTrailer = nav.onPlayTrailer,
                ),
            )
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

/** Действия экрана — группой, чтобы не раздувать списки параметров вложенных секций. */
private data class DetailsActions(
    val onBack: () -> Unit,
    /** [season] ≤ 0 — фильм/сезон неизвестен; номер видео уникален только внутри сезона. */
    val onPlay: (season: Int, videoId: Int) -> Unit,
    val onToggleFav: () -> Unit,
    val onShare: () -> Unit,
    val onOpenItem: (Int) -> Unit,
    /** Тап по актёру/режиссёру — открыть его фильмографию (isDirector различает запрос к API). */
    val onOpenPerson: (name: String, isDirector: Boolean) -> Unit,
    /** Играть трейлер: прямой HLS-url и заголовок «Трейлер · Название». */
    val onPlayTrailer: (url: String, title: String) -> Unit,
)

/** Дополнительные коллекции экрана — похожее и каст (группой, detekt LongParameterList). */
private data class DetailsExtras(
    val similar: List<Item>,
    val cast: List<CastMember>,
)

/** Сериальная часть состояния экрана — группой (detekt LongParameterList). */
private data class SeriesUiState(
    /** null — тайтл не сериал: ни сезонов, ни выбора дорожки. */
    val data: SeriesData?,
    /** Серии ВЫБРАННОГО сезона. */
    val episodes: List<MediaTrack>,
    val selectedSeason: Int,
    val onSelectSeason: (Int) -> Unit,
)

@Composable
private fun DetailsContent(
    item: Item,
    extras: DetailsExtras,
    isFav: Boolean,
    actions: DetailsActions,
) {
    val series = remember(item) { if (item.isSeries()) calculateSeriesData(item.tracklist) else null }
    // Селектор стартует на сезоне недосмотренной серии, а не на первом: продолжают чаще, чем
    // начинают заново.
    var selectedSeason by remember(item.id) { mutableIntStateOf(series?.resumeSeasonIndex ?: 0) }
    val seriesUi = SeriesUiState(
        data = series,
        episodes = series?.seasons?.getOrNull(selectedSeason)?.second.orEmpty(),
        selectedSeason = selectedSeason,
        onSelectSeason = { selectedSeason = it },
    )
    val overlap = if (series == null) FilmaxMetrics.DetailsTitleOverlap else FilmaxMetrics.SeriesTitleOverlap

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        DetailsHero(
            item = item,
            height = if (series == null) FilmaxMetrics.DetailsHeroHeight else FilmaxMetrics.SeriesHeroHeight,
            onBack = actions.onBack,
        )
        // Заголовок заезжает на низ кадра (в макете `margin-top:-52px`). Сдвиг забирает столько же
        // на хвосте скролла — там это просто нижнее поле, поэтому отдельно не компенсируем.
        Column(Modifier.offset(y = -overlap)) {
            DetailsBody(
                item = item,
                extras = extras,
                isFav = isFav,
                series = seriesUi,
                actions = actions,
            )
        }
    }
}

@Composable
private fun DetailsBody(
    item: Item,
    extras: DetailsExtras,
    isFav: Boolean,
    series: SeriesUiState,
    actions: DetailsActions,
) {
    val sidePadding = Modifier.padding(horizontal = FilmaxMetrics.DetailsPadding)
    // Кнопка играет недосмотренную серию, иначе первую серию ВЫБРАННОГО сезона.
    val target = series.data?.let { it.resume ?: series.episodes.firstOrNull() ?: item.tracklist.firstOrNull() }
    // Трейлер показываем, только если url — играбельный http(s) (kino.pub отдаёт прямой HLS).
    val trailerUrl = item.trailer?.url?.takeIf { it.startsWith("http") }
    // Люди для секции «Актёры»: фото из TMDB, если доехали; иначе — имена из строки kino.pub.
    val people = remember(extras.cast, item.cast) { resolveCast(extras.cast, item.cast) }

    DetailsHeader(item = item, series = series.data, modifier = sidePadding)
    DetailsButtons(
        isFav = isFav,
        resume = series.data?.resume,
        actions = DetailsButtonActions(
            onPlay = {
                // Сериал без серий играть нечем — кнопка молчит, а не открывает пустой плеер.
                // В плеер уходят номер серии И сезон: номер уникален только внутри сезона.
                if (series.data == null) {
                    actions.onPlay(NO_SEASON, MOVIE_VIDEO_ID)
                } else {
                    target?.let { actions.onPlay(it.seasonNumber, it.number) }
                }
            },
            onToggleFav = actions.onToggleFav,
            onShare = actions.onShare,
            onTrailer = trailerUrl?.let { url -> { actions.onPlayTrailer(url, "Трейлер · ${item.title}") } },
        ),
        modifier = sidePadding.padding(top = 18.dp),
    )
    DetailsAbout(item = item, modifier = sidePadding.padding(top = 18.dp))
    CastSection(
        people = people,
        director = item.director,
        onOpenPerson = actions.onOpenPerson,
        modifier = Modifier.padding(top = 22.dp),
    )
    if (series.episodes.isNotEmpty()) {
        EpisodesSection(
            data = EpisodesSectionData(
                seasons = series.data?.seasons.orEmpty(),
                episodes = series.episodes,
                selectedSeason = series.selectedSeason,
                onSelectSeason = series.onSelectSeason,
                onPlayEpisode = actions.onPlay,
            ),
            modifier = Modifier.padding(top = 24.dp),
        )
    }
    // «Похожее» — и у фильма, и у сериала: state.similar грузится всегда, а экран его молча
    // выбрасывал.
    if (extras.similar.isNotEmpty()) {
        SimilarSection(
            similar = extras.similar,
            onOpenItem = actions.onOpenItem,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
    Spacer(Modifier.height(24.dp))
}

// ─────────────────────────────────── Кадр ────────────────────────────────────

@Composable
private fun DetailsHero(item: Item, height: Dp, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(height)) {
        HeroBackdrop(
            item = item,
            scrims = listOf(heroScrim()),
            modifier = Modifier.matchParentSize(),
            posterUrl = item.posters.wide ?: item.posters.big,
            // Заглушка постера — нейтральная поверхность: цвет на экране только у самого кадра.
            accentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        BackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = BackButtonInset),
        )
    }
}

/**
 * Скрим кадра. Макет: `linear-gradient(0deg, #0A0A0A 4%, rgba(10,10,10,.15) 55%, rgba(10,10,10,.5)
 * 100%)` — CSS-градус 0 считает снизу вверх, поэтому стопы здесь развёрнуты.
 *
 * Прозрачность берём как surface с нулевой альфой, а НЕ `Color.Transparent`: у последнего нулевые
 * RGB, и интерполяция тянет переход через чёрный — на градиенте появляется грязный хвост.
 */
@Composable
private fun heroScrim(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    return remember(surface) {
        Brush.verticalGradient(
            0f to surface.copy(alpha = 0.5f),
            0.45f to surface.copy(alpha = 0.15f),
            0.96f to surface,
            1f to surface,
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(FilmaxMetrics.BackButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ──────────────────────── Заголовок, мета и рейтинги ─────────────────────────

@Composable
private fun DetailsHeader(item: Item, series: SeriesData?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            item.title,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        MetaRow(
            parts = remember(item, series) { metaParts(item, series) },
            modifier = Modifier.padding(top = 10.dp),
        )
        RatingsRow(rating = item.rating, modifier = Modifier.padding(top = 10.dp))
    }
}

/**
 * Мета-строка: «Фильм · 2024 · 2 ч 46 мин · США». Одним [Text], а не рядом из Text и точек: строка
 * переносится сама, а ряд на узком экране пришлось бы городить через FlowRow.
 */
@Composable
private fun MetaRow(parts: List<String>, modifier: Modifier = Modifier) {
    if (parts.isEmpty()) return
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * КП и IMDb показываем РАЗДЕЛЬНО: `rating.external` их усредняет, а расхождение оценок — это и есть
 * причина смотреть обе. Цветового кодирования нет: в монохроме оценку несёт число.
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
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        sources.forEachIndexed { index, (value, source) ->
            if (index > 0) {
                Box(
                    Modifier
                        .size(width = 1.dp, height = 13.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
            RatingValue(value = value, source = source)
        }
    }
}

@Composable
private fun RatingValue(value: String, source: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(source, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ────────────────────────────────── Кнопки ───────────────────────────────────

/** Действия кнопок — группой (detekt LongParameterList). */
private data class DetailsButtonActions(
    val onPlay: () -> Unit,
    val onToggleFav: () -> Unit,
    val onShare: () -> Unit,
    /** null — у тайтла нет играбельного трейлера, кнопки нет. */
    val onTrailer: (() -> Unit)? = null,
)

@Composable
private fun DetailsButtons(
    isFav: Boolean,
    resume: MediaTrack?,
    actions: DetailsButtonActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PlayButton(text = playLabel(resume), onClick = actions.onPlay)
        Row(
            Modifier.padding(top = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WatchlistButton(
                isFav = isFav,
                onClick = actions.onToggleFav,
                modifier = Modifier.weight(1f),
            )
            actions.onTrailer?.let { onTrailer ->
                IconSquareButton(
                    icon = Icons.Filled.Movie,
                    contentDescription = "Трейлер",
                    onClick = onTrailer,
                )
            }
            IconSquareButton(
                icon = Icons.Filled.Share,
                contentDescription = "Поделиться",
                onClick = actions.onShare,
            )
        }
    }
}

@Composable
private fun PlayButton(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.PrimaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun WatchlistButton(isFav: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(FilmaxMetrics.SecondaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isFav) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
        Text(
            if (isFav) "В списке" else "Буду смотреть",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Квадратная вторичная кнопка-иконка (Поделиться, Трейлер) — одна форма на оба действия. */
@Composable
private fun IconSquareButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(FilmaxMetrics.SecondaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ────────────────────────── Описание и строка состава ─────────────────────────

@Composable
private fun DetailsAbout(item: Item, modifier: Modifier = Modifier) {
    if (item.plot.isNotBlank()) {
        Text(
            item.plot,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

// ─────────────────────────────── Актёры и режиссёр ────────────────────────────

/**
 * Состав тайтла карточками с фото. Фото приходят из TMDB ([DetailsState.cast]); пока их нет —
 * показываем те же карточки, но с инициалами вместо фото (имена всегда есть от kino.pub). Любая
 * карточка кликабельна и ведёт в фильмографию человека.
 */
@Composable
private fun CastSection(
    people: List<CastMember>,
    director: String,
    onOpenPerson: (name: String, isDirector: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (people.isEmpty() && director.isBlank()) return
    Column(modifier) {
        if (people.isNotEmpty()) {
            Text(
                "Актёры",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = FilmaxMetrics.DetailsPadding, vertical = 0.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = FilmaxMetrics.DetailsPadding),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                items(people) { member ->
                    ActorCard(member = member, onClick = { onOpenPerson(member.name, false) })
                }
            }
        }
        if (director.isNotBlank()) {
            DirectorLine(
                director = director,
                // По запятой — только первый режиссёр: kino.pub ищет по одному имени в `director`.
                onClick = { onOpenPerson(director.substringBefore(",").trim(), true) },
                modifier = Modifier.padding(
                    start = FilmaxMetrics.DetailsPadding,
                    end = FilmaxMetrics.DetailsPadding,
                    top = if (people.isNotEmpty()) 18.dp else 0.dp,
                ),
            )
        }
    }
}

@Composable
private fun ActorCard(member: CastMember, onClick: () -> Unit) {
    Column(
        Modifier.width(ActorCardWidth).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ActorAvatar(member)
        Text(
            member.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        member.character?.takeIf { it.isNotBlank() }?.let { character ->
            Text(
                character,
                style = MaterialTheme.typography.bodySmall,
                color = FilmaxOnSurfaceDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ActorAvatar(member: CastMember) {
    Box(
        Modifier
            .size(ActorAvatarSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val photo = member.photoUrl
        if (photo != null) {
            PosterImage(
                url = photo,
                contentDescription = member.name,
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                accentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        } else {
            Text(
                initials(member.name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DirectorLine(director: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Режиссёр",
            style = MaterialTheme.typography.bodyMedium,
            color = FilmaxOnSurfaceDim,
        )
        Text(
            director,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

// ────────────────────────────── Эпизоды сериала ──────────────────────────────

/** Данные и действия секции эпизодов — группой (detekt LongParameterList). */
private data class EpisodesSectionData(
    val seasons: List<Pair<Int, List<MediaTrack>>>,
    val episodes: List<MediaTrack>,
    val selectedSeason: Int,
    val onSelectSeason: (Int) -> Unit,
    val onPlayEpisode: (season: Int, videoId: Int) -> Unit,
)

@Composable
private fun EpisodesSection(data: EpisodesSectionData, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = FilmaxMetrics.DetailsPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                EPISODES_TITLE,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Селектор — только когда сезонов больше одного: меню из единственного пункта ничего
            // не выбирает, а место и внимание забирает.
            if (data.seasons.size > 1) {
                SeasonSelector(
                    seasons = data.seasons,
                    selected = data.selectedSeason,
                    onSelect = data.onSelectSeason,
                )
            }
        }
        Column(
            Modifier.padding(top = 16.dp, start = FilmaxMetrics.DetailsPadding, end = FilmaxMetrics.DetailsPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            data.episodes.forEach { episode ->
                EpisodeRow(
                    episode = episode,
                    // Плееру нужен НОМЕР серии (API `video`), а не id трека.
                    onClick = { data.onPlayEpisode(episode.seasonNumber, episode.number) },
                )
            }
        }
    }
}

@Composable
private fun SeasonSelector(
    seasons: List<Pair<Int, List<MediaTrack>>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .height(34.dp)
                .clip(ShapeFull)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { expanded = true }
                .padding(start = 14.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                seasonLabel(seasons.getOrNull(selected)?.first ?: 0),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            seasons.forEachIndexed { index, season ->
                DropdownMenuItem(
                    text = {
                        Text(
                            seasonLabel(season.first),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (index == selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: MediaTrack, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        EpisodeThumb(episode)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    episodeNumberLabel(episode),
                    style = MaterialTheme.typography.bodySmall,
                    color = FilmaxOnSurfaceDim,
                )
                episodeBadge(episode)?.let { badge ->
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                episode.title.ifBlank { "Серия ${episode.number}" },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            episodeDuration(episode)?.let { duration ->
                Text(
                    duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun EpisodeThumb(episode: MediaTrack) {
    Box(
        Modifier
            .size(width = FilmaxMetrics.EpisodeThumbWidth, height = FilmaxMetrics.EpisodeThumbHeight)
            .clip(ShapePoster)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        // У kino.pub thumbnail часто пустой, и пустая плитка не отличима от соседней — тогда
        // показываем крупный номер серии.
        if (episode.thumbnail.isNotBlank()) {
            PosterImage(
                url = episode.thumbnail,
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
                shape = ShapePoster,
                accentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        } else {
            Text(
                "${episode.number}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilmaxProgressBar(
            progress = episodeProgress(episode),
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

// ───────────────────────────────── Похожее ───────────────────────────────────

@Composable
private fun SimilarSection(similar: List<Item>, onOpenItem: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            "Похожее",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = FilmaxMetrics.DetailsPadding,
                end = FilmaxMetrics.DetailsPadding,
                bottom = 12.dp,
            ),
        )
        // Ряд полноширинный, поля — через contentPadding: внутри общих боковых полей последняя
        // карточка обрезалась бы по правому краю, и ряд читался бы как сломанный.
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmaxMetrics.DetailsPadding),
            horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        ) {
            items(similar, key = { it.id }) { similarItem ->
                FilmaxPosterCard(
                    title = similarItem.title,
                    posterUrl = similarItem.posters.medium.ifBlank { similarItem.posters.big },
                    onClick = { onOpenItem(similarItem.id) },
                    width = FilmaxMetrics.SimilarPosterWidth,
                    height = FilmaxMetrics.SimilarPosterHeight,
                )
            }
        }
    }
}

// ─────────────────────────── Производные данные ──────────────────────────────
// Чистые производные сериала и подписи меты общие с TV — см. details.common.DetailsFormat.

/** Мета-строка макета: «Фильм · 2024 · 2 ч 46 мин · США» (у сериала вместо длительности — объём). */
private fun metaParts(item: Item, series: SeriesData?): List<String> = buildList {
    add(typeLabel(item.type))
    if (item.year > 0) add(item.year.toString())
    volumeLabel(item, series)?.let { add(it) }
    if (item.country.isNotBlank()) add(item.country)
}

/** «Продолжить · S1 E3» — сериал с недосмотренной серией; иначе «Смотреть». */
private fun playLabel(resume: MediaTrack?): String = when {
    resume == null -> "Смотреть"
    resume.seasonNumber > 0 -> "Продолжить · S${resume.seasonNumber} E${resume.number}"
    else -> "Продолжить · Серия ${resume.number}"
}

/** «Сезон 1»; у тайтла без сезонов (kino.pub отдаёт 0) сезона нет — есть просто серии. */
private fun seasonLabel(number: Int): String = if (number > 0) "Сезон $number" else "Серии"

/** «S1 · E3» — как в макете; без сезона остаётся только номер серии. */
private fun episodeNumberLabel(episode: MediaTrack): String =
    if (episode.seasonNumber > 0) "S${episode.seasonNumber} · E${episode.number}" else "E${episode.number}"

/** Бейдж по `watching.status`: досмотрен, в процессе или (не начат) — ничего. */
private fun episodeBadge(episode: MediaTrack): String? = when (episode.watchStatus) {
    WATCH_STATUS_FINISHED -> "просмотрено"
    WATCH_STATUS_IN_PROGRESS -> "смотрите"
    else -> null
}

private fun episodeDuration(episode: MediaTrack): String? =
    episode.durationSeconds.takeIf { it > 0 }?.let { "${it / SECONDS_IN_MINUTE} мин" }

private fun episodeProgress(episode: MediaTrack): Float =
    if (episode.durationSeconds > 0) episode.watchedSeconds.toFloat() / episode.durationSeconds else 0f

private fun shareItem(context: Context, item: Item) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${item.title} (${item.year}) — смотри в Filmax")
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Поделиться"))
    }
}
