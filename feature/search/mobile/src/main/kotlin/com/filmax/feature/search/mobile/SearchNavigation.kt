package com.filmax.feature.search.mobile

import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.feature.search.common.SearchScreenModel
import com.filmax.feature.search.common.SearchSideEffect

/**
 * Переходы общего [SearchScreenModel] на телефоне — одни и те же у Каталога и вкладки Поиска.
 * Переводы фокуса — телевизионные: на телефоне фокус ведёт палец, их пропускаем.
 */
internal fun openSearchEffect(navigator: Navigator, effect: SearchSideEffect) {
    when (effect) {
        is SearchSideEffect.OpenDetails -> navigator.open(Destination.Details(effect.itemId))
        SearchSideEffect.Back -> navigator.back()
        SearchSideEffect.FocusKeyboard, SearchSideEffect.FocusSearchField, SearchSideEffect.FocusBestMatch -> Unit
    }
}
