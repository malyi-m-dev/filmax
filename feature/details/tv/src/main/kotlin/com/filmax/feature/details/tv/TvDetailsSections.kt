// Секции под hero: описание, актёры с режиссёром, сезоны с эпизодами и «Похожее». Каждая —
// item(-ы) общего полотна: порядок задаёт detailsSections в TvDetailsScreen.
package com.filmax.feature.details.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.person.CastMember
import com.filmax.core.tv.designsystem.TvCardSize
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvRail
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.rememberDimAlpha
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.details.common.initials

// ─────────────────────────── Описание и состав ──────────────────────────────

@Composable
internal fun DetailsAbout(item: Item) {
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
internal fun LazyListScope.castRail(people: List<CastMember>, onOpenPerson: (String, Boolean) -> Unit) {
    item(key = "cast") {
        TvRail(title = "Актёры", modifier = Modifier.padding(top = 24.dp)) {
            // Без key: имена в составе могут повторяться, позиционного ключа достаточно.
            items(people) { member ->
                TvActorCard(
                    member = member,
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

/** Карточка актёра: круглый аватар (фото TMDB или инициалы) + имя. Фокус/скейл — как у медиа-карточек. */
@Composable
private fun TvActorCard(member: CastMember, onClick: () -> Unit) {
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
 * Секция эпизодов: заголовок → чипы сезонов → ряд карточек серий.
 *
 * Чипы — горизонтальный ряд, а не FlowRow с переносом: у сериала на 8+ сезонов перенос забирал
 * под чипы половину экрана. Чипы и карточки — разные ряды LazyColumn, поэтому «вниз» с чипов
 * ведёт в серии, а не прыгает через них.
 */
internal fun LazyListScope.episodesSection(section: EpisodesSection) {
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
            selectedSeason = section.selectedSeason,
            onPlay = section.onPlayEpisode,
            focus = section.focus,
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
 * TvRail жёстко ставит заголовок над своим рядом. Отступы и группа фокуса — как у TvRail.
 *
 * Ряд ПЕРЕСОЗДАЁТСЯ на каждый сезон (`key`), а не переиспользует один LazyListState. Соседний
 * сезон — это другой набор данных: другие ключи и другая длина. Общий стейт тащил в него скролл
 * прошлого сезона и — главное — удержанный (pinned) фокусом элемент: при следующем размещении
 * ряд ставил его вторым проходом и Compose падал с «Place was called on a node which was placed
 * already». Ловилось так: посмотреть серию → вернуться на карточку → полистать сезоны и серии
 * (Crashlytics 1.7.1, реальный ТВ-бокс). Свежий стейт не тащит ни скролла, ни пинов, и сброс
 * скролла к началу больше не нужен отдельным эффектом.
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
        LazyRow(
            state = rememberLazyListState(),
            modifier = Modifier.tvFocusGroup(),
            contentPadding = PaddingValues(
                start = TvMetrics.SafeHorizontal,
                end = TvMetrics.SafeHorizontal,
                top = TvMetrics.FocusInset,
                bottom = TvMetrics.FocusInset,
            ),
            horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
        ) {
            items(episodes, key = { episode -> episode.id }) { episode ->
                EpisodeCard(
                    episode = episode,
                    isResume = episode.id == resumeId,
                    // Возврат из плеера ставит фокус обратно на эту серию.
                    modifier = focus.item("episode:${episode.id}"),
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
        modifier = modifier,
        size = TvCardSize.Episode,
    ) { url, posterModifier ->
        EpisodeThumb(url = url, episode = episode, isResume = isResume, modifier = posterModifier)
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
