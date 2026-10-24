package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType

private val FieldShape = RoundedCornerShape(16.dp)

/**
 * Однострочное поле ввода: адрес телевизора, название папки. [label] — подпись над полем акцентом,
 * [maxLength] — счётчик «16/40» справа (обрезает сам ввод). [isError] обводит поле цветом ошибки —
 * объяснение пишет экран рядом, цвет его лишь дублирует. «Готово» на клавиатуре — [onDone].
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier прямым
// параметром, хвост — опции с дефолтами). Обёртка в data-класс сломала бы «минимальный API».
@Suppress("LongParameterList")
@Composable
fun FilmaxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    maxLength: Int? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val palette = FilmaxTheme.palette
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ring = when {
        isError -> FilmaxColors.Error
        focused -> palette.accent
        else -> null
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        label?.let { Text(it, style = FilmaxType.fieldLabel, color = palette.accentText) }
        Row(
            Modifier
                .fillMaxWidth()
                .height(FilmaxMetrics.SearchFieldHeight)
                .clip(FieldShape)
                .background(FilmaxColors.Field)
                .then(if (ring != null) Modifier.border(2.dp, ring, FieldShape) else Modifier)
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, style = FilmaxType.field, color = FilmaxColors.TextDim, maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = { input -> onValueChange(maxLength?.let { input.take(it) } ?: input) },
                    singleLine = true,
                    textStyle = FilmaxType.field.copy(color = FilmaxColors.Text),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    interactionSource = interaction,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            maxLength?.let {
                Text("${value.length}/$it", style = FilmaxType.metaSmall, color = FilmaxColors.TextDim)
            }
        }
    }
}

/** Многострочное поле — отзыв, комментарий. Растёт с текстом от [minLines] строк. */
@Composable
fun FilmaxTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
) {
    val palette = FilmaxTheme.palette
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(FilmaxColors.Field)
            .then(if (focused) Modifier.border(2.dp, palette.accent, FieldShape) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, style = FilmaxType.body, color = FilmaxColors.TextDim)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            minLines = minLines,
            textStyle = FilmaxType.body.copy(color = FilmaxColors.Text),
            cursorBrush = SolidColor(palette.accent),
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
