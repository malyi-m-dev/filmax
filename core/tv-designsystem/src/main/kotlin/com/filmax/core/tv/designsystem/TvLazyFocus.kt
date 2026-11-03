package com.filmax.core.tv.designsystem

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * Память фокуса ленивого ряда или сетки — замена [tvFocusGroup] там, где элементы за краем
 * не скомпонованы. Вход в список — только на размещённый элемент: на тот, где фокус был последним,
 * иначе на предпочтительный (выбранная дорожка, играющая серия, первый жанр), иначе на первый
 * видимый. Если не подходит ни один, вход штатный — на ближайший по геометрии.
 *
 * Почему не `focusRestorer(fallback)`. Запасной вход там — FocusRequester на элементе, а ленивый
 * список держит в композиции только видимые: прокрутили ряд — у запасного запросчика нет узла,
 * и Compose 1.8 падает на `requestFocus` («FocusRequester is not initialized», 2.6.0 на TCL:
 * каталог и панель плеера). Промахивается же память restorer чаще, чем кажется: все группы
 * сохраняют её в реестр под одним ключом, и после возврата с другого экрана ряд достаёт запись
 * соседа. Ещё restorer запоминает элемент только при выходе обычным поиском — выход по
 * `focusProperties` (состав в деталях уводит «вниз» явно) прошёл бы мимо.
 *
 * Номер — обычное поле, не состояние: переход по ряду не должен перекомпоновывать ряд.
 */
@Stable
class TvLazyFocus internal constructor(private var lastIndex: Int?) {

    /** Запросчики скомпонованных элементов: у элемента за краем узла нет, и звать его нельзя. */
    private val entries = mutableMapOf<Int, FocusRequester>()

    internal fun attach(index: Int, requester: FocusRequester) {
        entries[index] = requester
    }

    /** Элемент ушёл из композиции; на его номере мог уже встать новый — тот не трогаем. */
    internal fun detach(index: Int, requester: FocusRequester) {
        if (entries[index] === requester) entries.remove(index)
    }

    internal fun focused(index: Int) {
        lastIndex = index
    }

    /**
     * Куда войти: [placed] — номера размещённых элементов. Скомпонованный, но не размещённый
     * элемент (подгрузка впрок) фокус примет, а на экране его не будет.
     */
    internal fun entry(placed: List<Int>, preferred: Int?): FocusRequester? {
        val index = listOfNotNull(lastIndex, preferred).firstOrNull { it in placed && it in entries }
            ?: placed.firstOrNull { it in entries }
        return index?.let(entries::get)
    }

    internal companion object {
        /**
         * Номер переживает уход экрана из композиции — в детали, плеер и обратно, как прокрутка
         * списка. Запросчики не сохраняются: они принадлежат композиции и создаются заново.
         */
        val Saver: Saver<TvLazyFocus, Int> = Saver(save = { it.lastIndex }, restore = { TvLazyFocus(it) })
    }
}

/** Память фокуса ленивого списка; живёт рядом с его прокруткой и так же переживает уход с экрана. */
@Composable
fun rememberTvLazyFocus(): TvLazyFocus = rememberSaveable(saver = TvLazyFocus.Saver) { TvLazyFocus(lastIndex = null) }

/**
 * Группа фокуса ленивого ряда с памятью [focus]: вход — на последний элемент в фокусе, иначе на
 * [preferred], иначе на первый видимый; только среди размещённых. Элементы отмечаются
 * [tvLazyFocusItem]. FocusRequester перед группой входит в неё так же — им и ставят фокус
 * в список снаружи.
 */
fun Modifier.tvLazyFocusGroup(focus: TvLazyFocus, state: LazyListState, preferred: Int? = null): Modifier =
    lazyFocusGroup(focus, preferred) { state.layoutInfo.visibleItemsInfo.map { it.index } }

/** То же для ленивой сетки. */
fun Modifier.tvLazyFocusGroup(focus: TvLazyFocus, state: LazyGridState, preferred: Int? = null): Modifier =
    lazyFocusGroup(focus, preferred) { state.layoutInfo.visibleItemsInfo.map { it.index } }

/**
 * `focusGroup` обязателен и идёт ПОСЛЕ свойств: он и есть та цель, в которую входят. Пустой вход
 * (размещённых отмеченных нет) не отменяет перехода — фокус получает ближайший по геометрии.
 */
private fun Modifier.lazyFocusGroup(focus: TvLazyFocus, preferred: Int?, placed: () -> List<Int>): Modifier =
    focusProperties { onEnter = { focus.entry(placed(), preferred)?.requestFocus() } }.focusGroup()

/**
 * Отметка элемента ленивого списка с номером [index] для его памяти [focus]: в фокусе — запомнить,
 * в композиции — можно войти. `hasFocus`, а не `isFocused`: у карточки фокус бывает на вложенной цели.
 */
@Composable
fun Modifier.tvLazyFocusItem(focus: TvLazyFocus, index: Int): Modifier {
    val requester = remember { FocusRequester() }
    DisposableEffect(focus, index, requester) {
        focus.attach(index, requester)
        onDispose { focus.detach(index, requester) }
    }
    return focusRequester(requester).onFocusChanged { if (it.hasFocus) focus.focused(index) }
}
