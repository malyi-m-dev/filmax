package com.filmax.core.tv.designsystem.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/** Фон превью — фон палитры «Фиолет», как у раздела на ТВ. */
private const val PREVIEW_BACKGROUND = 0xFF0F0C1AL

/**
 * Превью экрана ТВ 1080p. Фокус в превью не ставится — его проверяют Robolectric-тесты фокуса;
 * превью показывает раскладку и состояния экрана.
 */
@Preview(name = "ТВ 1080p", device = Devices.TV_1080p, showBackground = true, backgroundColor = PREVIEW_BACKGROUND)
annotation class TvPreviews

/** Превью ТВ-компонента: без устройства, на фоне палитры. */
@Preview(showBackground = true, backgroundColor = PREVIEW_BACKGROUND, widthDp = 960)
annotation class TvComponentPreview
