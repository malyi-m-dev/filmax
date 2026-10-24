package com.filmax.feature.tvlink.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.filmax.core.tv.designsystem.TvInfoCard
import com.filmax.feature.tvlink.common.TvRemoteInfoScreenModel
import com.filmax.feature.tvlink.common.cardCaption
import com.filmax.feature.tvlink.common.cardValue
import org.koin.androidx.compose.koinViewModel

/** «Пульт с телефона» в Профиле ТВ: кто управляет телевизором и по какому адресу его найти. */
@Composable
fun TvRemoteCard(
    modifier: Modifier = Modifier,
    screenModel: TvRemoteInfoScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    TvInfoCard(
        overline = "Пульт с телефона",
        value = state.cardValue(),
        caption = state.cardCaption(),
        modifier = modifier,
    )
}
