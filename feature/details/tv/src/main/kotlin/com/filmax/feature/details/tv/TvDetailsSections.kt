// Секции под hero: описание, актёры с режиссёром, сезоны с эпизодами и «Похожее». Каждая —
// item(-ы) общего полотна: порядок задаёт detailsSections в TvDetailsScreen.
package com.filmax.feature.details.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.person.CastMember
import com.filmax.core.tv.designsystem.TvCardSize
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvExpandableText
import com.filmax.core.tv.designsystem.TvExpandableTextInset
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPersonCard
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvRail
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.details.common.episodeMeta
import com.filmax.feature.details.common.episodeProgress
import com.filmax.feature.details.common.episodeTitle
import com.filmax.feature.details.common.initials
import com.filmax.feature.details.common.seasonLabel
import com.filmax.feature.details.common.startEpisodeIndex

// ─────────────────────────── Описание и состав ──────────────────────────────

/**
 * Описание свёрнуто до четырёх строк, «ОК» разворачивает его целиком. Поле под рамку фокуса
 * вычтено из отступов: сам текст стоит на одной линии с hero и рядами.
 */
@Composable
internal fun DetailsAbout(item: Item) {
    if (item.plot.isNotBlank()) {
        TvExpandableText(
            text = item.plot,
            modifier = Modifier
                .padding(
                    start = TvMetrics.SafeHorizontal - TvExpandableTextInset,
                    end = TvMetrics.SafeHorizontal - TvExpandableTextInset,
                    top = AboutTop - TvExpandableTextInset,
                )
                .widthIn(max = ReadableTextWidth + TvExpandableTextInset * 2),
        )
    }
}

/** Отступ описания от hero — до текста, а не до рамки фокуса. */
private val AboutTop = 22.dp

// ─────────────────────────────── Актёры и режиссёр ────────────────────────────

/**
 * Ряд актёров карточками с круглым аватаром. Фото приходят из TMDB ([DetailsState.cast]); пока
 * их нет — те же карточки с инициалами (имена всегда есть от kino.pub). Каждая карточка ведёт в
 * фильмографию человека, поэтому каст на TV наконец фокусируемый и кликабельный, а не мёртвая строка.
 */
internal fun LazyListScope.castRail(people: List<CastMember>, onOpenPerson: (String, Boolean) -> Unit) {
    item(key = "cast") {
        TvRail(title = "Актёры", modifier = Modifier.padding(top = 24.dp)) {
            // Без key: имена в составе могут повторяться, позиционного ключа достаточно.
            items(people) { member ->
                TvPersonCard(
                    name = member.name,
                    photoUrl = member.photoUrl,
                    initials = initials(member.name),
                    onClick = { onOpenPerson(member.name, false) },
                )
            }
        }
    }
}

