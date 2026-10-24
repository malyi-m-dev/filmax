package com.filmax.feature.tvlink.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.filmax.feature.tvlink.common.navigation.RemoteRoute
import com.filmax.feature.tvlink.common.navigation.TvPickerRoute
import com.filmax.feature.tvlink.mobile.RemoteScreen
import com.filmax.feature.tvlink.mobile.TvPickerDialog

/** Выбор телевизора — окном поверх деталей или плеера: под ним остаётся то, что выбирали. */
fun NavGraphBuilder.tvPickerDialog() {
    dialog<TvPickerRoute> { TvPickerDialog() }
}

/** Пульт — экран на весь экран, без нижней навигации. */
fun NavGraphBuilder.remoteScreen() {
    composable<RemoteRoute> { RemoteScreen() }
}
