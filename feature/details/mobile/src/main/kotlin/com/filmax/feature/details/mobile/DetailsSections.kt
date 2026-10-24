// Секции под шапкой деталей: описание со ссылкой на полный текст, серии сериала, актёры, факты
// «О фильме» и «Похожие». Порядок — как на ТВ: у сериала серии сразу под описанием, ради них
// карточку сериала и открывают.

package com.filmax.feature.details.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.person.CastMember
import com.filmax.core.ui.components.FilmaxCastCard
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxSectionHeader
import com.filmax.core.ui.components.FilmaxTextButton
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsFact
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.detailsFacts
import com.filmax.feature.details.common.resolveCast

/** Сколько строк описания видно до «Читать полностью». */
private const val DESCRIPTION_LINES = 3

/** Секции ленты деталей после шапки. */
internal fun LazyListScope.detailsSections(item: Item, state: DetailsState, actions: DetailsActions) {
    if (item.plot.isNotBlank()) {
        item(key = "description") {
            Description(plot = item.plot, onReadMore = { actions.onDetails(DetailsEvent.OpenDescription) })
        }
    }
    if (state.series != null) episodesSection(state = state, actions = actions)
    // Люди: фото из TMDB, если доехали; иначе — имена из строки kino.pub. Режиссёр — первым.
    val people = resolveCast(state.cast, item.cast)
    if (people.isNotEmpty() || item.director.isNotBlank()) {
        item(key = "cast") { CastRow(people = people, director = item.director, onOpenPerson = actions.onOpenPerson) }
    }
    val facts = detailsFacts(item, state.series)
    if (facts.isNotEmpty()) {
        item(key = "facts") { Facts(title = if (state.series == null) "О фильме" else "О сериале", facts = facts) }
    }
    if (state.similar.isNotEmpty()) {
        item(key = "similar") { SimilarRow(similar = state.similar, onOpenItem = actions.onOpenItem) }
    }
}

@Composable
private fun Description(plot: String, onReadMore: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = FilmaxMetrics.ScreenPadding, end = 12.dp, top = 22.dp)) {
        Text(
            plot,
            style = FilmaxType.body,
            color = FilmaxColors.TextSecondary,
            maxLines = DESCRIPTION_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp),
        )
        FilmaxTextButton(text = "Читать полностью ›", onClick = onReadMore, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun CastRow(people: List<CastMember>, director: String, onOpenPerson: (String, Boolean) -> Unit) {
    Column {
        FilmaxSectionHeader(title = "Актёры")
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Режиссёров у kino.pub строка через запятую — карточка на каждого, с его фильмографией.
            val directors = director.split(",").map { it.trim() }.filter { it.isNotBlank() }
            items(directors, key = { "director-$it" }) { name ->
                FilmaxCastCard(name = name, photoUrl = null, role = "Режиссёр", onClick = { onOpenPerson(name, true) })
            }
            items(people, key = { "actor-${it.name}" }) { person ->
                FilmaxCastCard(
                    name = person.name,
                    photoUrl = person.photoUrl,
                    role = person.character,
                    onClick = { onOpenPerson(person.name, false) },
                )
            }
        }
    }
}

/** Факты сеткой «подпись — значение»: подписи одной колонкой, значения выровнены по ней. */
@Composable
internal fun Facts(title: String?, facts: List<DetailsFact>, modifier: Modifier = Modifier) {
    Column(modifier) {
        title?.let { FilmaxSectionHeader(title = it) }
        Column(
            Modifier.padding(horizontal = FilmaxMetrics.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            facts.forEach { fact ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        fact.label,
                        style = FilmaxType.bodySmall,
                        color = FilmaxColors.TextDim,
                        modifier = Modifier.width(FactLabelWidth)
                    )
                    Text(
                        fact.value,
                        style = FilmaxType.bodySmall,
                        color = FilmaxColors.Text,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SimilarRow(similar: List<Item>, onOpenItem: (Int) -> Unit) {
    Column {
        FilmaxSectionHeader(title = "Похожие")
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        ) {
            items(similar, key = { it.id }) { item ->
                FilmaxPosterCard(
                    title = item.title,
                    posterUrl = item.posters.medium,
                    onClick = { onOpenItem(item.id) },
                    rating = ratingLabel(item.rating.kinopoisk),
                    meta = posterMeta(typeLabel(item.type), item.year),
                    modifier = Modifier.width(FilmaxMetrics.PosterWidth),
                )
            }
        }
    }
}

private val FactLabelWidth = 110.dp
