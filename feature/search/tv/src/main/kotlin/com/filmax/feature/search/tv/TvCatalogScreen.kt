package com.filmax.feature.search.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSheetHost
import com.filmax.core.tv.designsystem.fxBackdrop
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.feature.search.common.CatalogSheet
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import com.filmax.feature.search.common.SearchSideEffect
import com.filmax.feature.search.common.SearchState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Ключ поля поиска — стартовая цель экрана и точка возврата. */
internal const val SEARCH_KEY = "search"

/** Ключ лучшего совпадения — точка возврата из деталей, открытых из поиска. */
internal const val BEST_MATCH_KEY = "match"

/**
 * ТВ-Каталог (макеты S05, S06, S16, S25) — витрина и поиск в одном разделе.
 *
 * Витрина — сетка постеров по фильтрам тип/жанр/сортировка, наполненная ещё до первой буквы:
 * поле поиска и чипы живут в той же сетке и уезжают со скроллом. «ОК» на поле открывает поиск —
 * своя экранная клавиатура слева, «Лучшее совпадение» и остальные результаты справа; «Назад»
 * возвращает витрину ровно такой, какой она была. Голос — оверлей поверх размытого каталога.
 *
 * Состояние экрана целиком в [SearchScreenModel] — тот же, что у телефонного каталога; экран
 * только рисует его и отдаёт события. Своё у экрана — только фокус и распознаватель речи.
 */
@Composable
fun TvCatalogScreen(
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val catalogFocus = rememberCatalogFocus()
    val voice = rememberCatalogVoice { spoken ->
        screenModel.dispatch(SearchEvent.OpenSearch(spoken))
        catalogFocus.moveTo(CatalogTarget.KEYBOARD)
    }
    val actions = remember(screenModel, navigator, catalogFocus, voice) {
        catalogActions(screenModel, navigator, catalogFocus, voice)
    }

    // Витрину и жанры тянем только здесь: телефонный поиск с тем же ScreenModel показывает
    // подсказки, и выдача каталога ему не нужна.
    LaunchedEffect(Unit) { screenModel.dispatch(SearchEvent.LoadCatalog) }
    screenModel.collectSideEffect { effect ->
        when (effect) {
            SearchSideEffect.FocusBestMatch -> catalogFocus.moveTo(CatalogTarget.BEST_MATCH)
        }
    }
    // «Назад» в поиске — к витрине, фокус на поле. Пока слушает голос, «Назад» — его отмена.
    BackHandler(enabled = state.searchOpen && !voice.controller.active) { actions.onCloseSearch() }

    val screenFocus = rememberTvScreenFocus(startAt = SEARCH_KEY)
    val focus = remember(screenFocus, catalogFocus) { CatalogScreenFocus(screenFocus, catalogFocus) }
    TvCatalogContent(state = state, actions = actions, focus = focus, voice = voice, modifier = modifier)
}

/**
 * Экран целиком: витрина или поиск, боковые панели над ними и голосовой оверлей поверх всего.
 * Сетка помнит прокрутку, пока открыт поиск, — состояние списка живёт здесь, над обоими режимами.
 */
@Composable
internal fun TvCatalogContent(
    state: SearchState,
    actions: CatalogActions,
    focus: CatalogScreenFocus,
    voice: CatalogVoice,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val chrome = remember(gridState) { CatalogGridChrome(gridState, SheetOpeners()) }
    Box(
        modifier
            .fillMaxSize()
            .background(LocalFxColors.current.background)
            .onPreviewKeyEvent { event ->
                // Кнопка поиска на пульте — сразу голос: на телевизоре это и есть поиск.
                val searchKey = event.type == KeyEventType.KeyDown && event.key == Key.Search
                if (searchKey && !voice.controller.active) actions.onVoice()
                searchKey
            },
    ) {
        TvSheetHost(
            open = state.sheetOpen,
            onClose = { actions.dispatch(SearchEvent.CloseSheet) },
            returnFocus = chrome.sheetOpeners.of(state.sheet),
            sheet = { CatalogSheetContent(state, actions.dispatch) },
        ) {
            CatalogModes(
                state = state,
                actions = actions,
                focus = focus,
                chrome = chrome,
                modifier = Modifier.fxBackdrop(voice.controller.active, VoiceBackdropBlur),
            )
        }
        TvVoiceOverlay(voice, focus.jumps)
    }
}

