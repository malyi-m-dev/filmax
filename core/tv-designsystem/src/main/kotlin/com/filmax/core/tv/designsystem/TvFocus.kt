package com.filmax.core.tv.designsystem

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import kotlinx.coroutines.flow.filterNotNull

/**
 * Держит ли фокус верхний таб-бар. Экран не отбирает у него фокус: пользователь выбирает
 * вкладки подряд, и подхват фокуса контентом стоил бы ему лишнего «вверх» на каждый переход.
 */
val LocalTvNavBarFocused: ProvidableCompositionLocal<State<Boolean>> =
    compositionLocalOf { mutableStateOf(false) }

/**
 * Группа фокуса с памятью — для ряда, сетки, панели чипов.
 *
 * `focusRestorer` запоминает выбранный элемент, когда фокус уходит из группы, и возвращает его
 * при следующем входе: иначе «вниз» в прокрученный ряд ведёт на пространственно-ближайшую
 * карточку, а не на ту, с которой ушли. При первом входе запоминать нечего: фокус получает
 * [fallback], а без него — элемент, ближайший к тому, откуда пришли. Группе, где один элемент
 * выбран (сезон среди чипов), fallback нужен: ближайший чип — не выбранный, а фокус на чипе
 * этот выбор меняет.
 *
 * `focusGroup` обязателен и идёт ПОСЛЕ restorer: он и есть тот focus target, чьих детей restorer
 * сохраняет. Без него свойства restorer достаются каждому элементу по отдельности — сохранять
 * внутри элемента нечего, и «восстановление» вырождается в вызов fallback при каждом входе.
 */
fun Modifier.tvFocusGroup(fallback: FocusRequester = FocusRequester.Default): Modifier =
    focusRestorer(fallback).focusGroup()

/**
 * Фокус TV-экрана: стартовая цель и возврат туда, откуда ушли на другой экран.
 *
 * Переходы внутри экрана держит [tvFocusGroup] — штатный `focusRestorer`. Но уход на другой
 * экран это снятие композиции: restorer сохраняет выбор только при живом переходе фокуса
 * наружу, а его запись в `LocalSaveableStateRegistry` до возврата не доживает (проверено на
 * устройстве: реестр отдаёт null). Поэтому выбранное экран помнит сам — одним ключом в
 * [rememberSaveable], который NavHost хранит рядом с прокруткой списков.
 *
 * Фокус ставится по `onPlaced` — это единственный честный момент: ленивые списки компонуют
 * элементы в фазе измерения, и до неё запрашивать фокус не на чем. Никаких ретраев по кадрам:
 * узел сам сообщает, что он размещён.
 *
 * Поставленным фокус считается, только когда запрос прошёл. Самое первое размещение экрана
 * приходит, пока окно ещё в первом проходе раскладки, и Android запрос отклоняет — без повтора
 * Главная оставалась вовсе без фокуса, и первое «вниз» уводило в боковое меню. Отклонённый
 * запрос повторяется один раз — после ближайшего кадра, когда раскладка окна уже завершена
 * ([rememberTvScreenFocus] ждёт его через `withFrameNanos`).
 */
@Stable
class TvScreenFocus internal constructor(
    private val lastFocused: MutableState<String?>,
    private val navBarFocused: State<Boolean>,
    returnTo: String?,
) {

    /** Куда вернуть фокус — снимок памяти на момент входа на экран. */
    private var returnTo: String? = returnTo

    private val container = FocusRequester()

    /** Фокус уже поставлен — дальше прокрутка и перекомпоновки его не трогают. */
    private var done = false

    /** Запрос, отклонённый до конца раскладки окна, — ждёт повтора после кадра. */
    internal var pendingRetry: FocusRequester? by mutableStateOf(null)
        private set

    private fun request(requester: FocusRequester) {
        done = requester.requestFocus()
        if (!done) pendingRetry = requester
    }

    /** Повтор отклонённого запроса — единственный: если окно не берёт фокус и теперь, экран ждёт пульта. */
    internal fun retryPending() {
        val requester = pendingRetry ?: return
        pendingRetry = null
        if (!done) done = requester.requestFocus()
    }

    /**
     * Модификатор корневого контейнера экрана: `LazyColumn`, сетка, скролл-колонка.
     *
     * Стартовый фокус берёт на себя только когда возвращаться некуда: на свежем экране фокуса
     * нет вовсе. Если [returnTo] есть, контейнер молчит — иначе фокус успел бы встать на первый
     * элемент, а bring-into-view утащил бы к нему восстановленную прокрутку.
     */
    val containerModifier: Modifier = Modifier
        .focusRequester(container)
        .tvFocusGroup()
        .onPlaced {
            if (!done && returnTo == null && !navBarFocused.value) request(container)
        }

    /**
     * Поставить фокус заново, когда содержимое экрана сменилось целиком: открыли папку
     * закладок, переключили сегмент, закрыли оверлей клавиатуры. Элемент, на котором стоял
     * фокус, при этом уходит из композиции, и без вмешательства фокус повис бы — пульт
     * перестал бы отвечать.
     *
     * [key] — ключ элемента, которому фокус причитается ([item]); null — первому элементу
     * нового содержимого. Сработает, когда этот элемент разместится.
     */
    fun focusOn(key: String? = null) {
        returnTo = key
        done = false
    }

    /**
     * Модификатор элемента, который может стать точкой возврата: карточки ряда, плитки сетки,
     * кнопки hero. [key] обязан быть уникальным в пределах экрана — один тайтл встречается
     * в нескольких рядах сразу, поэтому ключ карточки это «ряд:id».
     */
    @Composable
    fun item(key: String): Modifier {
        val requester = remember { FocusRequester() }
        return Modifier
            .focusRequester(requester)
            .onFocusChanged { if (it.isFocused) lastFocused.value = key }
            .onPlaced {
                if (!done && key == returnTo) request(requester)
            }
    }
}

/**
 * Создаёт [TvScreenFocus]: память экрана переживает уход в детали, плеер и обратно.
 *
 * [startAt] — куда поставить фокус, когда возвращать ещё некуда (первый заход на экран). Ключ
 * того же элемента, что и в [TvScreenFocus.item]: на Деталях это кнопка «Смотреть». Без него
 * стартовый фокус достаётся первому элементу, который найдёт D-pad-поиск, — для ленты
 * с карточками это ровно то, что нужно, а для экрана с шапкой уже нет.
 */
@Composable
fun rememberTvScreenFocus(startAt: String? = null): TvScreenFocus {
    val lastFocused = rememberSaveable { mutableStateOf<String?>(null) }
    val navBarFocused = LocalTvNavBarFocused.current
    val focus = remember { TvScreenFocus(lastFocused, navBarFocused, returnTo = lastFocused.value ?: startAt) }
    LaunchedEffect(focus) {
        snapshotFlow { focus.pendingRetry }.filterNotNull().collect {
            withFrameNanos { }
            focus.retryPending()
        }
    }
    return focus
}
