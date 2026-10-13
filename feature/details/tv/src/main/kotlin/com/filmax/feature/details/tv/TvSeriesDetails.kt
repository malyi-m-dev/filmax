// Детали сериала: две колонки — информация и действия слева (не прокручиваются), стеклянная
// панель «Эпизоды» справа — прокручивается только она.
package com.filmax.feature.details.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvBackHint
import com.filmax.core.tv.designsystem.TvBackdrop
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvIconButton
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.resolveCast

private val InfoTop = 24.dp
private val InfoEndGap = 24.dp

/** Сколько актёров в строке «В ролях» у сериала — дальше строка уходит за край колонки. */
private const val CAST_LINE_NAMES = 3

/**
 * Сериал. Открывается фокусом на серии «продолжить» (её сезон выбран моделью), иначе на
 * главной кнопке. Сезон меняется фокусом на чипе; «←» из панели серий возвращает на кнопки,
 * а не на описание.
 */
@Composable
internal fun TvSeriesDetails(
    state: DetailsState,
    item: Item,
    actions: DetailsActions,
    descriptionFocus: FocusRequester
) {
    val resume = state.series?.resume
    val startAt = resume?.takeIf { episode -> state.seasonEpisodes.any { it.id == episode.id } }?.let(::episodeKey)
    val focus = rememberTvScreenFocus(startAt = startAt ?: PLAY_KEY)
    val playFocus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize()) {
        TvBackdrop(item.posters.wide ?: item.posters.big)
        Row(Modifier.fillMaxSize().then(focus.containerModifier)) {
            SeriesInfo(state, item, actions, SeriesFocus(focus, playFocus, descriptionFocus), Modifier.weight(1f))
            TvEpisodesPanel(
                state = state,
                actions = actions,
                focus = focus,
                exitLeft = playFocus,
                modifier = Modifier.width(TvMetrics.EpisodesPanelWidth).fillMaxHeight(),
            )
        }
    }
}

/** Ключ серии для памяти фокуса: возврат из плеера — на ту же серию. */
internal fun episodeKey(episode: MediaTrack): String = "episode:${episode.id}"

/**
 * Фокус экрана сериала: память экрана, главная кнопка (на неё «←» возвращает из панели серий) и
 * описание (на него фокус возвращается, когда закрылась панель полного текста).
 */
private data class SeriesFocus(val screen: TvScreenFocus, val play: FocusRequester, val description: FocusRequester)

@Composable
private fun SeriesInfo(
    state: DetailsState,
    item: Item,
    actions: DetailsActions,
    focus: SeriesFocus,
    modifier: Modifier,
) {
    Column(
        modifier.padding(start = TvMetrics.SafeHorizontal, top = InfoTop, end = InfoEndGap),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TvBackHint("Назад")
        DetailsHeading(item, state.series, actions.onOpenDescription, focus.description, Modifier.padding(top = 18.dp))
        CastLine(state, item)
        SeriesButtons(state, actions, focus)
    }
}

/** «В ролях: Анна Лунд, Эрик Сунд, Мия Хольм» — у сериала состав строкой, место занимают серии. */
@Composable
private fun CastLine(state: DetailsState, item: Item) {
    val names = resolveCast(state.cast, item.cast).take(CAST_LINE_NAMES).joinToString(", ") { it.name }
    if (names.isBlank()) return
    Text(
        "В ролях: $names",
        style = FxType.metaRow,
        color = LocalFxColors.current.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** «Продолжить · S4E7» и круглые «В список» / «Трейлер»: место в колонке занимают серии. */
@Composable
private fun SeriesButtons(state: DetailsState, actions: DetailsActions, focus: SeriesFocus) {
    val resume = state.series?.resume
    // Кнопка играет серию «продолжить», иначе первую серию выбранного сезона.
    val target = resume ?: state.seasonEpisodes.firstOrNull()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvButton(
            playLabel(resume),
            onClick = { target?.let { actions.onPlay(it.seasonNumber, it.number) } },
            leadingIcon = Icons.Rounded.PlayArrow,
            modifier = focus.screen.item(PLAY_KEY).focusRequester(focus.play),
        )
        TvIconButton(
            icon = if (state.isFav) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            contentDescription = if (state.isFav) "В списке" else "Буду смотреть",
            onClick = actions.onToggleWatchLater,
        )
        actions.onTrailer?.let { onTrailer ->
            TvIconButton(icon = Icons.Rounded.Movie, contentDescription = "Трейлер", onClick = onTrailer)
        }
    }
}
