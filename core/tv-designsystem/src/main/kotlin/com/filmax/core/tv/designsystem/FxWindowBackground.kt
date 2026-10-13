package com.filmax.core.tv.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

/**
 * Фон окна — цвет фона палитры. Окно и так заливает себя целиком до того, как рисует Compose;
 * пока у него свой чёрный фон, экраны вынуждены заливать себя поверх, и каждый пиксель
 * закрашивается лишний раз. С фоном палитры у окна экранам своя сплошная заливка не нужна.
 * Палитра сменилась — фон окна следом.
 */
@Composable
internal fun FxWindowBackground(color: Color) {
    val view = LocalView.current
    DisposableEffect(view, color) {
        view.context.findActivity()?.window?.setBackgroundDrawable(ColorDrawable(color.toArgb()))
        onDispose { }
    }
}

/** Активность за контекстом Compose: в тестах и превью её нет — тогда фон окна не трогаем. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
