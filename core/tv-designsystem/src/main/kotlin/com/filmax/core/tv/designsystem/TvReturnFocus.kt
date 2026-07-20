package com.filmax.core.tv.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

/**
 * Возврат фокуса на карточку, с которой ушли на пуш-экран (детали, плеер, подборка).
 *
 * NavHost восстанавливает скролл вернувшегося экрана, но не фокус: без этого механизма фокус
 * доставался первому focusable экрана (hero, поиск), и bring-into-view утаскивал прокрутку
 * к нему — «экран теряет позицию». Экран помечает карточку в onClick через [onOpen] и вешает
 * [target] на неё же — при следующем входе в композицию фокус приезжает ровно туда, а
 * восстановленная прокрутка остаётся на месте (карточка уже видима).
 *
 * Ключ — стабильный идентификатор карточки, уникальный в пределах экрана («continue:42»,
 * «grid:17»): один тайтл может встречаться в нескольких рядах сразу.
 */
class TvReturnFocus internal constructor(
    private val savedKey: MutableState<String?>,
) {
    internal val requester = FocusRequester()

    /** Вызывать в onClick карточки перед навигацией: сюда вернётся фокус. */
    fun onOpen(key: String) {
        savedKey.value = key
    }

    /** FocusRequester для карточки с этим ключом; null для всех остальных. */
    fun target(key: String): FocusRequester? = requester.takeIf { savedKey.value == key }

    internal fun consume() {
        savedKey.value = null
    }

    internal fun pending(): Boolean = savedKey.value != null
}

/**
 * Создаёт [TvReturnFocus] и восстанавливает фокус при возврате на экран.
 *
 * Ленивые списки компонуют карточки в фазе измерения, а прокрученный ряд добирается до своей
 * позиции не с первого кадра — поэтому реквест ретраится несколько кадров подряд, пока
 * помеченная карточка не привяжет FocusRequester. Всё это успевает раньше глобального
 * фоллбека графа — тот увидит занятый фокус и не станет вмешиваться. Если помеченной карточки
 * больше нет (список изменился), попытки молча кончаются, и фокус ставит фоллбек графа.
 */
@Composable
fun rememberTvReturnFocus(): TvReturnFocus {
    val savedKey = rememberSaveable { mutableStateOf<String?>(null) }
    val returnFocus = remember { TvReturnFocus(savedKey) }
    LaunchedEffect(Unit) {
        if (!returnFocus.pending()) return@LaunchedEffect
        repeat(RETURN_FOCUS_ATTEMPTS) {
            withFrameNanos { }
            val granted = runCatching { returnFocus.requester.requestFocus() }.isSuccess
            if (granted) {
                // focusRestorer ряда перехватывает ВХОД фокуса в группу и уводит его на свой
                // fallback — первую карточку. Повторный реквест идёт уже изнутри группы,
                // enter не срабатывает, и фокус встаёт ровно на помеченную карточку.
                withFrameNanos { }
                runCatching { returnFocus.requester.requestFocus() }
                returnFocus.consume()
                return@LaunchedEffect
            }
        }
        returnFocus.consume()
    }
    return returnFocus
}

private const val RETURN_FOCUS_ATTEMPTS = 8
