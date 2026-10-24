package com.filmax.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeTile
import com.filmax.core.designsystem.glassFill

private val RowHorizontalPadding = 18.dp

/**
 * Строка списка на стекле: иконка, заголовок и подпись. Пока по строке идёт действие ([busy] —
 * подключение, загрузка), справа крутится индикатор, а повторный тап ничего не делает.
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier прямым
// параметром, хвост — опции с дефолтами). Обёртка в data-класс сломала бы «минимальный API».
@Suppress("LongParameterList")
@Composable
fun FilmaxListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    busy: Boolean = false,
) {
    val palette = FilmaxTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FilmaxMetrics.ListRowHeight)
            .glassFill(ShapeTile)
            .clickable(enabled = !busy, onClick = onClick)
            .padding(horizontal = RowHorizontalPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(24.dp)) }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = FilmaxType.itemTitle,
                color = FilmaxColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    it,
                    style = FilmaxType.meta,
                    color = FilmaxColors.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (busy) {
            CircularProgressIndicator(color = palette.accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        }
    }
}
