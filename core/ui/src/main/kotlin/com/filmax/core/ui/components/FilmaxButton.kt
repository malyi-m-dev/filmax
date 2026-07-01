package com.filmax.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Визуальный вариант [FilmaxButton] — повторяет шкалу кнопок Material 3. */
enum class FilmaxButtonVariant { Filled, Tonal, Outlined, Elevated, Text }

/**
 * Кнопка Filmax — единый компонент для всех вариантов кнопок приложения.
 *
 * Минимальное использование: `FilmaxButton("Смотреть", onClick = { … })`.
 * Иконка и вариант — опциональны.
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier — прямой
// параметр, хвост — опции с дефолтами). Обёртка в data-класс сломала бы «минимальный API» и modifier.
@Suppress("LongParameterList")
@Composable
fun FilmaxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FilmaxButtonVariant = FilmaxButtonVariant.Filled,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
    when (variant) {
        FilmaxButtonVariant.Filled ->
            Button(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        FilmaxButtonVariant.Tonal ->
            FilledTonalButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        FilmaxButtonVariant.Outlined ->
            OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        FilmaxButtonVariant.Elevated ->
            ElevatedButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        FilmaxButtonVariant.Text ->
            TextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    }
}
