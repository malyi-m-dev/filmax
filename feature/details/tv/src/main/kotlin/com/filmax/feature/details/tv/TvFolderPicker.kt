// Панель «В папку» в деталях (экраны 02c, 02d): список папок с отметками и, внутри той же
// панели, создание папки с цветом и экранной клавиатурой. Состояние — FolderPickerScreenModel.
// focusProperties.enter — вход в список сразу на нужную строку; API помечен экспериментальным.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.filmax.feature.details.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.user.model.FOLDER_TITLE_MAX_LENGTH
import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.core.presentation.input.KeyboardLayout
import com.filmax.core.presentation.input.keyboardRows
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvAddRow
import com.filmax.core.tv.designsystem.TvCheckRow
import com.filmax.core.tv.designsystem.TvCheckRowSpec
import com.filmax.core.tv.designsystem.TvFolderEditor
import com.filmax.core.tv.designsystem.TvFolderEditorActions
import com.filmax.core.tv.designsystem.TvFolderEditorState
import com.filmax.core.tv.designsystem.TvKeyHint
import com.filmax.core.tv.designsystem.TvKeyHints
import com.filmax.core.tv.designsystem.TvKeyboardActions
import com.filmax.core.tv.designsystem.tvFolderTint
import com.filmax.core.ui.components.titlesLabel
import com.filmax.feature.details.common.FolderPickerEvent
import com.filmax.feature.details.common.FolderPickerState

/** Ключ строки «Создать папку» — цель фокуса, когда все папки уже с тайтлом. */
private const val ADD_KEY = -1

/** Смена списка на редактор: сдвиг 24dp и проявление — как в макете. */
private val EditorShift = 24.dp
private val PickerTitle = FxType.screenTitle.copy(fontSize = 24.sp, lineHeight = 28.sp)

/**
 * Панель «В папку» глазами экрана деталей: состояние модели, её события и фокус кнопки
 * «В папку» — на неё он вернётся, когда панель закроется.
 */
internal data class DetailsFolders(
    val state: FolderPickerState,
    val onEvent: (FolderPickerEvent) -> Unit,
    val buttonFocus: FocusRequester,
) {
    companion object {
        /** Панель не подключена — UI-тесты экрана, которым она не нужна. */
        val Unused = DetailsFolders(FolderPickerState(), onEvent = {}, buttonFocus = FocusRequester())
    }
}

/** Отметка системной «Буду смотреть» и её переключение — они живут в модели деталей. */
internal data class WatchlistMark(val checked: Boolean, val onToggle: () -> Unit)

@Composable
internal fun BoxScope.FolderPickerSheet(item: Item, folders: DetailsFolders, watchlist: WatchlistMark) {
    val draft = folders.state.draft
    // Внутри редактора «Назад» — к списку, а не из панели: обработчик зарегистрирован позже
    // хоста панели и потому срабатывает раньше него.
    BackHandler(enabled = draft != null) { folders.onEvent(FolderPickerEvent.CancelCreate) }
    val shift = with(LocalDensity.current) { EditorShift.roundToPx() }
    AnimatedContent(
        targetState = draft,
        contentKey = { it != null },
        transitionSpec = {
            (fadeIn(FxMotion.fade()) + slideInHorizontally(FxMotion.panelIn()) { shift }) togetherWith
                fadeOut(FxMotion.fade())
        },
        label = "folderPicker",
        modifier = Modifier.matchParentSize(),
    ) { shown ->
        Box(Modifier.fillMaxSize()) {
            if (shown != null) {
                TvFolderEditor(shown.toEditorState(), editorActions(folders.onEvent))
            } else {
                FolderList(item, folders, watchlist)
            }
        }
    }
}

/**
 * Список: системная «Буду смотреть», папки пользователя (свежие первыми) и «Создать папку».
 * Фокус при появлении — на первой папке, где тайтла ещё нет, после создания — на новой папке.
 */
