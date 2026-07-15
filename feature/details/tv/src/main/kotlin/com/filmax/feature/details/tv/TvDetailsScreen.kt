// Экран деталей составной: hero с действиями, сезоны с эпизодами и ряд похожих. Каждая часть —
// свой composable, и это правильное дробление; растаскивать их по файлам значило бы разорвать
// один экран на куски, которые читаются только вместе.
@file:Suppress("TooManyFunctions")

package com.filmax.feature.details.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvCardSize
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvFocus
import com.filmax.core.tv.designsystem.TvMetaRow
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
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
import com.filmax.core.ui.components.HeroBackdrop
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import org.koin.androidx.compose.koinViewModel

/** Ширина текстового блока в hero (макет: 600dp из 960) — правее лежит открытый бэкдроп. */
private val HeroTextWidth = 600.dp

/** Максимальная ширина описания и строки состава: длинная строка на 3 метрах не читается. */
private val ReadableTextWidth = 760.dp

private const val EPISODES_TITLE = "Эпизоды"

/** Фильм играется целиком, без выбора дорожки: плеер ждёт videoId = -1. */
private const val MOVIE_VIDEO_ID = -1

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
    onPlay: (itemId: Int, videoId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
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
                isFav = state.isFav,
                actions = DetailsActions(
                    onPlay = { videoId -> onPlay(item.id, videoId) },
                    onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                    onOpenItem = onOpenItem,
                ),
            )
        }
    }
}

/** Действия экрана — группой, чтобы не раздувать списки параметров у вложенных секций. */
private data class DetailsActions(
    val onPlay: (videoId: Int) -> Unit,
    val onToggleFav: () -> Unit,
    val onOpenItem: (Int) -> Unit,
)

@Composable
private fun DetailsContent(
    item: Item,
    similar: List<Item>,
    isFav: Boolean,
    actions: DetailsActions,
) {
    val series = remember(item) { if (item.isSeries()) calculateSeriesData(item.tracklist) else null }
    // Селектор стартует на сезоне недосмотренной серии, а не на первом: продолжают чаще, чем
    // начинают заново.
    var selectedSeason by remember(item.id) { mutableIntStateOf(series?.resumeSeasonIndex ?: 0) }
    val episodes = series?.seasons?.getOrNull(selectedSeason)?.second.orEmpty()

    val playFocus = remember { FocusRequester() }
    // Пульту нужен стартовый фокус, иначе первое нажатие уходит в никуда. Ключ item.id —
    // при переходе на соседний тайтл фокус возвращается на «Смотреть». runCatching: на первом
    // кадре FocusRequester ещё может быть не привязан к узлу.
    LaunchedEffect(item.id) { runCatching { playFocus.requestFocus() } }

    // Кнопка играет недосмотренную серию, иначе первую серию ВЫБРАННОГО сезона (у фильма дорожка
    // не выбирается вовсе).
    val target = series?.let { it.resume ?: episodes.firstOrNull() ?: item.tracklist.firstOrNull() }

    // Единый LazyColumn вместо статичного Row из двух колонок: раньше у сериала нижние кнопки и
    // «Похожее» были недостижимы фокусом — упирались в край экрана без возможности докрутить.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 70.dp),
    ) {
        item(key = "hero") {
            DetailsHero(
                item = item,
                series = series,
                isFav = isFav,
                playback = HeroPlayback(
                    playFocus = playFocus,
                    // Фильм играется целиком (videoId = -1), сериал — конкретной серией.
                    // Сериал без серий играть нечем — кнопка молчит, а не открывает пустой плеер.
                    // В плеер уходит НОМЕР серии, а не id трека: тем же числом kino.pub
                    // принимает и отдаёт прогресс (watching/marktime → video).
                    onPlay = {
                        if (series == null) {
                            actions.onPlay(MOVIE_VIDEO_ID)
                        } else {
                            target?.let { actions.onPlay(it.number) }
                        }
                    },
                    onToggleFav = actions.onToggleFav,
                ),
            )
        }

        item(key = "about") { DetailsAbout(item) }

        if (episodes.isNotEmpty()) {
            episodesSection(
                EpisodesSection(
                    seasons = series?.seasons.orEmpty(),
                    episodes = episodes,
                    resumeId = series?.resume?.id,
                    selectedSeason = selectedSeason,
                    onSelectSeason = { selectedSeason = it },
                    onPlayEpisode = actions.onPlay,
                )
            )
        }

        // «Похожее» — и у фильма, и у сериала: state.similar грузится всегда, а сериал его
        // молча выбрасывал.
        if (similar.isNotEmpty()) {
            similarRail(similar = similar, onOpenItem = actions.onOpenItem)
        }
    }
}

// ─────────────────────────────────── Hero ───────────────────────────────────

/** Фокус и действия кнопок hero — группой (detekt LongParameterList). */
private data class HeroPlayback(
    val playFocus: FocusRequester,
    val onPlay: () -> Unit,
    val onToggleFav: () -> Unit,
)

/**
 * Hero: бэкдроп во всю ширину, текстовый блок прижат к низу слева (вариант A макета).
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
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
    Column(
        Modifier
            .padding(start = TvMetrics.SafeHorizontal, end = TvMetrics.SafeHorizontal, top = 22.dp)
            .widthIn(max = ReadableTextWidth),
    ) {
        if (item.plot.isNotBlank()) {
            Text(
                item.plot,
                style = MaterialTheme.typography.bodyLarge,
                color = TvOnSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CastLine(item = item, modifier = Modifier.padding(top = 14.dp))
    }
}

/**
 * Состав и режиссёр — строкой под описанием (макет `det.castLine`), а не отдельной стеклянной
 * панелью: форм в системе две, и асимметричной «печеньки» среди них нет.
 *
 * Строка кликабельна, ТОЛЬКО если текст реально не поместился: overflow меряем рантаймом через
 * [TextOverflow]/`hasVisualOverflow`, а не гадаем по длине. Иначе на коротком составе появляется
 * фокус-ловушка, которая ничего не открывает.
 */
