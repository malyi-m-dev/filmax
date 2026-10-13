package com.filmax.feature.library.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Передача фокуса соседу, когда карточку убирают долгим «ОК» («Буду смотреть», «Продолжить»,
 * содержимое папки).
 *
 * Фокус надо отдать ДО того, как карточка уйдёт из сетки: если сначала убрать её, Compose сам
 * переносит потерянный фокус — во входную точку экрана (вкладку сегмента), и пульт оказывается
 * далеко от места, где стоял. Поэтому сетка помнит фокус-цели своих видимых карточек по ключу.
 *
 * Но долгое «ОК» срабатывает, пока кнопка ещё нажата: её отпускание придёт уже соседу, и тот
 * воспримет его как клик — откроет тайтл. Поэтому после передачи фокуса сетка глотает ближайшее
 * отпускание «ОК» ([gridModifier]).
 */
@Stable
internal class CardFocusHandoff {

    /** Цели только размещённых карточек: просить фокус у ушедшей из композиции нельзя. */
    private val requesters = mutableMapOf<String, FocusRequester>()

    /** Фокус отдан по долгому «ОК» — его отпускание ещё впереди и соседу не достанется. */
    private var swallowOkRelease = false

    /**
     * Модификатор сетки: перехватывает отпускание «ОК» после долгого нажатия. Новое нажатие «ОК»
     * снимает перехват — отпускание могло уйти мимо сетки (соседа не было, фокус ушёл наверх).
     */
    val gridModifier: Modifier = Modifier.onPreviewKeyEvent { event ->
        if (event.key !in OkKeys) return@onPreviewKeyEvent false
        val freshPress = event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0
        if (freshPress) swallowOkRelease = false
        val swallow = swallowOkRelease && event.type == KeyEventType.KeyUp
        if (swallow) swallowOkRelease = false
        swallow
    }

    /** Модификатор карточки с ключом [key] — регистрирует её цель на время жизни в сетке. */
    @Composable
    fun card(key: String): Modifier {
        val requester = remember(key) { FocusRequester() }
        DisposableEffect(key) {
            requesters[key] = requester
            onDispose { requesters.remove(key) }
        }
        return Modifier.focusRequester(requester)
    }

    /** Фокус — на следующую за [removed] карточку, у последней — на предыдущую. */
    fun handOff(keys: List<String>, removed: String) {
        swallowOkRelease = true
        neighborKey(keys, removed)?.let { neighbor -> requesters[neighbor]?.requestFocus() }
    }
}

/** Клавиши «ОК» пульта: центр крестовины и Enter. */
private val OkKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

@Composable
internal fun rememberCardFocusHandoff(): CardFocusHandoff = remember { CardFocusHandoff() }

/** Сосед убранной карточки: следующая, у последней — предыдущая; null — карточка была одна. */
internal fun neighborKey(keys: List<String>, removed: String): String? {
    val index = keys.indexOf(removed)
    if (index < 0) return null
    return keys.getOrNull(index + 1) ?: keys.getOrNull(index - 1)
}
