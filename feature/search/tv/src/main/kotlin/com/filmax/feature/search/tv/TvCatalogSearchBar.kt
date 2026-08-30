// Строка поиска каталога: кнопка в навигации, поле ввода с системной клавиатурой по «ОК»
// и кнопка голосового поиска рядом.
package com.filmax.feature.search.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import kotlinx.coroutines.flow.drop

/**
 * Строка поиска: поле ввода и кнопка голоса.
 *
 * Поле настоящее — «ОК» на нём открывает системную клавиатуру телевизора. Своей мы больше не
 * держим: платформенная знает раскладки зрителя, помнит, что он вводил, и умеет голос сама.
 * Рамка фокуса рисуется здесь, а не через `TvFocusCard`: у карточки фокус живёт на ней самой,
 * а поле должно получать его себе — иначе клавиатуре некуда печатать.
 */
@Composable
internal fun CatalogSearchBar(
    query: String,
    onQuery: (String) -> Unit,
    onVoice: () -> Unit,
    onEditingFinished: () -> Unit,
    modifier: Modifier,
) {
    // Две роли одной строки. В навигации это кнопка: пульт ходит по экрану, стрелки достаются
    // фокусу, клавиатура не всплывает. По «ОК» строка становится полем ввода и зовёт системную
    // клавиатуру телевизора. Иначе никак: поле, получив фокус, показывает клавиатуру само — на
    // пульте она вылезала на пол-экрана каждый раз, когда фокус просто проходил мимо строки.
    var editing by rememberSaveable { mutableStateOf(false) }

    Row(
        // Строка поиска — не чип-ряд: остаётся в safe-области собственным отступом.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val barModifier = modifier.weight(1f).height(SearchBarHeight)
        if (editing) {
            SearchInput(
                query = query,
                onQuery = onQuery,
                onDone = {
                    editing = false
                    onEditingFinished()
                },
                modifier = barModifier,
            )
        } else {
            SearchButton(query = query, onClick = { editing = true }, modifier = barModifier)
        }
        VoiceSearchButton(onVoice)
    }
}

/** Строка в состоянии навигации: показывает запрос и по «ОК» уступает место полю ввода. */
@Composable
private fun SearchButton(query: String, onClick: () -> Unit, modifier: Modifier) {
    TvFocusCard(onClick = onClick, shape = TvMetrics.PanelShape, modifier = modifier) {
        SearchBarSurface {
            Text(
                text = query.ifEmpty { "Название фильма или сериала" },
                style = MaterialTheme.typography.titleMedium,
                color = if (query.isEmpty()) TvOnSurfaceDim else TvOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Строка в состоянии ввода: настоящее поле, за которым открывается системная клавиатура —
 * та же, к которой зритель привык в остальных приложениях, с его раскладками и историей.
 *
 * Выход из ввода — «Поиск» на клавиатуре, «Назад» или уход фокуса: строка возвращается
 * в состояние кнопки, и пульт снова ходит по экрану.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchInput(
    query: String,
    onQuery: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    val fieldState = rememberTextFieldState(query)
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Первое событие фокуса приходит ещё до запроса — «поле не в фокусе», и без этой отметки
    // строка откатывалась в кнопку в том же кадре, в котором открылась.
    var hadFocus by remember { mutableStateOf(false) }

    // Набранное уходит в модель (там debounce). Обратно текст не возвращаем: пока строка в
    // режиме ввода, источник правды — она сама, а голосовой запрос приходит уже в состоянии
    // кнопки, где текст берётся прямо из модели.
    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }.drop(1).collect(onQuery)
    }
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }

    // Клавиатуру закрывают «Назад» — и это событие достаётся ей, а не нам. Единственный
    // надёжный признак конца ввода поэтому такой: клавиатура была на экране и ушла.
    val keyboardVisible = WindowInsets.isImeVisible
    var keyboardWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) keyboardWasVisible = true else if (keyboardWasVisible) onDone()
    }

    SearchBarSurface(modifier = modifier, focused = true) {
        BasicTextField(
            state = fieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = TvOnSurface),
            cursorBrush = SolidColor(TvOnSurface),
            // «Поиск» вместо перевода строки: выдача под клавиатурой уже готова — запрос ушёл
            // в модель с первой буквы, и подтверждать нечего, кроме конца ввода.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = {
                keyboard?.hide()
                onDone()
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(fieldFocus)
                .onFocusChanged {
                    if (it.isFocused) hadFocus = true else if (hadFocus) onDone()
                },
        )
    }
}

/** Общая поверхность строки поиска: подложка, иконка и рамка фокуса — одна на оба состояния. */
@Composable
private fun SearchBarSurface(
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(TvMetrics.PanelShape)
            .background(TvSurfaceContainer)
            .border(
                width = if (focused) TvMetrics.FocusBorderWidth else 0.dp,
                color = if (focused) TvOnSurface else Color.Transparent,
                shape = TvMetrics.PanelShape,
            )
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = TvOnSurfaceDim,
                modifier = Modifier.size(20.dp),
            )
            content()
        },
    )
}

/** Кнопка голосового ввода рядом со строкой: на пульте это самый быстрый способ искать. */
@Composable
private fun VoiceSearchButton(onVoice: () -> Unit) {
    TvFocusCard(
        onClick = onVoice,
        shape = TvMetrics.PanelShape,
        modifier = Modifier.size(56.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(TvMetrics.PanelShape)
                .background(TvSurfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "Голосовой поиск",
                tint = TvOnSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
