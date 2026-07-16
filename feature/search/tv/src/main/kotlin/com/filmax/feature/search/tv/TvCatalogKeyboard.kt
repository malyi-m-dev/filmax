package com.filmax.feature.search.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnAccent
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvRatingPill
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.rememberVoiceSearch
import com.filmax.feature.search.common.MIN_QUERY_LENGTH
import com.filmax.feature.search.common.SearchState
import kotlinx.coroutines.delay

/**
 * Ширина клавиатуры. Раньше она занимала 500 из 844dp — постеры ужимались до размера иконки.
 * 400dp хватает раскладке в 11 клавиш и оставляет живой выдаче больше половины экрана.
 */
private val KeyboardWidth = 400.dp

private const val KEY_HEIGHT_DP = 44
private const val KEY_GAP_DP = 8

/** Служебная клавиша шире буквенной во столько раз. */
private const val KEY_WIDE = 2f
private const val KEY_SPACE = 4f

/** Живая выдача рядом с клавиатурой: три постера в ряд — как в макете. */
private const val LIVE_COLUMNS = 3

/** Индекс первого буквенного ряда: нулевой — служебный («Голос»/«Очистить»/«Готово»). */
private const val FIRST_LETTER_ROW = 1

private const val CARET_BLINK_MILLIS = 500L

private val RuRows = listOf(
    "Й Ц У К Е Н Г Ш Щ З Х",
    "Ф Ы В А П Р О Л Д Ж Э",
    "Я Ч С М И Т Ь Б Ю Ё",
)
private val EnRows = listOf(
    "Q W E R T Y U I O P",
    "A S D F G H J K L",
    "Z X C V B N M",
)
private val NumRows = listOf(
    "1 2 3 4 5 6 7 8 9 0",
    "- — № . , ! ? : ;",
)

private enum class KeyLayout { RU, EN, NUM }

private enum class KeyAction { CHAR, SPACE, BACKSPACE, CLEAR, DONE, VOICE, SWITCH }

private data class KeyCap(
    val label: String,
    val action: KeyAction,
    val weight: Float = 1f,
    val primary: Boolean = false,
)

/** Колбэки оверлея одним объектом — иначе список параметров упирается в порог detekt. */
internal data class TvKeyboardActions(
    val onQuery: (String) -> Unit,
    val onSubmit: (String) -> Unit,
    val onOpenItem: (Int) -> Unit,
    val onClose: () -> Unit,
)

/**
 * Экранная клавиатура каталога. Три раскладки (ЙЦУКЕН / QWERTY / цифры) с переключателем:
 * с одной кириллицей «Breaking Bad» на пульте набрать физически нельзя.
 *
 * Справа — живая выдача по мере ввода, и она фокусируемая: набрал три буквы, ушёл вправо,
 * выбрал фильм. Это и есть главный сценарий, ради которого клавиатура вообще открывается.
 */
