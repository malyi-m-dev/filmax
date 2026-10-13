// Секции экрана деталей под кнопками: описание, актёры с режиссёром и «Похожее».
// Сезоны и серии — в DetailsEpisodes.

package com.filmax.feature.details.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.person.CastMember
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.details.common.initials

/** Ширина карточки актёра и диаметр круглого аватара в секции «Актёры». */
private val ActorCardWidth = 88.dp
private val ActorAvatarSize = 72.dp

@Composable
internal fun DetailsAbout(item: Item, modifier: Modifier = Modifier) {
    if (item.plot.isNotBlank()) {
        Text(
            item.plot,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

/**
 * Состав тайтла карточками с фото. Фото приходят из TMDB ([DetailsState.cast]); пока их нет —
 * показываем те же карточки, но с инициалами вместо фото (имена всегда есть от kino.pub). Любая
 * карточка кликабельна и ведёт в фильмографию человека.
 */
@Composable
internal fun CastSection(
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

@Composable
internal fun SimilarSection(similar: List<Item>, onOpenItem: (Int) -> Unit, modifier: Modifier = Modifier) {
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