@Composable
private fun CastLine(item: Item, modifier: Modifier = Modifier) {
    val text = remember(item) { castLine(item) }
    if (text.isBlank()) return

    var overflow by remember(item.id) { mutableStateOf(false) }
    var expanded by remember(item.id) { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    // Держим источник взаимодействия снаружи ветки: внутри `if (overflow)` remember вызывался бы
    // условно и пересоздавался при каждом переключении overflow.
    val interaction = remember { MutableInteractionSource() }

    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = TvOnSurfaceDim,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { overflow = it.hasVisualOverflow },
        modifier = modifier
            // clickable сам по себе фокусируемый и реагирует на DPAD_CENTER — отдельный focusable
            // не нужен (он перехватил бы фокус, и центр не доходил бы до клика).
            .then(
                if (overflow) {
                    Modifier
                        .onFocusChanged { focused = it.isFocused }
                        .clickable(interactionSource = interaction, indication = null) { expanded = true }
                } else {
                    Modifier
                }
            )
            .then(
                if (focused) {
                    Modifier.border(TvMetrics.FocusBorderWidth, TvFocus, TvMetrics.PanelShape)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 4.dp),
    )

    if (expanded) {
        DetailsCastDialog(item = item, onDismiss = { expanded = false })
    }
}

/** Оверлей с полным составом и режиссёром; «Назад» закрывает (onDismissRequest). */
@Composable
private fun DetailsCastDialog(item: Item, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .widthIn(max = 720.dp)
                .heightIn(max = 440.dp)
                .clip(TvMetrics.PanelShape)
                .background(TvSurfaceContainer)
                .verticalScroll(rememberScrollState())
                .focusable()
                .padding(36.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.titleLarge, color = TvOnSurface)
            if (item.director.isNotBlank()) {
                CastDialogSection(title = "Режиссёр", body = item.director)
            }
            if (item.cast.isNotBlank()) {
                CastDialogSection(title = "В ролях", body = item.cast)
            }
        }
    }
}

@Composable
private fun CastDialogSection(title: String, body: String) {
    TvOverline(title, Modifier.padding(top = 20.dp, bottom = 6.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = TvOnSurfaceVariant)
}

// ────────────────────────────── Эпизоды сериала ──────────────────────────────

/** Данные и действия секции эпизодов — группой (detekt LongParameterList). */
private data class EpisodesSection(
    val seasons: List<Pair<Int, List<MediaTrack>>>,
    val episodes: List<MediaTrack>,
    val resumeId: Int?,
    val selectedSeason: Int,
    val onSelectSeason: (Int) -> Unit,
    val onPlayEpisode: (videoId: Int) -> Unit,
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
            TvRail(title = EPISODES_TITLE, modifier = Modifier.padding(top = 24.dp)) {
                itemsIndexed(section.seasons, key = { _, season -> season.first }) { index, season ->
                    val number = season.first
                    TvChip(
                        label = if (number > 0) "Сезон $number" else "Серии",
                        selected = index == section.selectedSeason,
                        onClick = { section.onSelectSeason(index) },
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
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EpisodesRow(
    episodes: List<MediaTrack>,
    resumeId: Int?,
    onPlay: (videoId: Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier.focusRestorer(),
        contentPadding = PaddingValues(
            start = TvMetrics.SafeHorizontal,
            end = TvMetrics.SafeHorizontal,
            top = TvMetrics.FocusInset,
            bottom = TvMetrics.FocusInset,
        ),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
    ) {
        items(episodes, key = { it.id }) { episode ->
            EpisodeCard(
                episode = episode,
                isResume = episode.id == resumeId,
                // Плееру нужен номер серии (API `video`), а не id трека.
                onClick = { onPlay(episode.number) },
            )
        }
    }
}

@Composable
private fun EpisodeCard(episode: MediaTrack, isResume: Boolean, onClick: () -> Unit) {
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
        TvRail(title = "Похожее", modifier = Modifier.padding(top = 26.dp)) {
            items(similar, key = { it.id }) { simItem ->
                TvPosterCard(
                    title = simItem.title,
                    meta = posterMeta(typeLabel(simItem.type), simItem.year),
                    posterUrl = simItem.posters.medium.ifEmpty { simItem.posters.big },
                    onClick = { onOpenItem(simItem.id) },
                    rating = ratingLabel(simItem.rating.kinopoisk),
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
    val resume = tracklist.firstOrNull { it.watchStatus == 0 }
        ?: tracklist.lastOrNull { it.watchStatus == 1 }
    val resumeSeasonIndex = resume
        ?.let { episode -> seasons.indexOfFirst { it.first == episode.seasonNumber }.takeIf { it >= 0 } }
        ?: 0
    return SeriesData(seasons = seasons, resume = resume, resumeSeasonIndex = resumeSeasonIndex)
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

/** Строка под описанием: «Режиссёр: X  ·  В ролях: A, B, C» (макет `det.castLine`). */
private fun castLine(item: Item): String = buildList {
    if (item.director.isNotBlank()) add("Режиссёр: ${item.director}")
    if (item.cast.isNotBlank()) add("В ролях: ${item.cast}")
}.joinToString("  ·  ")

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
