package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory

/**
 * Ряды ленты. Идентификатор — единственное, чем экран адресует ряд: по нему просят догрузку
 * и берут заголовок (формулировки у телефона и ТВ свои, поэтому они живут в экранах).
 */
enum class HomeRowId { CONTINUE, TRENDING, COLLECTIONS, FOR_YOU, FRESH }

/**
 * Срез постранично догружаемого ряда. [page] — последняя загруженная страница каталога
 * (0 — в ряду только стартовая горстка из фида, каталог ещё не листали).
 */
data class RowPaging<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
)

/**
 * Ряд ленты. Состав и порядок ленты задаёт модель — экраны идут по [HomeState.rows] и рисуют
 * то, что пришло. Раньше порядок был записан отдельно в телефонном экране и отдельно в ТВ, и
 * совпадал он по договорённости, а не по устройству.
 *
 * Типов ряда три, потому что три типа карточки: прогресс, постер тайтла и постер подборки.
 */
sealed interface HomeRow {
    val id: HomeRowId

    /** Пустой ряд экран пропускает — заголовок без карточек хуже отсутствующего ряда. */
    val isEmpty: Boolean

    /** «Продолжить» — история просмотра целиком, страниц у неё нет. */
    data class Continue(val entries: List<WatchHistory>) : HomeRow {
        override val id: HomeRowId get() = HomeRowId.CONTINUE
        override val isEmpty: Boolean get() = entries.isEmpty()
    }

    /** Ряд тайтлов: «В тренде», «Сериалы с высоким рейтингом», «Новинки». Догружается из каталога. */
    data class Titles(override val id: HomeRowId, val paging: RowPaging<Item>) : HomeRow {
        override val isEmpty: Boolean get() = paging.items.isEmpty()
    }

    /**
     * «Подборки» — свой источник и своя карточка. [previews] — постеры и число тайтлов по id
     * подборки: догружаются после ленты, до тех пор карточка рисуется без них.
     */
    data class Collections(
        val paging: RowPaging<Collection>,
        val previews: Map<Int, CollectionPreview> = emptyMap(),
    ) : HomeRow {
        override val id: HomeRowId get() = HomeRowId.COLLECTIONS
        override val isEmpty: Boolean get() = paging.items.isEmpty()
    }
}

/**
 * Превью подборки на карточке: первые постеры и число тайтлов. [count] — только если оно
 * точное: kino.pub отдаёт число страниц, а не тайтлов, поэтому счёт есть лишь у подборок в одну
 * страницу.
 */
data class CollectionPreview(val posters: List<String>, val count: Int?)

data class HomeState(
    val loading: Boolean = true,
    /** Инициалы текущего пользователя для аватара в шапке (пусто — пока не загружено). */
    val initials: String = "",
    /** «Выбор редакции» — тайтлы карусели hero. */
    val heroes: List<Item> = emptyList(),
    /** Какой тайтл карусели сейчас на экране. */
    val heroIndex: Int = 0,
    /** Тайтлы в «Буду смотреть» — по ним кнопка hero залита или пуста. */
    val watchLaterIds: Set<Int> = emptySet(),
    val rows: List<HomeRow> = emptyList(),
    val error: String? = null,
) {
    /** Тайтл hero на экране. Телефон показывает только его. */
    val hero: Item?
        get() = heroes.getOrNull(heroIndex)

    /**
     * Показывать нечего: ни hero, ни одного непустого ряда. Холодный старт без сети выглядит
     * именно так — экран должен объясниться, а не остаться пустым.
     */
    val isEmpty: Boolean
        get() = hero == null && rows.all { it.isEmpty }
}

sealed interface HomeEvent {
    data object Load : HomeEvent

    /**
     * Догрузить ряд. Экран зовёт, когда лента доехала до хвоста ряда; повторные вызовы гасит
     * идемпотентность модели, поэтому триггер может быть сколь угодно грубым.
     */
    data class LoadMoreRow(val id: HomeRowId) : HomeEvent

    /** Карусель hero — к следующему тайтлу (по кругу). Экран шлёт, пока hero в фокусе. */
    data object NextHero : HomeEvent

    /** «Буду смотреть» для тайтла hero. */
    data class ToggleWatchLater(val item: Item) : HomeEvent
}

/**
 * Одноразовые эффекты Главной. Навигацию экраны просят у Navigator сами; эффект — только когда
 * решение принимает модель.
 */
sealed interface HomeSideEffect {
    /** Приложение обновилось — пора спросить, как оно: экран откроет окно оценки. */
    data object AskFeedback : HomeSideEffect
}
