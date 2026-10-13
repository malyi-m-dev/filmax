// Сериальная часть экрана деталей: чипы сезонов и карусель серий выбранного сезона.
//
// Устроено как у Кинопоиска, Okko и Wink: сезон переключается одним касанием по чипу, серии
// листаются горизонтально. Раньше сезон прятался в выпадающем меню, а серии шли вертикальной
// простынёй: на сезоне в 24 серии «Похожее» уезжало на десяток экранов вниз, а сменить сезон
// можно было, только пролистав обратно к меню.

package com.filmax.feature.details.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxProgressCard
import com.filmax.feature.details.common.episodeMeta
import com.filmax.feature.details.common.episodeProgress
import com.filmax.feature.details.common.episodeTitle
import com.filmax.feature.details.common.seasonLabel
import com.filmax.feature.details.common.startEpisodeIndex

private const val EPISODES_TITLE = "Эпизоды"

private val SeasonChipGap = 8.dp

/** Данные и действия секции эпизодов — группой (detekt LongParameterList). */
internal data class EpisodesSectionData(
    val seasons: List<Pair<Int, List<MediaTrack>>>,
    val episodes: List<MediaTrack>,
    val selectedSeason: Int,
    /** Серия «продолжить» — с неё открывается карусель, если она в выбранном сезоне. */
    val resume: MediaTrack?,
    val onSelectSeason: (Int) -> Unit,
    val onPlayEpisode: (season: Int, videoId: Int) -> Unit,
)

@Composable
internal fun EpisodesSection(data: EpisodesSectionData, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            EPISODES_TITLE,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = FilmaxMetrics.DetailsPadding),
        )
        // Чипы — только когда сезонов больше одного: выбор из единственного варианта ничего не
        // выбирает, а место и внимание забирает.
        if (data.seasons.size > 1) {
            SeasonChips(
                seasons = data.seasons,
                selected = data.selectedSeason,
                onSelect = data.onSelectSeason,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        // Карусель пересоздаётся на каждый сезон: у соседнего сезона другие серии и своя стартовая
        // серия, прокрутка прошлого сезона ему ни к чему.
        key(data.selectedSeason) {
            EpisodesCarousel(
                episodes = data.episodes,
                startIndex = startEpisodeIndex(data.episodes, data.resume?.id),
                onPlayEpisode = data.onPlayEpisode,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun SeasonChips(
    seasons: List<Pair<Int, List<MediaTrack>>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ряд открывается на выбранном сезоне (это сезон недосмотренной серии), а предыдущий остаётся
    // виден слева: у сериала на 10+ сезонов выбранный иначе оказался бы за краем экрана, а без
    // соседа слева непонятно, что ряд листается и назад.
    val chipsState = rememberLazyListState(initialFirstVisibleItemIndex = (selected - 1).coerceAtLeast(0))
    LazyRow(
        state = chipsState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = FilmaxMetrics.DetailsPadding),
        horizontalArrangement = Arrangement.spacedBy(SeasonChipGap),
    ) {
        itemsIndexed(seasons, key = { _, season -> season.first }) { index, season ->
            FilmaxChip(
                label = seasonLabel(season.first),
                selected = index == selected,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun EpisodesCarousel(
    episodes: List<MediaTrack>,
    startIndex: Int,
    onPlayEpisode: (season: Int, videoId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val carouselState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    // Ряд полноширинный, поля — через contentPadding: внутри общих боковых полей последняя
    // карточка обрезалась бы по правому краю, и ряд читался бы как сломанный.
    LazyRow(
        state = carouselState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = FilmaxMetrics.DetailsPadding),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
    ) {
        items(episodes, key = { it.id }) { episode ->
            FilmaxProgressCard(
                title = episodeTitle(episode),
                meta = episodeMeta(episode),
                posterUrl = episode.thumbnail,
                progress = episodeProgress(episode),
                // Плееру нужны НОМЕР серии (API `video`), а не id трека, и сезон: номер
                // уникален только внутри сезона.
                onClick = { onPlayEpisode(episode.seasonNumber, episode.number) },
                placeholder = "${episode.number}",
            )
        }
    }
}
