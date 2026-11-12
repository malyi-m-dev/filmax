package com.filmax.core.ui.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/** Фон превью — фон палитры «Фиолет»: тёмные экраны на белом не читаются. */
private const val PREVIEW_BACKGROUND = 0xFF0F0C1AL

/**
 * Превью экрана телефона: обычный и узкий телефон, крупный шрифт (настройка доступности) и
 * планшет. Экраны превьюят чистую перегрузку `XxxScreen(state, actions)` — без модели и Koin.
 */
@Preview(name = "Телефон", device = Devices.PIXEL_7, showBackground = true, backgroundColor = PREVIEW_BACKGROUND)
@Preview(
    name = "Узкий телефон",
    device = "spec:width=360dp,height=640dp,dpi=320",
    showBackground = true,
    backgroundColor = PREVIEW_BACKGROUND,
)
@Preview(
    name = "Крупный шрифт",
    device = Devices.PIXEL_7,
    fontScale = 1.3f,
    showBackground = true,
    backgroundColor = PREVIEW_BACKGROUND,
)
@Preview(name = "Планшет", device = Devices.PIXEL_TABLET, showBackground = true, backgroundColor = PREVIEW_BACKGROUND)
annotation class PhonePreviews

/** Превью компонента: без устройства, на фоне палитры. */
@Preview(showBackground = true, backgroundColor = PREVIEW_BACKGROUND)
annotation class ComponentPreview
