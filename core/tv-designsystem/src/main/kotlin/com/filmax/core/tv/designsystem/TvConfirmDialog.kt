// Подтверждение необратимого действия поверх экрана: удалить папку, очистить историю, выйти.
// focusProperties.exit — ловушка фокуса внутри диалога; API помечен экспериментальным.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.filmax.core.tv.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

private val DialogWidth = 420.dp
private const val SCRIM_ALPHA = 0.7f
private const val DIALOG_ENTER_SCALE = 0.94f

/** Содержимое диалога: заголовок, пояснение и подпись кнопки действия («Удалить», «Выйти»). */
@Immutable
data class TvConfirmSpec(val title: String, val message: String, val confirmLabel: String)

/**
 * Диалог подтверждения поверх экрана. Рисуется в той же композиции, что и экран, а не отдельным
 * окном: фокус ловится внутри ([focusProperties] exit = Cancel), «Назад» отменяет. Фокус при
 * открытии — на «Отмена»: случайное «ОК» не должно удалить папку или стереть историю. Когда
 * диалог закрылся, фокус возвращается на [returnFocus] — элемент, который его открыл (если
 * после подтверждения он ушёл с экрана, запрос просто не сработает).
 */
@Composable
fun TvConfirmDialog(
    spec: TvConfirmSpec?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    returnFocus: FocusRequester,
) {
    val open = spec != null
    BackHandler(enabled = open, onBack = onDismiss)
    ReturnFocusAfterDialog(open, returnFocus)
    // AnimatedContent, а не AnimatedVisibility: уходящий диалог дорисовывается со своим текстом,
    // хотя spec уже null.
    AnimatedContent(
        targetState = spec,
        transitionSpec = {
            fadeIn(FxMotion.fade()) + scaleIn(FxMotion.panelIn(), initialScale = DIALOG_ENTER_SCALE) togetherWith
                fadeOut(FxMotion.panelOut())
        },
        label = "confirmDialog",
    ) { shown ->
        // Уходящий диалог ловушку фокуса снимает: иначе она отменила бы возврат фокуса на экран.
        if (shown != null) DialogPanel(shown, trapFocus = spec != null, DialogActions(onConfirm, onDismiss))
    }
}

@Composable
private fun ReturnFocusAfterDialog(open: Boolean, returnFocus: FocusRequester) {
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

private class DialogActions(val onConfirm: () -> Unit, val onDismiss: () -> Unit)

@Composable
private fun DialogPanel(spec: TvConfirmSpec, trapFocus: Boolean, actions: DialogActions) {
    val colors = LocalFxColors.current
    val cancelFocus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .focusProperties { exit = { if (trapFocus) FocusRequester.Cancel else FocusRequester.Default } }
            .focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(DialogWidth)
                .background(colors.background, TvMetrics.SheetShape)
                .fxGlass(TvMetrics.SheetShape)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(spec.title, style = FxType.rowTitle, color = colors.text)
            Text(spec.message, style = FxType.body, color = colors.textSecondary)
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(
                    text = "Отмена",
                    onClick = actions.onDismiss,
                    primary = false,
                    focusRequester = cancelFocus,
                    modifier = Modifier.onPlaced { if (!focused) focused = cancelFocus.requestFocus() },
                )
                DestructiveButton(spec.confirmLabel, actions.onConfirm)
            }
        }
    }
}

/** Кнопка необратимого действия — приглушённым красным палитры, как все ошибки и деструктив. */
@Composable
private fun DestructiveButton(label: String, onClick: () -> Unit) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        style = TvFocusStyle.Button,
        colors = TvSurfaceColors(container = colors.surfaceHigh, content = colors.error),
    ) {
        Text(
            label,
            style = FxType.button,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
