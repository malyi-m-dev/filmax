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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvBackHint
import com.filmax.core.tv.designsystem.TvBackdrop
import com.filmax.core.tv.designsystem.TvBackdropStyle
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvIconButton
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.FolderPickerEvent

private val InfoTop = 24.dp
private val InfoWidth = 430.dp
private val HeadingTop = 18.dp
private val CastGap = 12.dp
private val ButtonGap = 10.dp

/** Строка состава — подпись, а не текст: тише описания. */
private const val CAST_LINE_ALPHA = 0.55f

/** Строка состава и кнопки появляются следом за шапкой (у неё fxEnter 0–3). */
private const val CAST_ENTER_INDEX = 4
private const val BUTTONS_ENTER_INDEX = 5

/**
 * Сериал. Открывается фокусом на серии «продолжить» (её сезон выбран моделью), иначе на
 * главной кнопке. Сезон меняется фокусом на чипе; «←» из панели серий возвращает на кнопки,
 * а не на описание.
 */
@Composable
internal fun TvSeriesDetails(
    state: DetailsState,
    item: Item,
    onEvent: (DetailsEvent) -> Unit,
    sheets: DetailsSheetFocus,
) {
    val startAt = state.episodes?.startFocusEpisodeId?.let(::episodeKey)
    val focus = rememberTvScreenFocus(startAt = startAt ?: PLAY_KEY)
    val playFocus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize()) {
        TvBackdrop(item.posters.wide ?: item.posters.big, style = TvBackdropStyle.Split)
        Row(Modifier.fillMaxSize().then(focus.containerModifier)) {
            SeriesInfo(state, item, onEvent, SeriesFocus(focus, playFocus, sheets), Modifier.weight(1f))
            state.episodes?.let { panel ->
                TvEpisodesPanel(
                    panel = panel,
                    onEvent = onEvent,
                    focus = focus,
                    exitLeft = playFocus,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
    }
}

/** Ключ серии для памяти фокуса: возврат из плеера — на ту же серию. */
internal fun episodeKey(episodeId: Int): String = "episode:$episodeId"

/**
 * Фокус экрана сериала: память экрана, главная кнопка (на неё «←» возвращает из панели серий) и
 * панели — описание и «В папку» (на них фокус возвращается, когда панель закрылась).
 */
private data class SeriesFocus(val screen: TvScreenFocus, val play: FocusRequester, val sheets: DetailsSheetFocus)

/** Колонка информации — фиксированной ширины: длинное описание не растягивает её до панели. */
@Composable
private fun SeriesInfo(
    state: DetailsState,
    item: Item,
    onEvent: (DetailsEvent) -> Unit,
    focus: SeriesFocus,
    modifier: Modifier,
) {
    Column(
        modifier
            .padding(start = TvMetrics.SafeHorizontal, top = InfoTop)
            .wrapContentWidth(Alignment.Start)
            .width(InfoWidth),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TvBackHint("Назад")
        Column(Modifier.padding(top = HeadingTop), verticalArrangement = Arrangement.spacedBy(CastGap)) {
            DetailsHeading(
                item = item,
                heading = state.heading,
                description = HeadingDescription({ onEvent(DetailsEvent.OpenDescription) }, focus.sheets.description),
                style = SeriesHeadingStyle,
            )
            state.castLine?.let { CastLine(it, Modifier.fxEnter(CAST_ENTER_INDEX)) }
        }
        SeriesButtons(state, onEvent, focus, Modifier.fxEnter(BUTTONS_ENTER_INDEX))
    }
}

/** «В ролях: Анна Лунд, Эрик Сунд, Мия Хольм» — у сериала состав строкой, место занимают серии. */
@Composable
private fun CastLine(line: String, modifier: Modifier) {
    Text(
        line,
        style = FxType.meta,
        color = LocalFxColors.current.text.copy(alpha = CAST_LINE_ALPHA),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** «Продолжить · S4E7» и круглые «В список» / «В папку» / «Трейлер»: место в колонке занимают серии. */
@Composable
private fun SeriesButtons(
    state: DetailsState,
    onEvent: (DetailsEvent) -> Unit,
    focus: SeriesFocus,
    modifier: Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(ButtonGap)) {
        TvButton(
            state.play?.compact ?: "Смотреть",
            onClick = { onEvent(DetailsEvent.Play) },
            leadingIcon = Icons.Rounded.PlayArrow,
            modifier = focus.screen.item(PLAY_KEY).focusRequester(focus.play),
        )
        TvIconButton(
            icon = if (state.isFav) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            contentDescription = if (state.isFav) "В списке" else "Буду смотреть",
            onClick = { onEvent(DetailsEvent.ToggleFav) },
        )
        val folders = focus.sheets.folders
        TvIconButton(
            icon = folderIcon(folders.state.inAnyFolder),
            contentDescription = "В папку",
            onClick = { folders.onEvent(FolderPickerEvent.Open) },
            modifier = Modifier.focusRequester(folders.buttonFocus),
        )
        if (state.trailerUrl != null) {
            TvIconButton(
                icon = Icons.Rounded.Movie,
                contentDescription = "Трейлер",
                onClick = { onEvent(DetailsEvent.PlayTrailer) },
            )
        }
    }
}
