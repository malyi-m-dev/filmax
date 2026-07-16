package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.domain.error.AppError

// Палитра модалок ошибок. Цвета вынесены в именованные константы (detekt MagicNumber);
// значения не менялись. Цвета — фирменные для каждого типа [AppError], не из темы Material.
private val OfflineAccent = Color(0xFFF4B792)
private val OfflineOnAccent = Color(0xFF4F2500)
private val ServerAccent = Color(0xFFB4305A)
private val TimeoutAccent = Color(0xFFE8A43A)
private val TimeoutOnAccent = Color(0xFF3A2705)
private val NotFoundAccent = Color(0xFF9E8B91)
private val NotFoundOnAccent = Color(0xFF1A1012)
private val EmptyAccent = Color(0xFF6AC2B0)
private val EmptyOnAccent = Color(0xFF06251D)
private val PremiumAccent = Color(0xFFD4A84A)
private val PremiumOnAccent = Color(0xFF3A2C05)
private val RegionAccent = Color(0xFF6B4B8F)
private val AuthAccent = Color(0xFFE46962)

private data class ErrorVisual(
    val icon: ImageVector,
    val color: Color,
    val onColor: Color,
    val code: String,
    val title: String,
    val body: String,
    val primary: String,
    val secondary: String?,
    val retry: Boolean,
)

private fun visualFor(error: AppError): ErrorVisual = when (error) {
    AppError.Offline -> ErrorVisual(
        Icons.Filled.WifiOff, OfflineAccent, OfflineOnAccent,
        "NET · НЕТ СЕТИ", "Нет подключения",
        "Проверьте интернет-соединение и попробуйте снова.",
        "Повторить", null, retry = true,
    )
    AppError.Server -> ErrorVisual(
        Icons.Filled.CloudOff, ServerAccent, Color.White,
        "ERROR 500", "Что-то пошло не так",
        "На нашей стороне сбой. Мы уже разбираемся — попробуйте через минуту.",
        "Повторить", "Назад", retry = true,
    )
    AppError.Timeout -> ErrorVisual(
        Icons.Filled.Schedule, TimeoutAccent, TimeoutOnAccent,
        "ERROR 408", "Сервер долго отвечает",
        "Не дождались ответа сервера. Проверьте соединение и повторите запрос.",
        "Повторить", "Отмена", retry = true,
    )
    AppError.NotFound -> ErrorVisual(
        Icons.Filled.VisibilityOff, NotFoundAccent, NotFoundOnAccent,
        "ERROR 404", "Контент недоступен",
        "Похоже, этот тайтл больше не в каталоге или был перемещён.",
        "В каталог", null, retry = false,
    )
    AppError.Empty -> ErrorVisual(
        Icons.Filled.SearchOff, EmptyAccent, EmptyOnAccent,
        "ПУСТО", "Ничего не найдено",
        "По вашему запросу нет результатов. Измените формулировку или сбросьте фильтры.",
        "Сбросить фильтры", null, retry = false,
    )
    AppError.Premium -> ErrorVisual(
        Icons.Filled.WorkspacePremium, PremiumAccent, PremiumOnAccent,
        "ERROR 402", "Только для Premium",
        "Оформите подписку Filmax Premium, чтобы смотреть в 4K HDR без рекламы.",
        "Оформить Premium", "Позже", retry = false,
    )
    AppError.Region -> ErrorVisual(
        Icons.Filled.Public, RegionAccent, Color.White,
        "ERROR 403", "Недоступно в регионе",
        "Этот контент недоступен в вашей стране из-за лицензионных ограничений.",
        "Понятно", null, retry = false,
    )
    AppError.Auth -> ErrorVisual(
        Icons.Filled.Lock, AuthAccent, Color.White,
        "ERROR 401", "Сессия истекла",
        "Время сессии вышло. Войдите снова, чтобы продолжить просмотр.",
        "Войти заново", "Отмена", retry = false,
    )
    AppError.Playback -> ErrorVisual(
        Icons.Filled.ErrorOutline, ServerAccent, Color.White,
        "PLAYER", "Ошибка воспроизведения",
        "Не удалось загрузить видео. Попробуйте снизить качество или повторить.",
        "Повторить", "Закрыть", retry = true,
    )
}

/**
 * Модальное окно ошибки Filmax — резолвится из [AppError]. Выразительная cookie-иконка,
 * код, заголовок, текст и до двух действий.
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
                .padding(24.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.dp))
                .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            ErrorModalCloseButton(onDismiss)
            ErrorModalContent(visual, onPrimary, onSecondary, onDismiss)
        }
    }
}

/** Кнопка закрытия в правом верхнем углу модалки. */
@Composable
private fun BoxScope.ErrorModalCloseButton(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = "Закрыть",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Содержимое модалки: иконка-cookie, код, заголовок, текст и кнопки действий. */
@Composable
private fun ErrorModalContent(
    visual: ErrorVisual,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(ShapeButton)
                .background(visual.color.copy(alpha = 0.13f))
                .border(1.dp, visual.color.copy(alpha = 0.2f), ShapeButton),
            contentAlignment = Alignment.Center,
        ) {
            Icon(visual.icon, contentDescription = null, tint = visual.color, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(
            visual.code,
            color = visual.color,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            visual.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            visual.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(26.dp))

        ErrorModalPrimaryButton(visual, onPrimary)

        if (visual.secondary != null) {
            Spacer(Modifier.height(10.dp))
            ErrorModalSecondaryButton(visual.secondary, onSecondary ?: onDismiss)
        }
    }
}

/** Основное действие — заполненная акцентом pill-кнопка (с иконкой Refresh при retry). */
@Composable
private fun ErrorModalPrimaryButton(visual: ErrorVisual, onPrimary: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(visual.color)
            .clickable(onClick = onPrimary),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (visual.retry) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = visual.onColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(visual.primary, color = visual.onColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Вторичное действие — текстовая pill-кнопка без заливки. */
@Composable
private fun ErrorModalSecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}
