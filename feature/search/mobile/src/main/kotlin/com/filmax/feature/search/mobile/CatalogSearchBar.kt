// Строка поиска каталога в двух состояниях: на витрине это кнопка, в режиме поиска — поле
// ввода с кнопкой «назад», очисткой и голосовым поиском. Выдача режима поиска — в CatalogScreen.

package com.filmax.feature.search.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.ui.components.VoiceListeningDialog
import com.filmax.core.ui.components.rememberInAppVoiceSearch

private const val SEARCH_PLACEHOLDER = "Поиск фильмов, сериалов, людей"

/** Строка поиска каталога. Это кнопка, а не поле: текст набирают в режиме поиска. */
@Composable
internal fun CatalogSearchButton(query: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.SearchFieldHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = FilmaxOnSurfaceDim,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = query.ifEmpty { SEARCH_PLACEHOLDER },
            style = MaterialTheme.typography.bodyLarge,
            color = if (query.isEmpty()) FilmaxOnSurfaceDim else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SearchHeader(query: String, actions: SearchActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = FilmaxMetrics.ScreenPadding, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = actions.onClose, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        SearchInput(query = query, actions = actions, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SearchInput(query: String, actions: SearchActions, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    // Голос слушаем внутри приложения (SpeechRecognizer), без стороннего экрана распознавания.
    val voiceSearch = rememberInAppVoiceSearch(onResult = actions.onSubmitQuery)
    VoiceListeningDialog(voiceSearch)

    // Автофокус: в режим поиска входят, чтобы набирать, — клавиатуру система поднимет сама.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = modifier
            .height(46.dp)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.5.dp, MaterialTheme.colorScheme.surfaceContainerHighest, ShapeButton)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = FilmaxOnSurfaceDim,
            modifier = Modifier.size(18.dp),
        )
        SearchTextField(
            query = query,
            onQueryChange = actions.onQueryChange,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )
        SearchInputTrailing(
            query = query,
            onClear = { actions.onQueryChange("") },
            onVoiceSearch = voiceSearch::start,
        )
    }
}

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    // TextFieldValue, а не голая строка: запрос переживает закрытие режима поиска, и без явной
    // позиции курсор при возврате встал бы перед уже набранным текстом. Голос пишет в запрос
    // мимо поля — отсюда синхронизация по [query].
    var fieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(query) {
        if (query != fieldValue.text) fieldValue = TextFieldValue(query, TextRange(query.length))
    }

    BasicTextField(
        value = fieldValue,
        onValueChange = { value ->
            fieldValue = value
            onQueryChange(value.text)
        },
        modifier = modifier.focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // Выдача и так живёт по debounce — «Найти» остаётся убрать клавиатуру с результатов.
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { inner ->
            if (fieldValue.text.isEmpty()) {
                Text(
                    SEARCH_PLACEHOLDER,
                    style = MaterialTheme.typography.titleMedium,
                    color = FilmaxOnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            inner()
        },
    )
}

@Composable
private fun SearchInputTrailing(query: String, onClear: () -> Unit, onVoiceSearch: () -> Unit) {
    if (query.isEmpty()) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = "Голосовой поиск",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).clickable(onClick = onVoiceSearch),
        )
    } else {
        Icon(
            Icons.Filled.Close,
            contentDescription = "Очистить",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp).clickable(onClick = onClear),
        )
    }
}
