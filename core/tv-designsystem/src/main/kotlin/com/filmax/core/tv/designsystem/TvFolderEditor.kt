// Содержимое боковой панели «Новая папка» / «Переименовать»: поле названия со счётчиком, выбор
// цвета, экранная клавиатура и кнопки. Набранное и выбранное — в ScreenModel; панель рисует.
// focusProperties.enter — вход в редактор сразу на клавиатуру; API помечен экспериментальным.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

private val FieldHeight = 52.dp
private val FieldShape = RoundedCornerShape(14.dp)
private val FieldTileSize = 28.dp
private val FieldTileShape = RoundedCornerShape(8.dp)
private val SwatchSize = 30.dp
private val CaretWidth = 2.dp
private val CaretHeight = 22.dp
private val FieldText = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

/** Поле — стекло 8%: заметнее панели, но тише клавиш. */
private const val FIELD_ALPHA = 0.08f
private const val COUNTER_ALPHA = 0.45f

/** Кнопка подтверждения без названия — притушена: нажатие ничего не сделает. */
private const val DISABLED_ALPHA = 0.5f

/**
 * Что рисует редактор папки. [tintIndex] — номер цвета в [TvFolderTints]; [layouts] — подписи
 * раскладок клавиатуры, [layoutIndex] — текущая; [canConfirm] — название непустое.
 */
@Immutable
data class TvFolderEditorState(
    val heading: String,
    val title: String,
    val maxLength: Int,
    val tintIndex: Int,
    val keyboardRows: List<List<String>>,
    val layouts: List<String>,
    val layoutIndex: Int,
    val confirmLabel: String,
    val canConfirm: Boolean,
)

/** Нажатия редактора: клавиши, цвет, раскладка, «Создать», «Отмена». */
data class TvFolderEditorActions(
    val keyboard: TvKeyboardActions,
    val onTint: (Int) -> Unit,
    val onLayout: (Int) -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * Редактор папки внутри [TvSheetHost]. Фокус при появлении — на первой клавише: название
 * набирают раньше, чем выбирают цвет. Вход в редактор (панель забирает фокус в себя) ведёт туда
 * же. «Назад» закрывает редактор — его ловит хост панели.
 */
@Composable
fun BoxScope.TvFolderEditor(state: TvFolderEditorState, actions: TvFolderEditorActions) {
    val firstKey = remember { FocusRequester() }
    var focusPlaced by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .focusProperties { enter = { firstKey } }
            .focusGroup()
            .padding(start = 34.dp, end = 34.dp, top = 28.dp)
            // Клавиатура размещена — ставим на неё фокус один раз: и при открытии панели, и при
            // переходе к редактору из списка папок, где фокус стоял на ушедшей строке.
            .onPlaced {
                if (!focusPlaced) focusPlaced = firstKey.requestFocus()
            },
    ) {
        EditorHeading(state.heading)
        TitleField(state, Modifier.padding(top = 16.dp))
        TintAndLayoutRow(state, actions, Modifier.padding(top = 14.dp))
        TvKeyboard(
            rows = state.keyboardRows,
            actions = actions.keyboard,
            firstKey = firstKey,
            modifier = Modifier.padding(top = 16.dp),
        )
        EditorButtons(state, actions, Modifier.padding(top = 18.dp))
    }
}

@Composable
private fun EditorHeading(heading: String) {
    val colors = LocalFxColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BackKeyCap()
        Text(heading.uppercase(), style = FxType.overline, color = colors.accentText)
    }
}

/** Значок клавиши «Назад» перед заголовком: «назад» здесь — к списку или из панели. */
@Composable
private fun BackKeyCap() {
    Box(
        Modifier
            .size(KeyCapSize)
            .background(Color.White.copy(alpha = KEYCAP_FILL_ALPHA), KeyCapShape)
            .border(1.dp, Color.White.copy(alpha = KEYCAP_BORDER_ALPHA), KeyCapShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = null,
            tint = LocalFxColors.current.text,
            modifier = Modifier.size(16.dp),
        )
    }
}

private val KeyCapSize = 22.dp
private val KeyCapShape = RoundedCornerShape(6.dp)
private const val KEYCAP_FILL_ALPHA = 0.12f
private const val KEYCAP_BORDER_ALPHA = 0.22f

@Composable
private fun TitleField(state: TvFolderEditorState, modifier: Modifier) {
    val colors = LocalFxColors.current
    Row(
        modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .background(Color.White.copy(alpha = FIELD_ALPHA), FieldShape)
            .border(1.dp, colors.accent, FieldShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(FieldTileSize).background(tvFolderTint(state.tintIndex).swatch, FieldTileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        FieldText(state.title, Modifier.weight(1f, fill = false))
        Box(Modifier.width(CaretWidth).height(CaretHeight).background(colors.accent))
        Spacer(Modifier.weight(1f))
        Text(
            "${state.title.length} / ${state.maxLength}",
            style = FxType.metaOnImage,
            color = colors.text.copy(alpha = COUNTER_ALPHA),
        )
    }
}

/** Набранное название; пока пусто — подсказка приглушённым цветом. */
@Composable
private fun FieldText(title: String, modifier: Modifier) {
    val colors = LocalFxColors.current
    Text(
        title.ifEmpty { "Название папки" },
        style = FieldText,
        color = if (title.isEmpty()) colors.textDim else colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** «ЦВЕТ» и шесть кружков слева, переключатель раскладок справа — одним рядом, чтобы влезла клавиатура. */
@Composable
private fun TintAndLayoutRow(state: TvFolderEditorState, actions: TvFolderEditorActions, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("ЦВЕТ", style = FxType.overline, color = LocalFxColors.current.textDim)
        Row(
            Modifier.padding(start = 14.dp).tvFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TvFolderTints.forEachIndexed { index, tint ->
                Swatch(tint.swatch, selected = index == state.tintIndex, onClick = { actions.onTint(index) })
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.tvFocusGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.layouts.forEachIndexed { index, label ->
                TvChip(
                    label = label,
                    selected = index == state.layoutIndex,
                    onClick = { actions.onLayout(index) },
                    style = TvChipStyle.Compact,
                )
            }
        }
    }
}

/** Кружок цвета; выбранный — с галочкой и белым кольцом через зазор цвета фона. */
@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = Modifier
            .size(SwatchSize)
            .then(if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier),
        style = TvFocusStyle.Circle,
        colors = TvSurfaceColors(container = color, content = Color.White),
    ) {
        if (selected) {
            Box(Modifier.align(Alignment.Center).size(SwatchSize - 6.dp).border(2.dp, colors.background, CircleShape))
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Выбран",
                modifier = Modifier.align(Alignment.Center).size(16.dp),
            )
        }
    }
}

@Composable
private fun EditorButtons(state: TvFolderEditorState, actions: TvFolderEditorActions, modifier: Modifier) {
    Row(modifier.tvFocusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvButton(
            text = state.confirmLabel,
            onClick = actions.onConfirm,
            leadingIcon = Icons.Rounded.Check,
            modifier = Modifier.alpha(if (state.canConfirm) 1f else DISABLED_ALPHA),
        )
        TvButton(text = "Отмена", onClick = actions.onCancel, primary = false)
    }
}
