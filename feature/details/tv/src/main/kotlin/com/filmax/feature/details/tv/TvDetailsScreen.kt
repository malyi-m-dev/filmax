// Полотно экрана деталей: состояние, порядок секций и два его скролл-стейта (hero и контент).
// Сами секции живут рядом: hero с действиями — в TvDetailsHero, описание, актёры, эпизоды и
// «Похожее» — в TvDetailsSections.
// BringIntoViewSpec: единственный способ выключить фокус-прокрутку полотна в hero-стейте.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.details.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.person.CastMember
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import com.filmax.feature.details.common.SeriesData
import com.filmax.feature.details.common.calculateSeriesData
import com.filmax.feature.details.common.isSeries
import com.filmax.feature.details.common.resolveCast
import com.filmax.feature.details.common.volumeLabel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** Ширина текстового блока в hero (макет: 600dp из 960) — правее лежит открытый бэкдроп. */
internal val HeroTextWidth = 600.dp

/** Максимальная ширина описания и строки состава: длинная строка на 3 метрах не читается. */
internal val ReadableTextWidth = 760.dp

/** Отступ снизу единого полотна: рамке фокуса последнего ряда нужно место. */
private val ContentBottomPadding = 70.dp

/** Индекс элемента «описание» в полотне: сюда полотно едет, когда фокус уходит с кнопок вниз. */
private const val CONTENT_START_INDEX = 1

/** Сколько кадров пропустить перед прокруткой к стейту (см. [rememberHeroFocusScroller]). */
private const val FRAMES_BEFORE_STATE_SCROLL = 2

/** Ширина карточки актёра и диаметр круглого аватара в ряду «Актёры» (крупнее мобильных — 10-foot UI). */
internal val TvActorCardWidth = 120.dp
internal val TvActorAvatarSize = 104.dp

internal const val EPISODES_TITLE = "Эпизоды"

/** Ключ фокуса кнопки «Смотреть»: стартовая цель экрана. */
private const val HERO_PLAY_KEY = "hero:play"

/** Фильм играется целиком, без выбора дорожки: плеер ждёт videoId = -1. */
private const val MOVIE_VIDEO_ID = -1

/** «Сезона нет» — фильм или сезон неизвестен (PlayerRoute.season = -1). */
private const val NO_SEASON = -1

private const val MAX_META_GENRES = 2
private const val SECONDS_IN_MINUTE = 60

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

    // Первый заход открывает экран на «Смотреть», возврат из плеера — на серии, с которой ушли.
    // И то, и другое — одна цель фокуса, поэтому и механизм один: два конкурирующих реквеста в
    // одном кадре давали то кнопку, то серию, и ряд серий выглядел мёртвым (отсюда «иногда»).
    val focus = rememberTvScreenFocus(startAt = HERO_PLAY_KEY)

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
            modifier = Modifier.fillMaxSize().then(focus.containerModifier),
            contentPadding = PaddingValues(top = TvMetrics.SafeVertical, bottom = ContentBottomPadding),
        ) {
            item(key = "hero") {
                DetailsHero(
                    item = item,
                    series = series,
                    isFav = isFav,
                    playback = HeroPlayback(
                        playModifier = focus.item(HERO_PLAY_KEY),
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
                focus = focus,
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
    focus: TvScreenFocus,
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
                focus = focus,
            )
        )
    }
    if (data.similar.isNotEmpty()) {
        similarRail(similar = data.similar, onOpenItem = actions.onOpenItem)
    }
}

// ───────────────────────────── Производные данные ────────────────────────────
// Чистые производные сериала и подписи меты общие с mobile — см. details.common.DetailsFormat.

/** Мета-строка hero: год · объём/длительность · страна · жанры. Пустые части выпадают. */
internal fun metaParts(item: Item, series: SeriesData?): List<String> = buildList {
    if (item.year > 0) add(item.year.toString())
    volumeLabel(item, series)?.let { add(it) }
    if (item.country.isNotBlank()) add(item.country)
    if (item.genres.isNotEmpty()) {
        add(item.genres.take(MAX_META_GENRES).joinToString(", ") { it.title })
    }
}

/** «Продолжить · S2E5» — сериал с недосмотренной серией; иначе «Смотреть». */
internal fun playLabel(resume: MediaTrack?): String = when {
    resume == null -> "Смотреть"
    resume.seasonNumber > 0 -> "Продолжить · S${resume.seasonNumber}E${resume.number}"
    else -> "Продолжить · Серия ${resume.number}"
}

/** Мета карточки серии: «Серия 3 · 45 мин». Номер опускаем, если он уже стал заголовком. */
internal fun episodeMeta(episode: MediaTrack): String? = buildList {
    if (episode.title.isNotBlank()) add("Серия ${episode.number}")
    episode.durationSeconds.takeIf { it > 0 }?.let { add("${it / SECONDS_IN_MINUTE} мин") }
}.joinToString(" · ").ifBlank { null }
