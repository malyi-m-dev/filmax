package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.navigation.Destination

/**
 * Ряды ленты. Идентификатор — единственное, чем экран адресует ряд: по нему просят догрузку.
 * Порядок ленты задаёт модель, а не порядок констант.
 *
 * Первые пять приходят с фидом, остальные — тематические: у каждого свой запрос каталога
 * (`THEME_SOURCES`), и догружаются они уже после фида.
 */
enum class HomeRowId {
    CONTINUE,
    TRENDING,
    COLLECTIONS,
    FOR_YOU,
    FRESH,
    POPULAR_SERIES,
    CARTOONS,
    COMEDIES,
    SCI_FI,
    ANIME,
    DORAMAS,
    DOCUMENTARY,
    TV_SHOWS,
    CONCERTS,
}

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
 * Ряд ленты. Состав, порядок и заголовки ленты задаёт модель — экраны идут по [HomeState.rows] и
 * рисуют то, что пришло. Раньше порядок и формулировки были записаны отдельно в телефонном
 * экране и отдельно в ТВ, и совпадали они по договорённости, а не по устройству.
 *
 * Типов ряда три, потому что три типа карточки: прогресс, постер тайтла и постер подборки.
 */
sealed interface HomeRow {
    val id: HomeRowId

    /** Заголовок ряда и пояснение рядом с ним: «В тренде · на этой неделе». */
    val title: String
    val subtitle: String?

    /** Пустой ряд экран пропускает — заголовок без карточек хуже отсутствующего ряда. */
    val isEmpty: Boolean

    /** «Продолжить» — история просмотра целиком, страниц у неё нет. */
    data class Continue(val entries: List<WatchHistory>) : HomeRow {
        override val id: HomeRowId get() = HomeRowId.CONTINUE
        override val title: String get() = "Продолжить просмотр"
        override val subtitle: String? get() = null
        override val isEmpty: Boolean get() = entries.isEmpty()
    }

    /**
     * Ряд тайтлов: «В тренде», «Новинки», тематические ряды. Догружается из каталога; тематический
     * до первой страницы пуст, и экран его пропускает.
     */
    data class Titles(
        override val id: HomeRowId,
        override val title: String,
        override val subtitle: String?,
        val paging: RowPaging<Item>,
    ) : HomeRow {
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
        override val title: String get() = "Подборки"
        override val subtitle: String? get() = "от редакции"
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
    /** Какой тайтл карусели сейчас на экране ТВ и он сам — модель меняет их вместе. */
    val heroIndex: Int = 0,
    val hero: Item? = null,
    /** Тайтлы в «Буду смотреть» — по ним кнопка hero залита или пуста. */
    val watchLaterIds: Set<Int> = emptySet(),
    val rows: List<HomeRow> = emptyList(),
    /**
     * Показывать нечего: ни hero, ни одного непустого ряда. Холодный старт без сети выглядит
     * именно так — экран должен объясниться, а не остаться пустым.
     */
    val isEmpty: Boolean = true,
    /** Шторка «Разделы» на телефоне открыта. */
    val sectionsOpen: Boolean = false,
    val error: String? = null,
) {
    /** Hero ТВ уже в «Буду смотреть» — кнопка залита. */
    val heroInWatchLater: Boolean get() = hero != null && hero.id in watchLaterIds
}

sealed interface HomeEvent {
    data object Load : HomeEvent

    /**
     * Догрузить ряд. Экран зовёт, когда лента доехала до хвоста ряда; повторные вызовы гасит
     * идемпотентность модели, поэтому триггер может быть сколь угодно грубым.
     */
    data class LoadMoreRow(val id: HomeRowId) : HomeEvent

    /** Hero ТВ получил или потерял фокус: пока он в фокусе, карусель листается сама. */
    data class HeroFocused(val focused: Boolean) : HomeEvent

    /** «Буду смотреть» для тайтла hero. */
    data class ToggleWatchLater(val item: Item) : HomeEvent

    data class OpenItem(val itemId: Int) : HomeEvent

    /** «Смотреть» у hero — плеер возьмёт первый трек или продолжит с сохранённого прогресса. */
    data class Play(val itemId: Int) : HomeEvent

    /** Карточка «Продолжить» — сразу в плеер на недосмотренную серию из истории. */
    data class ContinueWatching(val entry: WatchHistory) : HomeEvent

    data class OpenCollection(val collection: Collection) : HomeEvent

    /** Раздел каталога из чипов шапки или шторки «Разделы» (телефон). */
    data class OpenSection(val section: CatalogSection) : HomeEvent
    data object OpenSections : HomeEvent
    data object CloseSections : HomeEvent

    /** Пульт телевизора (телефон) — без тайтла: телевизор выбирают ради пульта. */
    data object OpenTv : HomeEvent

    /** Финал ленты ТВ: дальше — каталог. */
    data object OpenCatalog : HomeEvent
}

/**
 * Переходы с Главной. Модель решает, куда вести (сезон и серию «Продолжить», окно оценки после
 * обновления), а экран исполняет переход навигатором: смена экрана — дело вёрстки.
 */
sealed interface HomeSideEffect {
    data class OpenDetails(val itemId: Int) : HomeSideEffect

    /** Плеер; [videoId] и [season] — [Destination.Player.UNKNOWN], если серия не известна. */
    data class OpenPlayer(
        val itemId: Int,
        val videoId: Int = Destination.Player.UNKNOWN,
        val season: Int = Destination.Player.UNKNOWN,
    ) : HomeSideEffect

    data class OpenSection(val section: CatalogSection) : HomeSideEffect
    data class OpenCollection(val collectionId: Int, val title: String) : HomeSideEffect
    data object OpenTvPicker : HomeSideEffect
    data object OpenCatalog : HomeSideEffect

    /** Приложение обновилось — пора спросить оценку. */
    data object AskFeedback : HomeSideEffect
}
