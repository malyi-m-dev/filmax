// Боковая панель поверх экрана: полное описание, выбор папки, значение настройки. Экран под ней
// размывается и темнеет, фокус уходит в панель и не выходит из неё, «Назад» закрывает панель.
package com.filmax.core.tv.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

private val SheetWidth = 560.dp
private val SheetMargin = 10.dp
private const val SHEET_ALPHA = 0.94f

/**
 * Хост боковой панели. [content] — экран; пока [open], он размыт (Android 12+, ниже и в
 * облегчённом режиме — только затемнение) и затемнён до 0.62, а справа выезжает [sheet]
 * шириной 560dp. Фокус при открытии уходит в панель и выйти из неё стрелками не может;
 * «Назад» зовёт [onClose]. Когда панель закрылась, фокус возвращается на [returnFocus] —
 * элемент, который её открыл.
 */
@Composable
fun TvSheetHost(
    open: Boolean,
    onClose: () -> Unit,
    returnFocus: FocusRequester,
    sheet: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler(enabled = open, onBack = onClose)
    ReturnFocusOnClose(open, returnFocus)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().fxBackdrop(open)) {
            // Под панелью бесконечные анимации экрана на паузе — иначе размытие считалось бы
            // заново на каждом кадре ken-burns, которого не видно.
            CompositionLocalProvider(LocalFxPaused provides open, content = content)
        }
        AnimatedVisibility(
            visible = open,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(FxMotion.panelIn()) { it } + fadeIn(FxMotion.fade()),
            exit = slideOutHorizontally(FxMotion.panelOut()) { it } + fadeOut(FxMotion.panelOut()),
        ) {
            SheetPanel(open, sheet)
        }
    }
}

/** Закрылась панель — фокус обратно на то, что её открыло, а не в начало экрана. */
@Composable
private fun ReturnFocusOnClose(open: Boolean, returnFocus: FocusRequester) {
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open) {
            wasOpen = true
        } else if (wasOpen) {
            wasOpen = false
            returnFocus.requestFocus()
        }
    }
}

// focusProperties.exit — ловушка фокуса внутри панели; API помечен экспериментальным.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SheetPanel(open: Boolean, sheet: @Composable BoxScope.() -> Unit) {
    val panelFocus = remember { FocusRequester() }
    var focusInside by remember { mutableStateOf(false) }
    // Панель уже размещена к следующему кадру после появления — тогда и забираем в неё фокус.
    // Если содержимое уже поставило его само (список папок — на нужную строку), не трогаем:
    // запрос фокуса группе увёл бы его на верхний левый элемент панели.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!focusInside) panelFocus.requestFocus()
    }
    val colors = LocalFxColors.current
    Box(
        Modifier
            .padding(SheetMargin)
            .width(SheetWidth)
            .fillMaxHeight()
            .background(colors.background.copy(alpha = SHEET_ALPHA), TvMetrics.SheetShape)
            .fxGlass(TvMetrics.SheetShape)
            .focusRequester(panelFocus)
            .onFocusChanged { focusInside = it.hasFocus }
            // Ловушка только пока панель открыта: отменённый выход отменяет и requestFocus извне,
            // и закрывающаяся панель не отдала бы фокус элементу, который её открыл.
            .focusProperties { exit = { if (open) FocusRequester.Cancel else FocusRequester.Default } }
            .focusGroup(),
        content = sheet,
    )
}
