package com.filmax.core.tv.designsystem

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch

/**
 * Сигнал «прокрутить контент в самый верх».
 *
 * Боковое меню инкрементирует значение при повторном выборе открытого раздела. Экраны слушают
 * сигнал и скроллят свой контейнер к началу.
 */
val LocalTvScrollToTop = compositionLocalOf { 0 }

/** Сколько кадров переждать перед прокруткой к началу — см. [scrollToTopOnFocus]. */
private const val FRAMES_BEFORE_SCROLL = 2

/**
 * Фокус пришёл в этот элемент ленты (hero) — лента целиком к началу. Фокус-прокрутка ТВ поднимает
 * в кадр только сфокусированную кнопку, и высокий баннер над ней оставался срезанным сверху,
 * стоило вернуться к нему из рядов.
 *
 * Прокрутка через два кадра: в кадре смены фокуса системный bringIntoView перехватил бы её, и
 * лента застряла бы на полпути (тот же приём, что у полотна ТВ-деталей).
 */
@Composable
fun Modifier.scrollToTopOnFocus(state: LazyListState): Modifier {
    val scope = rememberCoroutineScope()
    return onFocusChanged { focusState ->
        if (focusState.hasFocus) {
            scope.launch {
                repeat(FRAMES_BEFORE_SCROLL) { withFrameNanos { } }
                state.animateScrollToItem(0)
            }
        }
    }
}

/** Подписывает [LazyColumn]/список на сигнал шапки: при фокусе на таб-баре едет к началу. */
@Composable
fun ScrollToTopOnNavFocus(state: LazyListState) {
    val initial = rememberInitialSignal()
    val signal = LocalTvScrollToTop.current
    LaunchedEffect(signal) { if (signal > initial) state.animateScrollToItem(0) }
}

/** Подписывает сетку (`LazyVerticalGrid`) на сигнал шапки. */
@Composable
fun ScrollToTopOnNavFocus(state: LazyGridState) {
    val initial = rememberInitialSignal()
    val signal = LocalTvScrollToTop.current
    LaunchedEffect(signal) { if (signal > initial) state.animateScrollToItem(0) }
}

/** Подписывает обычный скролл-контейнер (`Modifier.verticalScroll`) на сигнал шапки. */
@Composable
fun ScrollToTopOnNavFocus(state: ScrollState) {
    val initial = rememberInitialSignal()
    val signal = LocalTvScrollToTop.current
    LaunchedEffect(signal) { if (signal > initial) state.animateScrollTo(0) }
}

/**
 * Значение сигнала в момент входа экрана в композицию. Реагировать можно только на рост
 * ПОСЛЕ этого: сигнал копится за сессию, и вернувшийся из бэкстека экран иначе видел бы
 * «свежий» ненулевой сигнал и перетирал восстановленный скролл прокруткой к началу.
 */
@Composable
private fun rememberInitialSignal(): Int {
    val signal = LocalTvScrollToTop.current
    return remember { signal }
}
