// Боковая панель плеера: вкладки «Серии · Аудио · Субтитры · Качество · Скорость». Открыта ли она
// и какая вкладка, решает PlayerScreenModel: открытие ставит видео на паузу, закрытие возвращает.

package com.filmax.feature.player.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassLevel
import com.filmax.core.designsystem.GlassPanel
import com.filmax.core.designsystem.ShapeSidePanel
import com.filmax.core.ui.components.EpisodeMark
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxChipSize
import com.filmax.core.ui.components.FilmaxEpisodeRow
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxRadioRow
import com.filmax.core.ui.components.FilmaxTabButton
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.WATCH_STATUS_FINISHED
import com.filmax.feature.player.common.episodeLength
import com.filmax.feature.player.common.episodeProgress
import com.filmax.feature.player.common.episodeTitle
import com.filmax.feature.player.common.optionEvent
import com.filmax.feature.player.common.optionItems
import com.filmax.feature.player.common.panelTitle
import com.filmax.feature.player.common.playerSeasons
import com.filmax.feature.player.common.tabNote

@Composable
internal fun PlayerSidePanel(state: PlayerState, onEvent: (PlayerEvent) -> Unit) {
    val close = { onEvent(PlayerEvent.ClosePanel) }
    BackHandler(enabled = state.panelOpen, onBack = close)
    AnimatedVisibility(visible = state.panelOpen, enter = fadeIn(), exit = fadeOut()) {
        // Тап мимо панели закрывает её — как у шторки.
        Box(
            Modifier
                .fillMaxSize()
                .background(PanelScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = close,
                ),
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        AnimatedVisibility(
            visible = state.panelOpen,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            // Видео — отдельная поверхность, размыть её стеклу нечем: панель плотнее, текст читается.
            GlassPanel(
                state = null,
                shape = ShapeSidePanel,
                level = GlassLevel.Sheet,
                modifier = Modifier
                    .windowInsetsPadding(OverlayTopInsets)
                    .padding(12.dp)
                    .width(PanelWidth)
                    .fillMaxHeight(),
            ) {
                PanelContent(state = state, onEvent = onEvent, onClose = close)
            }
        }
    }
}

@Composable
private fun PanelContent(state: PlayerState, onEvent: (PlayerEvent) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                panelTitle(state),
                style = FilmaxType.meta,
                color = FilmaxColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            FilmaxIconButton(Icons.Filled.Close, contentDescription = "Закрыть", onClick = onClose, size = CloseSize)
        }
        PanelTabs(state = state, onSelect = { onEvent(PlayerEvent.SelectTab(it)) })
        Box(Modifier.fillMaxSize().padding(start = 6.dp, end = 6.dp, top = 10.dp)) {
            when (val tab = state.panelTab) {
                PlayerTab.Episodes -> EpisodesTab(state = state, onEvent = onEvent)
                PlayerTab.Speed -> SpeedTab(state = state, onEvent = onEvent)
                null -> Unit
                else -> OptionsTab(tab = tab, state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun PanelTabs(state: PlayerState, onSelect: (PlayerTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp)
            .clip(TabsShape)
            .background(FilmaxColors.Field)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        state.panelTabs().forEach { tab ->
            FilmaxTabButton(
                icon = tabIcon(tab),
                label = tab.label,
                selected = tab == state.panelTab,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Серии сезона, который листают; играющая подсвечена, тап по ней просто закрывает панель. */
@Composable
private fun EpisodesTab(state: PlayerState, onEvent: (PlayerEvent) -> Unit) {
    val seasons = playerSeasons(state.item?.tracklist.orEmpty())
    val shown = state.shownSeason() ?: return
    val playingId = state.track?.id
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (seasons.size > 1) {
            item(key = "seasons") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                ) {
                    items(seasons, key = { it.number }) { season ->
                        FilmaxChip(
                            label = "Сезон ${season.number}",
                            selected = season.number == shown.number,
                            onClick = { onEvent(PlayerEvent.ShowSeason(season.number)) },
                            size = FilmaxChipSize.Small,
                        )
                    }
                }
            }
        }
        items(shown.episodes, key = { it.id }) { episode ->
            val playing = episode.id == playingId
            FilmaxEpisodeRow(
                title = episodeTitle(episode),
                meta = episodeLength(episode),
                thumbUrl = episode.thumbnail,
                mark = when {
                    playing -> EpisodeMark.Current
                    episode.watchStatus == WATCH_STATUS_FINISHED -> EpisodeMark.Watched
                    else -> EpisodeMark.Normal
                },
                progress = episodeProgress(episode) ?: 0f,
                onClick = {
                    if (playing) {
                        onEvent(PlayerEvent.ClosePanel)
                    } else {
                        onEvent(PlayerEvent.PlayEpisode(episode.seasonNumber, episode.number))
                    }
                },
                placeholder = episode.number.toString(),
                compact = true,
            )
        }
    }
}

/** Дорожки, субтитры, качество: варианты радиокнопками, выбор применяется сразу. */
@Composable
private fun OptionsTab(tab: PlayerTab, state: PlayerState, onEvent: (PlayerEvent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(optionItems(tab, state), key = { it.key }) { option ->
            FilmaxRadioRow(
                label = option.text.title,
                selected = option.selected,
                onClick = { optionEvent(tab, option.key)?.let(onEvent) },
                subtitle = option.text.detail,
                badge = option.text.badge,
            )
        }
        tabNote(tab, state.isSeries)?.let { note ->
            item(key = "note") {
                Text(
                    note,
                    style = FilmaxType.metaSmall,
                    color = FilmaxColors.TextDim,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }
}

private val PanelWidth = 404.dp
private val CloseSize = 32.dp
private val TabsShape = RoundedCornerShape(20.dp)
private val PanelScrim = Color.Black.copy(alpha = 0.35f)
