package com.filmax.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassPanel
import com.filmax.core.designsystem.GlassState
import com.filmax.core.designsystem.ShapeFull

/**
 * Поле поиска: стеклянная пилюля, в фокусе — обводка акцентом. Слева «назад» ([onBack]) или лупа,
 * справа очистка непустого запроса и круглая кнопка голоса ([onVoice]).
 *
 * Минимально: `FilmaxSearchField(query, onQueryChange = { … })`.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Фильмы, сериалы, актёры",
    glass: GlassState? = null,
    onBack: (() -> Unit)? = null,
    onVoice: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val palette = FilmaxTheme.palette
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    GlassPanel(
        state = glass,
        shape = ShapeFull,
        border = if (focused) palette.accent else FilmaxColors.PanelBorder,
        modifier = modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.SearchFieldHeight)
            .then(if (focused) Modifier.border(2.dp, palette.accent, ShapeFull) else Modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchLeading(onBack)
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(placeholder, style = FilmaxType.field, color = FilmaxColors.TextDim, maxLines = 1)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = FilmaxType.field.copy(color = FilmaxColors.Text),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                    interactionSource = interaction,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            SearchTrailing(query = query, onClear = { onQueryChange("") }, onVoice = onVoice)
        }
    }
}

/** Слева в поле: «назад», если поиск открыт поверх экрана, иначе лупа. */
@Composable
private fun SearchLeading(onBack: (() -> Unit)?) {
    if (onBack != null) {
        FilmaxIconButton(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            onClick = onBack,
            style = FilmaxIconButtonStyle.Plain,
        )
    } else {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = FilmaxColors.TextSecondary,
            modifier = Modifier.padding(start = 12.dp, end = 6.dp),
        )
    }
}

/** Справа в поле: очистка непустого запроса и круглая кнопка голоса. */
@Composable
private fun SearchTrailing(query: String, onClear: () -> Unit, onVoice: (() -> Unit)?) {
    if (query.isNotEmpty()) {
        FilmaxIconButton(
            Icons.Filled.Close,
            contentDescription = "Очистить",
            onClick = onClear,
            style = FilmaxIconButtonStyle.Plain,
            tint = FilmaxColors.TextDim,
        )
    }
    onVoice?.let {
        FilmaxIconButton(
            Icons.Filled.Mic,
            contentDescription = "Голосовой поиск",
            onClick = it,
            style = FilmaxIconButtonStyle.Accent
        )
    }
}
