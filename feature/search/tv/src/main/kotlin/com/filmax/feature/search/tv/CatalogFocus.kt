package com.filmax.feature.search.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/** Куда экран переводит фокус сам: из сетки в поиск и обратно, после голоса, на найденное. */
internal enum class CatalogTarget { SEARCH_FIELD, KEYBOARD, BEST_MATCH, MIC }

/**
 * Переводы фокуса внутри экрана каталога: открыли поиск — на клавиатуру, закрыли — на поле,
 * голос нашёл — на лучшее совпадение, голос отменили — на микрофон.
 *
 * Цель бывает уже на экране (голос из поиска — клавиатура стоит), а бывает, что только появится
 * (поиск сменил сетку целиком). Поэтому перевод — не вызов `requestFocus`, а заявка: цель,
 * помеченная [catalogTarget], исполняет её сама, как только оказывается на экране и размещена.
 *
 * Вход на экран и возврат из деталей — забота `rememberTvScreenFocus`: она помнит карточку, с
 * которой ушли, и переживает уход экрана из композиции. Здесь — только переходы на живом экране.
 */
@Stable
internal class CatalogFocus {

    /** Заявка на перевод фокуса; null — переводить некуда. */
    var pending: CatalogTarget? by mutableStateOf(null)
        private set

    fun moveTo(target: CatalogTarget) {
        pending = target
    }

    fun done(target: CatalogTarget) {
        if (pending == target) pending = null
    }
}

@Composable
internal fun rememberCatalogFocus(): CatalogFocus = remember { CatalogFocus() }

/**
 * Сколько кадров цель пробует взять фокус. Запрос отклоняют, пока окно в первом проходе
 * раскладки; дольше пары кадров это не длится, а вечная заявка украла бы фокус позже, невпопад.
 */
private const val FOCUS_ATTEMPTS = 3

/**
 * Помечает цель [target]: пока заявка [CatalogFocus.pending] указывает на неё, цель забирает фокус.
 * Запрос — через кадр после появления заявки: к нему цель уже размещена.
 */
@Composable
internal fun Modifier.catalogTarget(focus: CatalogFocus, target: CatalogTarget): Modifier {
    val requester = remember { FocusRequester() }
    val wanted = focus.pending == target
    LaunchedEffect(wanted) {
        if (!wanted) return@LaunchedEffect
        var granted = false
        var attempt = 0
        while (!granted && attempt < FOCUS_ATTEMPTS) {
            withFrameNanos { }
            granted = requester.requestFocus()
            attempt++
        }
        focus.done(target)
    }
    return focusRequester(requester)
}