/** Режиссёр отдельным фокусируемым чипом под рядом актёров — ведёт в его фильмографию. */
internal fun LazyListScope.directorSection(director: String, onOpenPerson: (String, Boolean) -> Unit) {
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

// ────────────────────────────── Эпизоды сериала ──────────────────────────────

/** Данные и действия секции эпизодов — группой (detekt LongParameterList). */
internal data class EpisodesSection(
    val seasons: List<Pair<Int, List<MediaTrack>>>,
    val episodes: List<MediaTrack>,
    val resumeId: Int?,
    val selectedSeason: Int,
    val onSelectSeason: (Int) -> Unit,
    val onPlayEpisode: (season: Int, videoId: Int) -> Unit,
    val focus: TvScreenFocus,
)

/**
 * Секция эпизодов: заголовок → чипы сезонов → ряд серий выбранного сезона.
 *
 * Чипы и карточки — разные ряды полотна, поэтому «вниз» с чипов ведёт в серии, а не прыгает
 * через них. У одного сезона чипов нет: выбирать нечего, а шаг пульта они бы забрали.
 */
internal fun LazyListScope.episodesSection(section: EpisodesSection) {
    item(key = "episodes-title") {
        SectionTitle(EPISODES_TITLE, Modifier.padding(top = 24.dp))
    }
    if (section.seasons.size > 1) {
        item(key = "seasons") {
            SeasonChips(
                seasons = section.seasons,
                selected = section.selectedSeason,
                onSelect = section.onSelectSeason,
            )
        }
    }
    item(key = "episodes") {
        EpisodesRow(
            episodes = section.episodes,
            resumeId = section.resumeId,
            selectedSeason = section.selectedSeason,
            onPlay = section.onPlayEpisode,
            focus = section.focus,
        )
    }
}

/** Заголовок секции (TvRail рисует свой вплотную к ряду, а здесь между ними чипы сезонов). */
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
 * Чипы сезонов. Сезон переключается фокусом, без «ОК»: ряд серий меняется, пока пульт идёт по
 * сезонам, — как у Apple TV и Netflix. Раньше сезон выбирался только нажатием, и, пройдя по
 * чипам фокусом, зритель спускался в серии прошлого сезона.
 *
 * Обычный Row со скроллом, а не LazyRow: сезонов единицы, все чипы скомпонованы, и запасная
 * цель фокуса — выбранный сезон — всегда привязана к узлу. Первый заход в ряд встаёт на неё,
 * а не на первый чип: фокус переключает сезон, и вход на «Сезон 1» сбрасывал бы выбор.
 */
@Composable
private fun SeasonChips(
    seasons: List<Pair<Int, List<MediaTrack>>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val selectedChip = remember { FocusRequester() }
    Row(
        Modifier
            .fillMaxWidth()
            .focusRestorer(selectedChip)
            .focusGroup()
            .horizontalScroll(rememberScrollState())
            // Поля — внутри скролла: чип при фокусе растёт, и рамке нужно место у краёв.
            .padding(horizontal = TvMetrics.SafeHorizontal, vertical = TvMetrics.FocusInset),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
    ) {
        seasons.forEachIndexed { index, (number, _) ->
            TvChip(
                label = seasonLabel(number),
                selected = index == selected,
                onClick = { onSelect(index) },
                modifier = Modifier
                    .then(if (index == selected) Modifier.focusRequester(selectedChip) else Modifier)
                    .onFocusChanged { state -> if (state.isFocused) onSelect(index) },
            )
        }
    }
}

/**
 * Ряд серий. Свой LazyRow, а не [TvRail]: заголовок «Эпизоды» стоит над чипами сезонов, а
 * TvRail жёстко ставит заголовок над своим рядом. Отступы и группа фокуса — как у TvRail.
 *
 * Ряд ПЕРЕСОЗДАЁТСЯ на каждый сезон (`key`), а не переиспользует один LazyListState. Соседний
 * сезон — это другой набор данных: другие ключи и другая длина. Общий стейт тащил в него скролл
 * прошлого сезона и — главное — удержанный (pinned) фокусом элемент: при следующем размещении
 * ряд ставил его вторым проходом и Compose падал с «Place was called on a node which was placed
 * already». Ловилось так: посмотреть серию → вернуться на карточку → полистать сезоны и серии
 * (Crashlytics 1.7.1, реальный ТВ-бокс). Свежий стейт не тащит ни скролла, ни пинов.
 *
 * Ряд открывается на серии «продолжить» (иначе — на первой недосмотренной), и первый заход
 * фокуса встаёт на неё же: D-pad сажает фокус на ближайшую под чипом карточку, а на 17-й серии
 * сезона это ещё 16 нажатий «вправо» от первой.
 */
@Composable
private fun EpisodesRow(
    episodes: List<MediaTrack>,
    resumeId: Int?,
    selectedSeason: Int,
    onPlay: (season: Int, videoId: Int) -> Unit,
    focus: TvScreenFocus,
) {
    key(selectedSeason) {
        val startIndex = remember(episodes, resumeId) { startEpisodeIndex(episodes, resumeId) }
        val startCard = remember { FocusRequester() }
        // Сохраняемый: возврат из плеера ставит фокус на сыгранную серию (TvScreenFocus), и
        // перехватывать его нельзя — заход в ряд тогда уже случился.
        var entered by rememberSaveable { mutableStateOf(false) }
        var moveToStart by remember { mutableStateOf(false) }
        // Через эффект, а не прямо в onFocusChanged: менять фокус внутри его же перехода нельзя.
        LaunchedEffect(moveToStart) {
            if (moveToStart) startCard.requestFocus()
        }
        LazyRow(
            state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex),
            modifier = Modifier
                .tvFocusGroup()
                .onFocusChanged { state ->
                    if (state.hasFocus && !entered) {
                        entered = true
                        moveToStart = true
                    }
                },
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
                    // Возврат из плеера ставит фокус обратно на эту серию.
                    modifier = focus.item("episode:${episode.id}")
                        .then(if (index == startIndex) Modifier.focusRequester(startCard) else Modifier),
                    // Плееру нужны номер серии (API `video`) и сезон, а не id трека.
                    onClick = { onPlay(episode.seasonNumber, episode.number) },
                )
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: MediaTrack,
    isResume: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    TvProgressCard(
        title = episodeTitle(episode),
        meta = episodeMeta(episode),
        posterUrl = episode.thumbnail,
        progress = episodeProgress(episode),
        onClick = onClick,
        modifier = modifier,
        size = TvCardSize.Episode,
        // Явная плашка вместо слова «продолжить» в мете: в ряду из десятка одинаковых плиток
        // текстовый признак не находится взглядом.
        badge = if (isResume) RESUME_BADGE else null,
        fallbackLabel = "${episode.number}",
    )
}

private const val RESUME_BADGE = "Продолжить"

// ─────────────────────────────────── Похожее ─────────────────────────────────

internal fun LazyListScope.similarRail(similar: List<Item>, onOpenItem: (Int) -> Unit) {
    item(key = "similar") {
        TvRail(title = "Похожее", modifier = Modifier.padding(top = 26.dp)) {
            items(similar, key = { simItem -> simItem.id }) { simItem ->
                TvPosterCard(
                    title = simItem.title,
                    meta = posterMeta(typeLabel(simItem.type), simItem.year),
                    posterUrl = simItem.posters.medium.ifEmpty { simItem.posters.big },
                    onClick = { onOpenItem(simItem.id) },
                    rating = ratingLabel(simItem.rating.kinopoisk),
                )
            }
        }
    }
}
