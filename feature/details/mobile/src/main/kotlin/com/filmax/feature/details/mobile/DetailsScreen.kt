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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxProgressBar
import com.filmax.core.ui.components.HeroBackdrop
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import org.koin.androidx.compose.koinViewModel

/** Фильм играется целиком, без выбора дорожки: плеер ждёт videoId = -1. */
private const val MOVIE_VIDEO_ID = -1

private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_MINUTE = 60

/** Статусы просмотра kino.pub (`watching.status`): -1 не начат, 0 в процессе, 1 досмотрен. */
private const val WATCH_STATUS_IN_PROGRESS = 0
private const val WATCH_STATUS_FINISHED = 1

// Модули русских правил склонения по числу (последние две / одна цифра).
private const val PLURAL_MOD_HUNDRED = 100
private const val PLURAL_MOD_TEN = 10

/**
 * Отступ кнопки «назад» под статус-баром. В макете она в 44dp от верха кадра, но кадр макета
 * нарисован без статус-бара, а приложение живёт edge-to-edge — те же 44dp набираются
 * `statusBarsPadding()` плюс это поле.
 */
private val BackButtonInset = 8.dp

private const val EPISODES_TITLE = "Эпизоды"

/**
 * Сериал определяем по ТИПУ тайтла, а не по числу дорожек: у фильма с двумя озвучками
 * `tracklist.size > 1`, и он получил бы селектор сезонов из одного бессмысленного сезона.
 */
private fun Item.isSeries(): Boolean =
    type == ItemType.SERIES || type == ItemType.ANIME || type == ItemType.DOCUMENTARY

/**
 * Мобильные Детали поверх общего [DetailsScreenModel] (itemId берётся из маршрута через
 * SavedStateHandle).
 *
 * [onPlay] вторым аргументом принимает НОМЕР серии, а не её id: тем же числом kino.pub и
 * принимает, и отдаёт прогресс (`watching/marktime?video=`). Фильм играется целиком —
 * [MOVIE_VIDEO_ID].
 */
@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
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
                similar = state.similar,
                isFav = state.isFav,
                actions = DetailsActions(
                    onBack = onBack,
                    onPlay = { videoId -> onPlay(item.id, videoId) },
                    onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                    onShare = { shareItem(context, item) },
                    onOpenItem = onOpenItem,
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
    val onPlay: (videoId: Int) -> Unit,
    val onToggleFav: () -> Unit,
    val onShare: () -> Unit,
    val onOpenItem: (Int) -> Unit,
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
private fun DetailsContent(item: Item, similar: List<Item>, isFav: Boolean, actions: DetailsActions) {
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
            DetailsBody(item = item, similar = similar, isFav = isFav, series = seriesUi, actions = actions)
        }
    }
}

