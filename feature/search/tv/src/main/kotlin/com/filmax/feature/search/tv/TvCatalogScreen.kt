package com.filmax.feature.search.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.LocalReducedMotion
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
    val onEvent: (SearchEvent) -> Unit = screenModel::dispatch
    val screenFocus = rememberTvScreenFocus(startAt = SEARCH_KEY)
    val focus = remember(screenFocus) { CatalogScreenFocus(screenFocus) }
    val voice = rememberCatalogVoice { spoken -> onEvent(SearchEvent.OpenSearch(spoken)) }

    // Витрину и жанры тянем только здесь: телефонный поиск с тем же ScreenModel показывает
    // подсказки, и выдача каталога ему не нужна.
    LaunchedEffect(Unit) { onEvent(SearchEvent.LoadCatalog) }
    // Переводы фокуса — от модели: в том порядке, в каком она открыла поиск и нашла совпадение.
    screenModel.collectSideEffect { effect ->
        when (effect) {
            is SearchSideEffect.OpenDetails -> navigator.open(Destination.Details(effect.itemId))
            SearchSideEffect.Back -> navigator.back()
            SearchSideEffect.FocusKeyboard -> focus.jumps.moveTo(CatalogTarget.KEYBOARD)
            SearchSideEffect.FocusSearchField -> focus.jumps.moveTo(CatalogTarget.SEARCH_FIELD)
            SearchSideEffect.FocusBestMatch -> focus.jumps.moveTo(CatalogTarget.BEST_MATCH)
        }
    }
    // «Назад» в поиске — к витрине. Пока слушает голос, «Назад» — его отмена.
    BackHandler(enabled = state.searchOpen && !voice.controller.active) { onEvent(SearchEvent.CloseSearch) }

    TvCatalogContent(state = state, onEvent = onEvent, focus = focus, voice = voice, modifier = modifier)
}

/**
 * Экран целиком: витрина или поиск, боковые панели над ними и голосовой оверлей поверх всего.
 * Сетка помнит прокрутку, пока открыт поиск, — состояние списка живёт здесь, над обоими режимами.
 */
@Composable
internal fun TvCatalogContent(
    state: SearchState,
    onEvent: (SearchEvent) -> Unit,
    focus: CatalogScreenFocus,
    voice: CatalogVoice,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    // Своей заливки у экрана нет: фон — у окна (FilmaxTvTheme), второй слой цвета на весь экран —
    // лишняя работа GPU на каждом кадре.
    Box(
        modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                // Кнопка поиска на пульте — сразу голос: на телевизоре это и есть поиск.
                val searchKey = event.type == KeyEventType.KeyDown && event.key == Key.Search
                if (searchKey && !voice.controller.active) voice.start()
                searchKey
            },
    ) {
        TvSheetHost(
            open = state.sheetOpen,
            onClose = { onEvent(SearchEvent.CloseSheet) },
            returnFocus = focus.sheetOpeners.of(state.sheet),
            sheet = { CatalogSheetContent(state, onEvent) },
        ) {
            CatalogModes(
                state = state,
                onEvent = onEvent,
                focus = focus,
                voice = voice,
                gridState = gridState,
                modifier = Modifier.fxBackdrop(voice.controller.active, VoiceBackdropBlur),
            )
        }
        TvVoiceOverlay(voice, focus.jumps)
    }
}

/**
 * Витрина или поиск — по состоянию модели; подсветка палитры сверху — общая для обоих. Смена
 * режима — кроссфейдом: поле поиска в обоих режимах на одном месте и читается неподвижным, а
 * остальное сменяется. Фокус переводят заявки [CatalogFocus]: цель в новом режиме берёт его сама,
 * как только размещена, — уходящий режим за время кроссфейда фокус не удерживает.
 */
@Suppress("LongParameterList") // Вёрстка: состояние, события, фокус, голос, прокрутка и modifier — всё разной природы.
@Composable
private fun CatalogModes(
    state: SearchState,
    onEvent: (SearchEvent) -> Unit,
    focus: CatalogScreenFocus,
    voice: CatalogVoice,
    gridState: LazyGridState,
    modifier: Modifier,
) {
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = state.searchOpen,
        modifier = modifier.fillMaxSize().catalogWash().then(focus.screen.containerModifier),
        transitionSpec = { modeTransition(reduced) },
        label = "catalogMode",
    ) { searchOpen ->
        // Режим — из перехода, остальное — свежее состояние: уходящий режим доживает кроссфейд
        // с актуальными данными.
        if (searchOpen) {
            TvSearchPanel(state, onEvent, voice::start, focus)
        } else {
            TvCatalogGrid(state, onEvent, voice::start, focus, gridState)
        }
    }
}

private fun modeTransition(reduced: Boolean): ContentTransform =
    if (reduced) {
        ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)
    } else {
        ContentTransform(fadeIn(FxMotion.fade()), fadeOut(FxMotion.fade()), sizeTransform = null)
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

/**
 * Фокус экрана одной связкой: вход и возврат из деталей ([TvScreenFocus]), переходы на живом
 * экране ([jumps]) и чипы, на которые возвращается фокус из боковых панелей ([sheetOpeners]).
 */
@Stable
internal class CatalogScreenFocus(val screen: TvScreenFocus) {
    val jumps = CatalogFocus()
    val sheetOpeners = SheetOpeners()
}

/** Чипы, открывающие боковые панели: закрытая панель возвращает фокус на свой. */
@Stable
internal class SheetOpeners {
    val sort = FocusRequester()
    val filters = FocusRequester()

    /** Жанры на ТВ — чипы в шапке, панели у них нет; на всякий случай возврат — на сортировку. */
    fun of(sheet: CatalogSheet): FocusRequester = when (sheet) {
        CatalogSheet.SORT, CatalogSheet.GENRE -> sort
        CatalogSheet.FILTERS -> filters
    }
}
