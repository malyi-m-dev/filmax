package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeSidePanel
import com.filmax.core.domain.error.AppError

/**
 * Оформление модалки: иконка, код и подписи кнопок. Заголовок и текст сюда не входят — они общие
 * с TV и живут в [appErrorText]. Цвет у всех ошибок один — акцент палитры: модалка говорит текстом
 * и иконкой, а не своим цветом на каждый случай.
 */
private data class ErrorVisual(
    val icon: ImageVector,
    val code: String,
    val primary: String,
    val secondary: String? = null,
    val retry: Boolean = false,
)

private fun visualFor(error: AppError): ErrorVisual = when (error) {
    AppError.Offline -> ErrorVisual(Icons.Filled.WifiOff, "NET · НЕТ СЕТИ", "Повторить", retry = true)
    AppError.Server -> ErrorVisual(Icons.Filled.CloudOff, "ERROR 500", "Повторить", "Назад", retry = true)
    AppError.Timeout -> ErrorVisual(Icons.Filled.Schedule, "ERROR 408", "Повторить", "Отмена", retry = true)
    AppError.NotFound -> ErrorVisual(Icons.Filled.VisibilityOff, "ERROR 404", "В каталог")
    AppError.Empty -> ErrorVisual(Icons.Filled.SearchOff, "ПУСТО", "Сбросить фильтры")
    AppError.Premium -> ErrorVisual(Icons.Filled.WorkspacePremium, "ERROR 402", "Оформить Premium", "Позже")
    AppError.Region -> ErrorVisual(Icons.Filled.Public, "ERROR 403", "Понятно")
    AppError.Auth -> ErrorVisual(Icons.Filled.Lock, "ERROR 401", "Войти заново", "Отмена")
    AppError.Playback -> ErrorVisual(Icons.Filled.ErrorOutline, "PLAYER", "Повторить", "Закрыть", retry = true)
}

/**
 * Модальное окно ошибки Filmax — резолвится из [AppError]: иконка, код, заголовок, текст и до двух
 * действий.
 *
 * Показывается, когда `screenModel.collectErrorAsState()` не null:
 * ```
 * val error by screenModel.collectErrorAsState()
 * error?.let {
 *     FilmaxErrorModal(it, onDismiss = screenModel::dismissError, onPrimary = { … })
 * }
 * ```
 */
@Composable
fun FilmaxErrorModal(
    error: AppError,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)? = null,
) {
    val visual = visualFor(error)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(ShapeSidePanel)
                .background(FilmaxTheme.palette.background)
                .border(1.dp, FilmaxColors.PanelBorder, ShapeSidePanel)
                .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            ErrorModalCloseButton(onDismiss)
            ErrorModalContent(visual, appErrorText(error), onPrimary, onSecondary ?: onDismiss)
        }
    }
}

@Composable
private fun BoxScope.ErrorModalCloseButton(onDismiss: () -> Unit) {
    FilmaxIconButton(
        Icons.Filled.Close,
        contentDescription = "Закрыть",
        onClick = onDismiss,
        size = CloseSize,
        modifier = Modifier.align(Alignment.TopEnd),
    )
}

/** Содержимое модалки: иконка в плитке, код, заголовок, текст и кнопки действий. */
@Composable
private fun ErrorModalContent(
    visual: ErrorVisual,
    text: AppErrorText,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val palette = FilmaxTheme.palette
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(IconShape)
                .background(palette.accent.copy(alpha = ICON_FILL_ALPHA))
                .border(1.dp, palette.accent.copy(alpha = ICON_BORDER_ALPHA), IconShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(visual.icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(visual.code, style = FilmaxType.overline, color = palette.accentText)
        Spacer(Modifier.height(10.dp))
        Text(text.title, style = FilmaxType.sheetTitle, color = FilmaxColors.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            text.message,
            style = FilmaxType.body,
            color = FilmaxColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(26.dp))
        FilmaxButton(
            text = visual.primary,
            onClick = onPrimary,
            icon = Icons.Filled.Refresh.takeIf { visual.retry },
            modifier = Modifier.fillMaxWidth(),
        )
        visual.secondary?.let {
            Spacer(Modifier.height(10.dp))
            FilmaxButton(
                text = it,
                onClick = onSecondary,
                style = FilmaxButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val IconShape = RoundedCornerShape(26.dp)
private val CloseSize = 36.dp
private const val ICON_FILL_ALPHA = 0.18f
private const val ICON_BORDER_ALPHA = 0.3f
