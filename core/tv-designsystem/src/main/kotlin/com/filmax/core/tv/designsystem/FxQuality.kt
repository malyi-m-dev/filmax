package com.filmax.core.tv.designsystem

import android.app.ActivityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Облегчённые эффекты для слабых телевизоров и приставок. Бесконечные анимации (ken-burns фона,
 * пульс свечения фокуса) и размытие под панелями не дают приложению простаивать: экран
 * перекомпонуется на каждом кадре, и на слабом GPU прокрутка рядов начинает рваться. В облегчённом
 * режиме фон неподвижен, свечение фокуса горит ровно, а под панелью экран только затемняется.
 * Фокус, рамка, масштаб и переходы остаются — интерфейс тот же, без фоновой нагрузки.
 */
val LocalLightEffects = staticCompositionLocalOf { false }

/**
 * Экран под панелью или оверлеем: его бесконечные анимации на паузе. Иначе размытие под панелью
 * пересчитывалось бы на каждом кадре ken-burns, которого под панелью всё равно не видно.
 */
val LocalFxPaused = staticCompositionLocalOf { false }

/**
 * Облегчённый режим — только там, где его просит сама система (`isLowRamDevice`: Android Go,
 * приставки на 1 ГБ). Внешний вид важнее, поэтому обычные приставки получают полные эффекты:
 * их хватает оптимизаций, которые картинку не меняют — свечение из готовой текстуры, затемнения
 * одним слоем, пауза анимаций под панелями и вне фокуса.
 */
@Composable
internal fun rememberLightEffects(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true }
}
