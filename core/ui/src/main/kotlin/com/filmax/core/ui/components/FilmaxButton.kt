package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.designsystem.glassFill
import com.filmax.core.designsystem.glow

/** Вид кнопки. */
enum class FilmaxButtonStyle {
    /** Акцент палитры со свечением — одна на экран: вторая такая отнимает у первой весь смысл. */
    Primary,

    /** Стекло — остальные действия рядом с главной. */
    Secondary,

    /** Белая — действие на цветной плашке (карточка подписки), где акцент сливается с фоном. */
    Light,

    /** Необратимое действие: «Удалить», «Очистить» — приглушённый красный, а не акцент. */
    Destructive,
}

/** Высота кнопки: [Regular] — главное действие экрана, [Compact] — ряд действий под заголовком. */
enum class FilmaxButtonSize(internal val height: Dp, internal val padding: Dp, internal val icon: Dp) {
    Regular(height = FilmaxMetrics.PrimaryButtonHeight, padding = 22.dp, icon = 22.dp),
    Compact(height = FilmaxMetrics.CompactButtonHeight, padding = 18.dp, icon = 20.dp),
}

/**
 * Кнопка-пилюля Filmax. Ширину задаёт место вызова (`Modifier.weight(1f)`, `fillMaxWidth()`),
 * сама кнопка — по содержимому. Минимально: `FilmaxButton("Смотреть", onClick = …)`.
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier прямым
// параметром, хвост — опции с дефолтами). Обёртка в data-класс сломала бы «минимальный API».
@Suppress("LongParameterList")
@Composable
fun FilmaxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: FilmaxButtonStyle = FilmaxButtonStyle.Primary,
    size: FilmaxButtonSize = FilmaxButtonSize.Regular,
    enabled: Boolean = true,
) {
    val palette = FilmaxTheme.palette
    val content = when (style) {
        FilmaxButtonStyle.Primary -> palette.onAccent
        FilmaxButtonStyle.Secondary -> FilmaxColors.Text
        FilmaxButtonStyle.Light -> palette.background
        FilmaxButtonStyle.Destructive -> palette.background
    }
    Row(
        modifier
            .height(size.height)
            .buttonSurface(style, enabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = size.padding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(size.icon)) }
        Text(
            text,
            style = if (size == FilmaxButtonSize.Regular) FilmaxType.button else FilmaxType.chip,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Главная кнопка деталей: «Смотреть» / «Продолжить» с подписью «2 ч 11 мин · 4K» и полоской
 * [progress] досмотренного по низу — так кнопка сама говорит, с какого места продолжит.
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier прямым
// параметром, хвост — опции с дефолтами).
@Suppress("LongParameterList")
@Composable
fun FilmaxPlayButton(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val palette = FilmaxTheme.palette
    Box(
        modifier
            .height(FilmaxMetrics.PrimaryButtonHeight)
            .buttonSurface(FilmaxButtonStyle.Primary, enabled = true)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxHeight().align(Alignment.Center).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(PlayIconSize))
            Column {
                Text(title, style = FilmaxType.button, color = palette.onAccent, maxLines = 1)
                subtitle?.let {
                    Text(
                        it,
                        style = FilmaxType.metaSmall,
                        color = palette.onAccent.copy(alpha = SUBTITLE_ALPHA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        progress?.let {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(it.coerceIn(0f, 1f))
                    .height(FilmaxMetrics.ProgressBarThin)
                    .background(Color.White.copy(alpha = PROGRESS_ALPHA)),
            )
        }
    }
}

/**
 * Текстовая кнопка-ссылка акцентом: «Очистить» над историей, «Читать полностью ›» под описанием.
 * Без подложки — действие второго плана не спорит с кнопками, но тап-цель остаётся ≥ 40dp.
 */
@Composable
fun FilmaxTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = FilmaxMetrics.IconButtonSmall)
            .clip(ShapeFull)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = FilmaxType.chip, color = FilmaxTheme.palette.accentText, maxLines = 1)
    }
}

/** Подложка кнопки по стилю; главная ещё и светится цветом палитры. */
@Composable
private fun Modifier.buttonSurface(style: FilmaxButtonStyle, enabled: Boolean): Modifier {
    val palette = FilmaxTheme.palette
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    return when (style) {
        FilmaxButtonStyle.Primary ->
            this
                .then(if (enabled) Modifier.glow(palette.glow, ShapeFull) else Modifier)
                .clip(ShapeFull)
                .background(palette.accent.copy(alpha = alpha))
        FilmaxButtonStyle.Secondary -> glassFill(ShapeFull)
        FilmaxButtonStyle.Light -> clip(ShapeFull).background(Color.White.copy(alpha = alpha))
        FilmaxButtonStyle.Destructive -> clip(ShapeFull).background(FilmaxColors.Error.copy(alpha = alpha))
    }
}

private val PlayIconSize = 26.dp
private const val SUBTITLE_ALPHA = 0.82f
private const val PROGRESS_ALPHA = 0.75f
private const val DISABLED_ALPHA = 0.4f
