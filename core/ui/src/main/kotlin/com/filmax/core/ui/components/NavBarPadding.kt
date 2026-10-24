package com.filmax.core.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Сколько места снизу экран раздела оставляет плавающей навигации — вместе с системной панелью.
 * Ставит оболочка приложения, когда навигация видна; экраны вне разделов (детали, плеер) получают
 * ноль и сами отступают от системной панели. Лента прокручивается ПОД навигацией — стекло размывает
 * её, — а последний ряд останавливается над ней.
 */
val LocalNavBarPadding = staticCompositionLocalOf<Dp> { 0.dp }
