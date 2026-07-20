package com.filmax.core.tv.designsystem

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * Сигнал «прокрутить контент в самый верх».
 *
 * Верхний таб-бар инкрементирует значение каждый раз, когда фокус заходит в него с контента.
 * Экраны слушают сигнал и скроллят свой контейнер к началу — иначе после ухода фокуса вверх
 * контент остаётся «застрявшим» в прокрученном состоянии.
 */
val LocalTvScrollToTop = compositionLocalOf { 0 }

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
