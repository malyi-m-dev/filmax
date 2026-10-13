// Выбор значения в боковой панели (TvSheetHost): надзаголовок, название настройки и варианты
// списком; выбранный — с галочкой. Фокус при открытии — на выбранном варианте.
// focusProperties.enter — вход в список сразу на выбранный вариант; API помечен экспериментальным.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

private val OptionHeight = 52.dp
private val SwatchSize = 22.dp
private val SheetTitle = FxType.screenTitle.copy(fontSize = 24.sp, lineHeight = 28.sp)
private const val OPTION_ALPHA = 0.05f
private const val OPTION_FOCUSED_ALPHA = 0.14f

/** Вариант значения; [swatch] — кружок цвета перед подписью (палитры), null — без него. */
@Immutable
data class TvOption(val label: String, val swatch: Color? = null)

/** Что показывает панель: надзаголовок (раздел), название настройки, варианты и выбранный. */
@Immutable
data class TvOptionSheetState(
    val overline: String,
    val title: String,
    val options: List<TvOption>,
    val selectedIndex: Int,
)

/**
 * Панель выбора значения. «ОК» на варианте зовёт [onSelect] с его номером — применить и закрыть
 * решает экран. Фокус при появлении — на выбранном варианте: зритель видит текущее значение и
 * меняет его одним шагом.
 */
@Composable
fun BoxScope.TvOptionSheet(state: TvOptionSheetState, onSelect: (Int) -> Unit) {
    val colors = LocalFxColors.current
    val selectedFocus = remember { FocusRequester() }
    var focusPlaced by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(start = 26.dp, end = 26.dp, top = 28.dp)) {
        Text(
            state.overline.uppercase(),
            style = FxType.overline,
            color = colors.accentText,
            modifier = Modifier.padding(start = 8.dp),
        )
        Text(
            state.title,
            style = SheetTitle,
            color = colors.text,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        )
        Column(
            Modifier
                .padding(top = 18.dp)
                .focusProperties { enter = { selectedFocus } }
                .focusGroup()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.options.forEachIndexed { index, option ->
                val selected = index == state.selectedIndex
                OptionRow(
                    option = option,
                    selected = selected,
                    onClick = { onSelect(index) },
                    modifier = if (selected) {
                        Modifier
                            .focusRequester(selectedFocus)
                            .onPlaced { if (!focusPlaced) focusPlaced = selectedFocus.requestFocus() }
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
    TvKeyHints(
        hints = listOf(
            TvKeyHint(listOf(Icons.Rounded.KeyboardArrowUp, Icons.Rounded.KeyboardArrowDown), "варианты"),
            TvKeyHint(listOf(Icons.AutoMirrored.Rounded.ArrowBack), "закрыть"),
        ),
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 34.dp, bottom = 22.dp),
    )
}

@Composable
private fun OptionRow(option: TvOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(OptionHeight),
        style = TvFocusStyle.Row,
        colors = TvSurfaceColors(
            container = Color.White.copy(alpha = OPTION_ALPHA),
            content = if (selected) colors.accentText else colors.text,
            focusedContainer = Color.White.copy(alpha = OPTION_FOCUSED_ALPHA),
        ),
    ) {
        Row(
            Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            option.swatch?.let { swatch -> Box(Modifier.size(SwatchSize).background(swatch, CircleShape)) }
            Text(option.label, style = FxType.button, maxLines = 1)
            Spacer(Modifier.weight(1f))
            if (selected) Icon(Icons.Rounded.Check, contentDescription = "Выбрано", modifier = Modifier.size(20.dp))
        }
    }
}
