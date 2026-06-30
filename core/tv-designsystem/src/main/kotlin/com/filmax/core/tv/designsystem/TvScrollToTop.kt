package com.filmax.core.tv.designsystem

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf

/**
 * Сигнал «прокрутить контент в самый верх».
 *
 * Верхний таб-бар инкрементирует значение каждый раз, когда фокус заходит в него с контента.
 * Экраны слушают сигнал и скроллят свой контейнер к началу — иначе после ухода фокуса вверх
 * контент остаётся «застрявшим» в прокрученном состоянии. Стартовое значение 0 реакции не
 * вызывает (первый скролл произойдёт только при реальном получении фокуса шапкой).
 */
val LocalTvScrollToTop = compositionLocalOf { 0 }

/** Подписывает [LazyColumn]/список на сигнал шапки: при фокусе на таб-баре едет к началу. */
@Composable
fun ScrollToTopOnNavFocus(state: LazyListState) {
    val signal = LocalTvScrollToTop.current
    LaunchedEffect(signal) { if (signal > 0) state.animateScrollToItem(0) }
}

/** Подписывает сетку (`LazyVerticalGrid`) на сигнал шапки. */
@Composable
fun ScrollToTopOnNavFocus(state: LazyGridState) {
    val signal = LocalTvScrollToTop.current
    LaunchedEffect(signal) { if (signal > 0) state.animateScrollToItem(0) }
}

/** Подписывает обычный скролл-контейнер (`Modifier.verticalScroll`) на сигнал шапки. */
@Composable
fun ScrollToTopOnNavFocus(state: ScrollState) {
    val signal = LocalTvScrollToTop.current
    LaunchedEffect(signal) { if (signal > 0) state.animateScrollTo(0) }
}
