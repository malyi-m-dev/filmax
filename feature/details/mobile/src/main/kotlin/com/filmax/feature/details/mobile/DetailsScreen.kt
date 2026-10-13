// Экран деталей — один вертикальный поток и для фильма, и для сериала: кадр, заголовок с метой и
// рейтингами, кнопки, описание, эпизоды (сериал) и «Похожее». Здесь — состояние экрана и порядок
// секций; кадр, заголовок и кнопки живут в DetailsHero, описание, актёры и «Похожее» — в
// DetailsSections, сезоны и серии — в DetailsEpisodes.

package com.filmax.feature.details.mobile

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.person.CastMember
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import com.filmax.feature.details.common.SeriesData
import com.filmax.feature.details.common.calculateSeriesData
import com.filmax.feature.details.common.isSeries
import com.filmax.feature.details.common.resolveCast
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Фильм играется целиком, без выбора дорожки: плеер ждёт videoId = -1. */
private const val MOVIE_VIDEO_ID = -1

/** «Сезона нет» — фильм или сезон неизвестен (PlayerRoute.season = -1). */
private const val NO_SEASON = -1

/**
 * Мобильные Детали поверх общего [DetailsScreenModel] (itemId берётся из маршрута через
 * SavedStateHandle).
 */
@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
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
                    onBack = navigator::back,
                    // Второй аргумент — НОМЕР серии, а не её id: тем же числом kino.pub принимает
                    // и отдаёт прогресс. Фильм играется целиком ([MOVIE_VIDEO_ID]).
                    onPlay = { season, videoId ->
                        navigator.open(
                            Destination.Player(itemId = item.id, videoId = videoId, season = season),
                        )
                    },
                    onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                    onShare = { shareItem(context, item) },
                    onOpenItem = { itemId -> navigator.open(Destination.Details(itemId)) },
                    onOpenPerson = { name, isDirector ->
                        navigator.open(Destination.Filmography(name = name, isDirector = isDirector))
                    },
                    onPlayTrailer = { url, title ->
                        navigator.open(Destination.Trailer(url = url, title = title))
                    },
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

private fun shareItem(context: Context, item: Item) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${item.title} (${item.year}) — смотри в Filmax")
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Поделиться"))
    }
}