@Composable
private fun BoxScope.FolderList(item: Item, folders: DetailsFolders, watchlist: WatchlistMark) {
    val state = folders.state
    val colors = LocalFxColors.current
    Column(Modifier.fillMaxSize().padding(start = 26.dp, end = 26.dp, top = 28.dp, bottom = 56.dp)) {
        Text(
            "ДОБАВИТЬ В ПАПКУ",
            style = FxType.overline,
            color = colors.accentText,
            modifier = Modifier.padding(start = 8.dp)
        )
        Text(
            item.title,
            style = PickerTitle,
            color = colors.text,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
        )
        if (state.loading) {
            CircularProgressIndicator(color = colors.accent, modifier = Modifier.padding(start = 8.dp, top = 24.dp))
        } else {
            FolderRows(state, folders.onEvent, watchlist, Modifier.padding(top = 10.dp).weight(1f))
        }
        state.error?.let { message ->
            Text(
                message,
                style = FxType.meta,
                color = colors.error,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
        }
    }
    PickerHints(Modifier.align(Alignment.BottomStart).padding(start = 34.dp, bottom = 22.dp))
}

@Composable
private fun FolderRows(
    state: FolderPickerState,
    onEvent: (FolderPickerEvent) -> Unit,
    watchlist: WatchlistMark,
    modifier: Modifier,
) {
    val requesters = remember { mutableMapOf<Int, FocusRequester>() }
    val target = state.focusFolderId?.takeIf { id -> state.folders.any { it.id == id } } ?: ADD_KEY
    val targetFocus = requesters.getOrPut(target) { FocusRequester() }
    var focusPlaced by remember { mutableStateOf(false) }
    // Строка-цель разместилась — фокус на неё, один раз: дальше пульт ведёт сам.
    val placeFocus = Modifier.onPlaced { if (!focusPlaced) focusPlaced = targetFocus.requestFocus() }
    LazyColumn(
        modifier = modifier.focusProperties { enter = { targetFocus } }.focusGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        // Поля под увеличение строки в фокусе: список клипает края, а строка растёт на 2%.
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        item(key = "watchlist") { WatchlistRow(state.watchlistCount, watchlist) }
        folderRows(state, onEvent) { folderId ->
            val requester = requesters.getOrPut(folderId) { FocusRequester() }
            Modifier.focusRequester(requester).then(if (folderId == target) placeFocus else Modifier)
        }
        item(key = "add") {
            TvAddRow(
                label = "Создать папку",
                onClick = { onEvent(FolderPickerEvent.StartCreate) },
                modifier = Modifier
                    .focusRequester(requesters.getOrPut(ADD_KEY) { FocusRequester() })
                    .then(if (target == ADD_KEY) placeFocus else Modifier),
            )
        }
    }
}

@Composable
private fun WatchlistRow(count: Int, watchlist: WatchlistMark) {
    TvCheckRow(
        spec = TvCheckRowSpec(
            title = "Буду смотреть",
            subtitle = "системная · ${titlesLabel(count)}",
            icon = Icons.Rounded.Bookmark,
            tile = LocalFxColors.current.accent,
        ),
        checked = watchlist.checked,
        onClick = watchlist.onToggle,
    )
}

private fun LazyListScope.folderRows(
    state: FolderPickerState,
    onEvent: (FolderPickerEvent) -> Unit,
    rowModifier: (Int) -> Modifier,
) {
    items(state.folders, key = { it.id }) { folder ->
        TvCheckRow(
            spec = folder.toRowSpec(),
            checked = folder.id in state.memberOf,
            onClick = { onEvent(FolderPickerEvent.Toggle(folder.id)) },
            modifier = rowModifier(folder.id),
        )
    }
}

private fun UserFolder.toRowSpec() = TvCheckRowSpec(
    title = title,
    subtitle = titlesLabel(count),
    icon = Icons.Rounded.Folder,
    tile = tvFolderTint(tint).tile,
)

@Composable
private fun PickerHints(modifier: Modifier) {
    TvKeyHints(
        hints = listOf(
            TvKeyHint(listOf(Icons.Rounded.RadioButtonChecked), "отметить"),
            TvKeyHint(listOf(Icons.Rounded.KeyboardArrowUp, Icons.Rounded.KeyboardArrowDown), "папки"),
            TvKeyHint(listOf(Icons.AutoMirrored.Rounded.ArrowBack), "готово"),
        ),
        modifier = modifier,
    )
}

/** Черновик новой папки → то, что рисует редактор. Папка создаётся сразу с этим тайтлом. */
private fun FolderDraft.toEditorState() = TvFolderEditorState(
    heading = "Новая папка",
    title = title,
    maxLength = FOLDER_TITLE_MAX_LENGTH,
    tintIndex = tint,
    keyboardRows = keyboardRows(layout),
    layouts = KeyboardLayout.entries.map { it.label },
    layoutIndex = layout.ordinal,
    confirmLabel = "Создать и добавить",
    canConfirm = canSubmit,
)

private fun editorActions(onEvent: (FolderPickerEvent) -> Unit): TvFolderEditorActions {
    val edit: (FolderInput) -> Unit = { input -> onEvent(FolderPickerEvent.Edit(input)) }
    return TvFolderEditorActions(
        keyboard = TvKeyboardActions(
            onKey = { key -> edit(FolderInput.Key(key)) },
            onSpace = { edit(FolderInput.Space) },
            onDelete = { edit(FolderInput.Erase) },
        ),
        onTint = { tint -> edit(FolderInput.Tint(tint)) },
        onLayout = { index -> edit(FolderInput.Layout(KeyboardLayout.entries[index])) },
        onConfirm = { onEvent(FolderPickerEvent.Submit) },
        onCancel = { onEvent(FolderPickerEvent.CancelCreate) },
    )
}

/** Иконка «В папку»: тайтл уже лежит в папке — залитая папка, иначе «новая папка». */
internal fun folderIcon(inAnyFolder: Boolean): ImageVector =
    if (inAnyFolder) Icons.Rounded.Folder else Icons.Rounded.CreateNewFolder
