package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeSheet
import kotlinx.coroutines.launch

/** Содержимое шторки: колонка и [dismiss] — закрыть с анимацией ухода вниз. */
interface FilmaxSheetScope : ColumnScope {
    fun dismiss()
}

private class SheetScope(column: ColumnScope, private val onDismiss: () -> Unit) :
    FilmaxSheetScope,
    ColumnScope by column {
    override fun dismiss() = onDismiss()
}

/**
 * Шторка Filmax: стекло палитры сверху экрана, ручка, заголовок с подписью и круглая «✕».
 * Описание тайтла, выбор папки, разделы, фильтры — все шторки приложения одного вида.
 *
 * [onDismiss] зовётся, когда шторка уже уехала: смахнули, тапнули мимо, «✕» или [FilmaxSheetScope.dismiss]
 * из содержимого («Готово»). Экран в ответ убирает шторку из композиции.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmaxSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable FilmaxSheetScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    val dismiss: () -> Unit = remember(state, scope) {
        {
            scope.launch { state.hide() }.invokeOnCompletion { currentOnDismiss.value() }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = ShapeSheet,
        // Шторка — отдельное окно: размыть экран под ней нечем, а полупрозрачный контейнер окна
        // смешивается с ним сильнее заданной прозрачности, и текст шторки тонет в ленте. Поэтому — плотно.
        containerColor = FilmaxTheme.palette.background,
        contentColor = FilmaxColors.Text,
        scrimColor = FilmaxColors.Scrim,
        dragHandle = { SheetHandle() },
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth()) {
            if (title != null) FilmaxSheetHeader(title = title, subtitle = subtitle, onClose = dismiss)
            SheetScope(this, dismiss).content()
        }
    }
}

/** Заголовок шторки: название, подпись под ним и «✕» справа. */
@Composable
fun FilmaxSheetHeader(title: String, subtitle: String?, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = FilmaxMetrics.ScreenPadding, end = 16.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = FilmaxType.sheetTitle,
                color = FilmaxColors.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            subtitle?.let {
                Text(
                    it,
                    style = FilmaxType.meta,
                    color = FilmaxColors.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        FilmaxIconButton(Icons.Filled.Close, contentDescription = "Закрыть", onClick = onClose, size = CloseSize)
    }
}

@Composable
private fun SheetHandle() {
    Box(Modifier.padding(top = 10.dp, bottom = 14.dp)) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(HandleShape)
                .background(HandleColor),
        )
    }
}

private val CloseSize = 36.dp
private val HandleShape = RoundedCornerShape(2.dp)
private val HandleColor = Color.White.copy(alpha = 0.35f)

/**
 * Шторка подтверждения необратимого действия: «Очистить историю?», «Удалить папку?». Кнопка
 * действия — цветом ошибки ([destructive]): это единственное место, где красный — не про сбой.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    FilmaxSheet(onDismiss = onDismiss, title = title) {
        val sheet = this
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = FilmaxMetrics.ScreenPadding)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(message, style = FilmaxType.body, color = FilmaxColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilmaxButton(
                    text = "Отмена",
                    onClick = sheet::dismiss,
                    style = FilmaxButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                FilmaxButton(
                    text = confirmLabel,
                    onClick = {
                        onConfirm()
                        sheet.dismiss()
                    },
                    style = if (destructive) FilmaxButtonStyle.Destructive else FilmaxButtonStyle.Primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