@Composable
internal fun TvKeyboardOverlay(state: SearchState, actions: TvKeyboardActions) {
    var layout by remember { mutableStateOf(KeyLayout.RU) }
    val firstKey = remember { FocusRequester() }
    val startVoice = rememberVoiceSearch { spoken -> actions.onSubmit(spoken) }

    BackHandler(onBack = actions.onClose)
    LaunchedEffect(Unit) {
        // Ждём кадр: до раскладки клавиш FocusRequester ещё не привязан к узлу.
        withFrameNanos { }
        runCatching { firstKey.requestFocus() }
    }

    fun onKey(key: KeyCap) {
        when (key.action) {
            KeyAction.CHAR -> actions.onQuery(state.query + key.label)
            KeyAction.SPACE -> actions.onQuery(state.query + " ")
            KeyAction.BACKSPACE -> actions.onQuery(state.query.dropLast(1))
            KeyAction.CLEAR -> actions.onQuery("")
            KeyAction.SWITCH -> layout = nextLayout(layout)
            KeyAction.VOICE -> startVoice()
            KeyAction.DONE -> actions.onClose()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(TvSurface.copy(alpha = 0.97f))
            .padding(
                start = TvMetrics.SafeHorizontal,
                end = TvMetrics.SafeHorizontal,
                top = 64.dp,
                bottom = 40.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(modifier = Modifier.width(KeyboardWidth)) {
            KeyboardInput(query = state.query)
            Text(
                "Раскладка: ${layoutLabel(layout)}",
                style = MaterialTheme.typography.bodySmall,
                color = TvOnSurfaceDim,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
            KeyboardKeys(layout = layout, firstKey = firstKey, onKey = ::onKey)
        }
        KeyboardResults(
            state = state,
            onOpenItem = { id ->
                actions.onOpenItem(id)
                actions.onClose()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Строка ввода с мигающим курсором и прокруткой в конец: раньше это был `Box` с `Text`, и
 * длинный запрос обрезался с концом — ровно там, куда попадает следующая буква.
 */
@Composable
private fun KeyboardInput(query: String) {
    val scroll = rememberScrollState()
    var caretOn by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        // На время набора курсор горит ровно, а строка догоняет конец текста — так видно,
        // куда именно уходит буква. Мигание начинается, когда набор остановился.
        caretOn = true
        withFrameNanos { }
        scroll.animateScrollTo(scroll.maxValue)
        while (true) {
            delay(CARET_BLINK_MILLIS)
            caretOn = !caretOn
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(TvMetrics.PanelShape)
            .background(TvSurfaceContainerHigh)
            .border(2.dp, TvSurfaceContainerHighest, TvMetrics.PanelShape)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = TvOnSurfaceDim,
            modifier = Modifier.size(20.dp),
        )
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(scroll),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                query,
                style = MaterialTheme.typography.titleMedium,
                color = TvOnSurface,
                maxLines = 1,
                softWrap = false,
            )
            Box(
                Modifier
                    .padding(start = 2.dp)
                    .size(width = 2.dp, height = 22.dp)
                    .background(if (caretOn) TvAccent else Color.Transparent)
            )
        }
    }
}

@Composable
private fun KeyboardKeys(layout: KeyLayout, firstKey: FocusRequester, onKey: (KeyCap) -> Unit) {
    val rows = remember(layout) { keyRows(layout) }
    Column(verticalArrangement = Arrangement.spacedBy(KEY_GAP_DP.dp)) {
        rows.forEachIndexed { rowIndex, row ->
            KeyboardRow(
                row = row,
                // Фокус при открытии — на первой букве, а не на служебном ряду сверху.
                firstKey = firstKey.takeIf { rowIndex == FIRST_LETTER_ROW },
                onKey = onKey,
            )
        }
    }
}

@Composable
private fun KeyboardRow(row: List<KeyCap>, firstKey: FocusRequester?, onKey: (KeyCap) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP_DP.dp)) {
        row.forEachIndexed { index, key ->
            KeyboardKey(
                key = key,
                onClick = { onKey(key) },
                focusRequester = firstKey.takeIf { index == 0 },
            )
        }
    }
}

@Composable
private fun RowScope.KeyboardKey(key: KeyCap, onClick: () -> Unit, focusRequester: FocusRequester?) {
    val shape = MaterialTheme.shapes.small
    TvFocusCard(
        onClick = onClick,
        shape = shape,
        focusRequester = focusRequester,
        modifier = Modifier.weight(key.weight).height(KEY_HEIGHT_DP.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(if (key.primary) TvAccent else TvSurfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                key.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (key.primary) TvOnAccent else TvOnSurface,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun KeyboardResults(state: SearchState, onOpenItem: (Int) -> Unit, modifier: Modifier = Modifier) {
    // Выдачу показываем только для актуального запроса: иначе под коротким «О» висели бы
    // результаты предыдущего, уже стёртого слова.
    val results = if (state.query.length >= MIN_QUERY_LENGTH) state.results else emptyList()

    Column(modifier) {
        Text("Результаты", style = MaterialTheme.typography.bodySmall, color = TvOnSurfaceDim)
        Spacer(Modifier.height(14.dp))
        if (results.isEmpty()) {
            Text(
                "Начните вводить или нажмите «Голос» — совпадений пока нет.",
                style = MaterialTheme.typography.bodyLarge,
                color = TvOnSurfaceVariant,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(LIVE_COLUMNS),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = TvMetrics.FocusInset),
            ) {
                items(results, key = { it.id }) { item ->
                    LiveResultCard(item = item, onClick = { onOpenItem(item.id) })
                }
            }
        }
    }
}

@Composable
private fun LiveResultCard(item: Item, onClick: () -> Unit) {
    Column {
        TvFocusCard(
            onClick = onClick,
            shape = TvMetrics.PosterShape,
            modifier = Modifier.fillMaxWidth().aspectRatio(POSTER_RATIO),
        ) {
            Box(Modifier.fillMaxSize().clip(TvMetrics.PosterShape)) {
                PosterImage(
                    url = item.posters.medium.ifEmpty { item.posters.big },
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    shape = TvMetrics.PosterShape,
                    accentColor = TvSurfaceContainer,
                )
                formatRating(item.rating.external)?.let { rating ->
                    TvRatingPill(
                        rating = rating,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    )
                }
            }
        }
        Text(
            item.title,
            style = MaterialTheme.typography.bodySmall,
            color = TvOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private const val POSTER_RATIO = 2f / 3f

/** Раскладка по макету: служебный ряд, буквы, ряд с переключателем/пробелом/забоем. */
private fun keyRows(layout: KeyLayout): List<List<KeyCap>> {
    val letters = when (layout) {
        KeyLayout.RU -> RuRows
        KeyLayout.EN -> EnRows
        KeyLayout.NUM -> NumRows
    }
    return buildList {
        add(
            listOf(
                KeyCap("Голос", KeyAction.VOICE, weight = KEY_WIDE),
                KeyCap("Очистить", KeyAction.CLEAR, weight = KEY_WIDE),
                KeyCap("Готово", KeyAction.DONE, weight = KEY_WIDE, primary = true),
            )
        )
        letters.forEach { row -> add(row.split(" ").map { KeyCap(it, KeyAction.CHAR) }) }
        add(
            listOf(
                KeyCap(switchLabel(layout), KeyAction.SWITCH, weight = KEY_WIDE),
                // Словом, а не глифом ⎵: клавиша шириной в 192dp, а U+23B5 во встроенных
                // шрифтах Android TV есть не всегда — вместо пробела вышел бы «тофу».
                KeyCap("пробел", KeyAction.SPACE, weight = KEY_SPACE),
                KeyCap("⌫", KeyAction.BACKSPACE, weight = KEY_WIDE),
            )
        )
    }
}

/** На клавише переключателя написано, КУДА он ведёт, а не где мы сейчас. */
private fun switchLabel(layout: KeyLayout): String = when (layout) {
    KeyLayout.RU -> "ABC"
    KeyLayout.EN -> "123"
    KeyLayout.NUM -> "АБВ"
}

private fun layoutLabel(layout: KeyLayout): String = when (layout) {
    KeyLayout.RU -> "русская"
    KeyLayout.EN -> "латиница"
    KeyLayout.NUM -> "цифры и символы"
}

private fun nextLayout(layout: KeyLayout): KeyLayout = when (layout) {
    KeyLayout.RU -> KeyLayout.EN
    KeyLayout.EN -> KeyLayout.NUM
    KeyLayout.NUM -> KeyLayout.RU
}
