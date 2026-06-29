package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.domain.error.AppError

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
        Icons.Filled.WifiOff, Color(0xFFF4B792), Color(0xFF4F2500),
        "NET · НЕТ СЕТИ", "Нет подключения",
        "Проверьте интернет-соединение и попробуйте снова.",
        "Повторить", null, retry = true,
    )
    AppError.Server -> ErrorVisual(
        Icons.Filled.CloudOff, Color(0xFFB4305A), Color.White,
        "ERROR 500", "Что-то пошло не так",
        "На нашей стороне сбой. Мы уже разбираемся — попробуйте через минуту.",
        "Повторить", "Назад", retry = true,
    )
    AppError.Timeout -> ErrorVisual(
        Icons.Filled.Schedule, Color(0xFFE8A43A), Color(0xFF3A2705),
        "ERROR 408", "Сервер долго отвечает",
        "Не дождались ответа сервера. Проверьте соединение и повторите запрос.",
        "Повторить", "Отмена", retry = true,
    )
    AppError.NotFound -> ErrorVisual(
        Icons.Filled.VisibilityOff, Color(0xFF9E8B91), Color(0xFF1A1012),
        "ERROR 404", "Контент недоступен",
        "Похоже, этот тайтл больше не в каталоге или был перемещён.",
        "В каталог", null, retry = false,
    )
    AppError.Empty -> ErrorVisual(
        Icons.Filled.SearchOff, Color(0xFF6AC2B0), Color(0xFF06251D),
        "ПУСТО", "Ничего не найдено",
        "По вашему запросу нет результатов. Измените формулировку или сбросьте фильтры.",
        "Сбросить фильтры", null, retry = false,
    )
    AppError.Premium -> ErrorVisual(
        Icons.Filled.WorkspacePremium, Color(0xFFD4A84A), Color(0xFF3A2C05),
        "ERROR 402", "Только для Premium",
        "Оформите подписку Filmax Premium, чтобы смотреть в 4K HDR без рекламы.",
        "Оформить Premium", "Позже", retry = false,
    )
    AppError.Region -> ErrorVisual(
        Icons.Filled.Public, Color(0xFF6B4B8F), Color.White,
        "ERROR 403", "Недоступно в регионе",
        "Этот контент недоступен в вашей стране из-за лицензионных ограничений.",
        "Понятно", null, retry = false,
    )
    AppError.Auth -> ErrorVisual(
        Icons.Filled.Lock, Color(0xFFE46962), Color.White,
        "ERROR 401", "Сессия истекла",
        "Время сессии вышло. Войдите снова, чтобы продолжить просмотр.",
        "Войти заново", "Отмена", retry = false,
    )
    AppError.Playback -> ErrorVisual(
        Icons.Filled.ErrorOutline, Color(0xFFB4305A), Color.White,
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
    val v = visualFor(error)
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
            // Кнопка закрытия
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(ShapeCookie)
                        .background(v.color.copy(alpha = 0.13f))
                        .border(1.dp, v.color.copy(alpha = 0.2f), ShapeCookie),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(v.icon, contentDescription = null, tint = v.color, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    v.code,
                    color = v.color,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    v.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    v.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(26.dp))

                // Primary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(v.color)
                        .clickable(onClick = onPrimary),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (v.retry) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = v.onColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(v.primary, color = v.onColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Secondary
                if (v.secondary != null) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSecondary ?: onDismiss,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            v.secondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}
