package com.filmax.feature.player.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.player.common.navigation.TrailerRoute

/**
 * Трейлер играет штатным контроллером Media3 по готовому URL — модели здесь нечего загружать.
 * Она лишь отдаёт экрану адрес и название из маршрута и закрывает его по «Назад».
 */
class TrailerScreenModel(
    savedStateHandle: SavedStateHandle,
    private val navigator: Navigator,
) : BaseScreenModel<TrailerState, TrailerSideEffect, TrailerEvent>(initialState(savedStateHandle)) {

    override fun onFetchData() = Unit

    override fun onEvent(event: TrailerEvent) {
        when (event) {
            TrailerEvent.Back -> navigator.back()
        }
    }
}

private fun initialState(savedStateHandle: SavedStateHandle): TrailerState {
    val route = savedStateHandle.toRoute<TrailerRoute>()
    return TrailerState(url = route.url, title = route.title)
}
