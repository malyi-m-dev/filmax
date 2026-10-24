// Нижняя панель плеера: выезжает по «вниз» поверх остановленного кадра, вкладки Серии · Аудио ·
// Субтитры · Качество · Скорость. Что открыто и какая вкладка — решает модель; панель рисует.
package com.filmax.feature.player.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.LocalReducedMotion
import com.filmax.core.tv.designsystem.TvKeyHint
import com.filmax.core.tv.designsystem.TvKeyHints
import com.filmax.core.tv.designsystem.TvPanelTab
import com.filmax.core.tv.designsystem.TvPanelTabs
import com.filmax.core.tv.designsystem.TvSeekBar
import com.filmax.core.tv.designsystem.fxGlass
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.label
import com.filmax.feature.player.common.openingTab
import com.filmax.feature.player.common.panelTabs
import com.filmax.feature.player.common.playerSubtitle
import com.filmax.feature.player.common.tabValue

private val PanelHeight = 330.dp
private val PanelMargin = 14.dp
private val PanelShape = RoundedCornerShape(24.dp)
private val PanelPaddingHorizontal = 44.dp
private val PanelPaddingTop = 18.dp
private val ContentTop = 18.dp
private val HandleWidth = 40.dp
private val HandleHeight = 4.dp
private val HandleOffset = (-10).dp
private val HideHintTop = 32.dp
private val HideHintEnd = 40.dp
private val ProgressTop = 150.dp
private val ProgressInset = 80.dp
private val ProgressThickness = 4.dp
private const val HANDLE_ALPHA = 0.25f

/** Тёмное стекло панели: она лежит на кадре, и подписи вариантов должны читаться на любом. */
private val PanelFill = Color(0xD112101C)

/** Затемнение кадра под открытой панелью: сверху — под названием, к низу — почти глухое. */
private val PanelScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.55f),
    0.3f to Color.Black.copy(alpha = 0.35f),
    0.62f to Color.Black.copy(alpha = 0.9f),
)

/**
 * Слой открытой панели: затемнение, название с «на паузе», тонкий прогресс и сама панель. Панель
 * выезжает снизу (сниженная анимация — только проявляется), «вверх» со вкладок и «Назад» её
 * закрывают, воспроизведение продолжается.
 */
@Composable
internal fun PanelOverlay(state: PlayerState, transport: TvTransportState, actions: PlayerActions) {
    val reduced = LocalReducedMotion.current
    AnimatedVisibility(
        visible = state.panelOpen,
        enter = fadeIn(FxMotion.fade()),
        exit = fadeOut(FxMotion.panelOut()),
    ) {
        Box(Modifier.fillMaxSize().background(PanelScrim)) {
            PlayerTitle(
                state.item?.title.orEmpty(),
                playerSubtitle(state, paused = true),
                Modifier.align(Alignment.TopStart)
            )
            TvKeyHints(
                listOf(TvKeyHint(listOf(Icons.Rounded.KeyboardArrowUp), "скрыть панель")),
                Modifier.align(Alignment.TopEnd).padding(top = HideHintTop, end = HideHintEnd),
            )
            PanelProgress(transport)
            PlayerPanel(
                state = state,
                transport = transport,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .animateEnterExit(
                        enter = if (reduced) EnterTransition.None else slideInVertically(FxMotion.panelIn()) { it },
                        exit = if (reduced) ExitTransition.None else slideOutVertically(FxMotion.panelOut()) { it },
                    ),
            )
        }
    }
}

/** Тонкий прогресс над панелью: где мы в серии, пока выбирают дорожку. */
@Composable
private fun PanelProgress(transport: TvTransportState) {
    Column(Modifier.padding(top = ProgressTop, start = ProgressInset, end = ProgressInset)) {
        TvSeekBar(
            progress = { fraction(transport.positionMs, transport.durationMs) },
            buffered = { 0f },
            thickness = ProgressThickness,
            thumb = false,
        )
        TransportTimes(transport, Modifier.padding(top = 6.dp))
    }
}

/**
 * Сама панель. Фокус при открытии — на выбранном варианте открытой вкладки (играющая серия,
 * текущая дорожка): поменять соседнее — одно нажатие. Вкладка меняется фокусом на ней.
 */
@Composable
private fun PlayerPanel(state: PlayerState, transport: TvTransportState, actions: PlayerActions, modifier: Modifier) {
    val tabs = state.panelTabs()
    val tab = state.openingTab()
    val contentFocus = remember { FocusRequester() }
    // Панель размещена к следующему кадру после появления — тогда и забираем в неё фокус.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        contentFocus.requestFocus()
    }
    Column(
        modifier
            .padding(PanelMargin)
            .fillMaxWidth()
            .height(PanelHeight)
            .fxGlass(PanelShape, fill = PanelFill)
            .trapFocus()
            .padding(start = PanelPaddingHorizontal, end = PanelPaddingHorizontal, top = PanelPaddingTop),
    ) {
        Handle(Modifier.align(Alignment.CenterHorizontally))
        TvPanelTabs(
            tabs = tabs.map { TvPanelTab(it.label, it.icon, tabValue(it, state)) },
            selected = tabs.indexOf(tab),
            onSelect = { index -> actions.dispatch(PlayerEvent.SelectTab(tabs[index])) },
            modifier = Modifier.closeOnUp { actions.dispatch(PlayerEvent.ClosePanel) },
        )
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = ContentTop)) {
            key(tab) { PanelTabContent(tab, state, transport, actions, contentFocus) }
        }
        PanelHints(tab)
    }
}

/**
 * Фокус из открытой панели не выходит стрелками: под ней лежат скрытые контролы, и «вниз» с ряда
 * серий иначе увёл бы фокус на невидимую паузу. Выход — только «вверх» со вкладок и «Назад».
 */
// focusProperties.exit — ловушка фокуса внутри панели; API помечен экспериментальным.
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.trapFocus(): Modifier = focusProperties { exit = { FocusRequester.Cancel } }.focusGroup()

/** «Вверх» с ряда вкладок — выше ничего нет, это закрытие панели (так в макете: «˄ скрыть панель»). */
private fun Modifier.closeOnUp(onClose: () -> Unit): Modifier = onPreviewKeyEvent { event ->
    val up = event.key == Key.DirectionUp
    if (up && event.type == KeyEventType.KeyDown) onClose()
    up
}

/** Ручка панели — намёк, что это шторка, которую можно убрать. */
@Composable
private fun Handle(modifier: Modifier) {
    Box(
        modifier
            .offset(y = HandleOffset)
            .size(HandleWidth, HandleHeight)
            .background(Color.White.copy(alpha = HANDLE_ALPHA), CircleShape),
    )
}

@Composable
private fun PanelHints(tab: PlayerTab) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)) {
        TvKeyHints(
            listOf(
                TvKeyHint(listOf(Icons.Rounded.KeyboardArrowLeft, Icons.Rounded.KeyboardArrowRight), tab.moveHint),
                TvKeyHint(listOf(Icons.Rounded.KeyboardArrowUp, Icons.Rounded.KeyboardArrowDown), "вкладка"),
                // Выбор применяется по OK, а не фокусом — без подсказки это неочевидно.
                TvKeyHint(emptyList(), "включить", keyText = "OK"),
            ),
            Modifier.weight(1f),
        )
        TvKeyHints(listOf(TvKeyHint(listOf(Icons.AutoMirrored.Rounded.ArrowBack), "к видео")))
    }
}