@Composable
private fun DetailsBody(
    item: Item,
    similar: List<Item>,
    isFav: Boolean,
    series: SeriesUiState,
    actions: DetailsActions,
) {
    val sidePadding = Modifier.padding(horizontal = FilmaxMetrics.DetailsPadding)
    // Кнопка играет недосмотренную серию, иначе первую серию ВЫБРАННОГО сезона.
    val target = series.data?.let { it.resume ?: series.episodes.firstOrNull() ?: item.tracklist.firstOrNull() }

    DetailsHeader(item = item, series = series.data, modifier = sidePadding)
    DetailsButtons(
        isFav = isFav,
        resume = series.data?.resume,
        actions = DetailsButtonActions(
            onPlay = {
                // Сериал без серий играть нечем — кнопка молчит, а не открывает пустой плеер.
                if (series.data == null) {
                    actions.onPlay(MOVIE_VIDEO_ID)
                } else {
                    target?.let { actions.onPlay(it.number) }
                }
            },
            onToggleFav = actions.onToggleFav,
            onShare = actions.onShare,
        ),
        modifier = sidePadding.padding(top = 18.dp),
    )
    DetailsAbout(item = item, modifier = sidePadding.padding(top = 18.dp))
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
    if (similar.isNotEmpty()) {
        SimilarSection(
            similar = similar,
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
            ShareButton(onClick = actions.onShare)
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

@Composable
private fun ShareButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(FilmaxMetrics.SecondaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Share,
            contentDescription = "Поделиться",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ────────────────────────── Описание и строка состава ─────────────────────────

@Composable
private fun DetailsAbout(item: Item, modifier: Modifier = Modifier) {
    val cast = remember(item) { castLine(item) }
    Column(modifier) {
        if (item.plot.isNotBlank()) {
            Text(
                item.plot,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (cast.isNotBlank()) {
            Text(
                cast,
                style = MaterialTheme.typography.bodyMedium,
                color = FilmaxOnSurfaceDim,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

// ────────────────────────────── Эпизоды сериала ──────────────────────────────

/** Данные и действия секции эпизодов — группой (detekt LongParameterList). */
private data class EpisodesSectionData(
    val seasons: List<Pair<Int, List<MediaTrack>>>,
    val episodes: List<MediaTrack>,
    val selectedSeason: Int,
    val onSelectSeason: (Int) -> Unit,
    val onPlayEpisode: (videoId: Int) -> Unit,
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
                    onClick = { data.onPlayEpisode(episode.number) },
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

/**
 * Производные сериала для экрана: сезоны (сгруппированы и отсортированы) и точка «продолжить».
 * Отдельная чистая модель вместо переплетённых remember-блоков в Composable.
 */
private data class SeriesData(
    /** Пары «номер сезона → серии по порядку», отсортированные по номеру сезона. */
    val seasons: List<Pair<Int, List<MediaTrack>>>,
    /** Эпизод для «продолжить»: в процессе → последний досмотренный → иначе null. */
    val resume: MediaTrack?,
    /** Индекс сезона эпизода «продолжить» в [seasons] (0, если не определён). */
    val resumeSeasonIndex: Int,
)

/** Считает [SeriesData] из плейлиста серий — чистая функция, тестируемая отдельно от UI. */
private fun calculateSeriesData(tracklist: List<MediaTrack>): SeriesData {
    val seasons = tracklist
        .groupBy { it.seasonNumber }
        .toSortedMap()
        .map { (number, episodes) -> number to episodes.sortedBy { it.number } }
    val resume = tracklist.firstOrNull { it.watchStatus == WATCH_STATUS_IN_PROGRESS }
        ?: tracklist.lastOrNull { it.watchStatus == WATCH_STATUS_FINISHED }
    val resumeSeasonIndex = resume
        ?.let { episode -> seasons.indexOfFirst { it.first == episode.seasonNumber }.takeIf { it >= 0 } }
        ?: 0
    return SeriesData(seasons = seasons, resume = resume, resumeSeasonIndex = resumeSeasonIndex)
}

/** Мета-строка макета: «Фильм · 2024 · 2 ч 46 мин · США» (у сериала вместо длительности — объём). */
private fun metaParts(item: Item, series: SeriesData?): List<String> = buildList {
    add(typeLabel(item.type))
    if (item.year > 0) add(item.year.toString())
    volumeLabel(item, series)?.let { add(it) }
    if (item.country.isNotBlank()) add(item.country)
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

/** «Продолжить · S1 E3» — сериал с недосмотренной серией; иначе «Смотреть». */
private fun playLabel(resume: MediaTrack?): String = when {
    resume == null -> "Смотреть"
    resume.seasonNumber > 0 -> "Продолжить · S${resume.seasonNumber} E${resume.number}"
    else -> "Продолжить · Серия ${resume.number}"
}

/**
 * Состав и режиссёр — одной строкой под описанием. Режиссёр жил на цветной стат-карточке; карточки
 * ушли вместе с «экспрессивной» палитрой, и эта строка — единственное оставшееся для него место
 * (так же сделано на TV, чтобы экраны не расходились).
 */
private fun castLine(item: Item): String = buildList {
    if (item.cast.isNotBlank()) add("В ролях: ${item.cast}")
    if (item.director.isNotBlank()) add("Режиссёр: ${item.director}")
}.joinToString("  ·  ")

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

private fun typeLabel(type: ItemType): String = when (type) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY -> "Док. сериал"
    ItemType.TV -> "ТВ"
}

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

private fun shareItem(context: Context, item: Item) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${item.title} (${item.year}) — смотри в Filmax")
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Поделиться"))
    }
}