/** Витрина или поиск — по состоянию модели; подсветка палитры сверху — общая для обоих. */
@Composable
private fun CatalogModes(
    state: SearchState,
    actions: CatalogActions,
    focus: CatalogScreenFocus,
    chrome: CatalogGridChrome,
    modifier: Modifier,
) {
    Box(modifier.fillMaxSize().catalogWash().then(focus.screen.containerModifier)) {
        if (state.searchOpen) {
            TvSearchPanel(state, actions, focus)
        } else {
            TvCatalogGrid(state, actions, focus, chrome)
        }
    }
}

/** Подсветка палитры у верхнего края: эллипс 50% ширины на 260dp, центр — на 80% ширины (как в макете). */
private val WashHeight = 260.dp
private const val WASH_CENTER_X = 0.8f
private const val WASH_HALF_WIDTH = 0.5f
private const val WASH_FADE = 0.75f

@Composable
private fun Modifier.catalogWash(): Modifier {
    val wash = LocalFxColors.current.wash
    return drawBehind {
        val radius = WashHeight.toPx()
        val center = Offset(size.width * WASH_CENTER_X, 0f)
        // Градиент Compose круглый — эллипс макета получается растяжением круга по ширине.
        scale(scaleX = size.width * WASH_HALF_WIDTH / radius, scaleY = 1f, pivot = center) {
            drawCircle(
                Brush.radialGradient(0f to wash, WASH_FADE to Color.Transparent, center = center, radius = radius),
                radius = radius,
                center = center,
            )
        }
    }
}

/** Фокус экрана: вход и возврат из деталей ([TvScreenFocus]) и переходы на живом экране. */
@Stable
internal class CatalogScreenFocus(val screen: TvScreenFocus, val jumps: CatalogFocus)

/** Чипы, открывающие боковые панели: закрытая панель возвращает фокус на свой. */
@Stable
internal class SheetOpeners {
    val sort = FocusRequester()
    val filters = FocusRequester()

    fun of(sheet: CatalogSheet): FocusRequester = when (sheet) {
        CatalogSheet.SORT -> sort
        CatalogSheet.FILTERS -> filters
    }
}

/**
 * Действия экрана одним объектом. Почти всё — событие модели как есть ([dispatch]); отдельно —
 * то, что кроме события двигает фокус или навигацию.
 */
internal data class CatalogActions(
    val dispatch: (SearchEvent) -> Unit,
    /** Карточка витрины — в детали. */
    val onOpenItem: (Int) -> Unit,
    /** Результат поиска — в детали; запрос запоминается в недавних. */
    val onOpenResult: (Int) -> Unit,
    /** «ОК» на поле поиска: поиск открыт, фокус — на клавиатуре. */
    val onOpenSearch: () -> Unit,
    /** «Назад» в поиске: витрина, фокус — на поле. */
    val onCloseSearch: () -> Unit,
    val onVoice: () -> Unit,
)

private fun catalogActions(
    screenModel: SearchScreenModel,
    navigator: Navigator,
    focus: CatalogFocus,
    voice: CatalogVoice,
) = CatalogActions(
    dispatch = screenModel::dispatch,
    onOpenItem = { itemId -> navigator.open(Destination.Details(itemId)) },
    onOpenResult = { itemId ->
        screenModel.dispatch(SearchEvent.ResultOpened)
        navigator.open(Destination.Details(itemId))
    },
    onOpenSearch = {
        screenModel.dispatch(SearchEvent.OpenSearch())
        focus.moveTo(CatalogTarget.KEYBOARD)
    },
    onCloseSearch = {
        screenModel.dispatch(SearchEvent.CloseSearch)
        focus.moveTo(CatalogTarget.SEARCH_FIELD)
    },
    onVoice = voice::start,
)
