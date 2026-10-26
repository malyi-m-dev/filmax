package com.filmax.feature.library.common

import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput

/**
 * Сегменты раздела «Моё» на телефоне и ТВ — четыре непересекающихся ответа на вопрос «что у меня
 * есть». «Загрузок» нет: kino.pub не отдаёт файлы для офлайна.
 */
enum class MineSegment(val label: String) {
    CONTINUE("Продолжить"),
    WATCHLIST("Буду смотреть"),
    FOLDERS("Папки"),
    HISTORY("История"),
}

/**
 * Порядок «Буду смотреть». «Сначала новые» — порядок самой папки на сервере: kino.pub кладёт
 * добавленный тайтл первым, даты добавления в ответе нет.
 */
enum class WatchlistSort(val label: String) {
    NEWEST("Сначала новые"),
    RATING("По рейтингу"),
    TITLE("А–Я"),
}

/** Порядок папок: по дате изменения (свежие первыми) или по названию. */
enum class FolderSort(val label: String) {
    UPDATED("По изменению"),
    TITLE("По названию"),
}

/**
 * Карточка папки: папка с оформлением, постеры веера и подпись — «6 тайтлов · изменена вчера»
 * ([caption]); [changed] — одна дата изменения, если нужна отдельно.
 */
data class MineFolder(
    val folder: UserFolder,
    val posters: List<String>,
    val changed: String?,
    val caption: String,
)

/**
 * Запись истории с готовыми подписями: время просмотра «21:40», серия «S4E7» (null — фильм) и
 * сколько посмотрели («на 45%», «просмотрено»). Платформы складывают их в свои строки.
 */
data class HistoryEntry(
    val entry: WatchHistory,
    val time: String?,
    val episodeCode: String?,
    val watchedShare: String?,
)

/** День истории: «Сегодня», «Вчера», «12 октября» или «Раньше» — для записей без времени. */
data class HistoryDay(val label: String, val entries: List<HistoryEntry>)

/** Что телефон просит подтвердить перед удалением: тайтл из «Буду смотреть» или из истории. */
sealed interface MineRemoval {
    val itemId: Int
    val title: String

    data class Watchlist(override val itemId: Int, override val title: String) : MineRemoval
    data class History(override val itemId: Int, override val title: String) : MineRemoval
}

data class MineState(
    val loading: Boolean = true,
    val segment: MineSegment = MineSegment.CONTINUE,
    /** Начатое и не досмотренное — сегмент «Продолжить». */
    val continueWatching: List<WatchHistory> = emptyList(),
    val history: List<HistoryDay> = emptyList(),
    /** «Буду смотреть» уже в порядке [watchlistSort]. */
    val watchlist: List<FavoriteItem> = emptyList(),
    val watchlistSort: WatchlistSort = WatchlistSort.NEWEST,
    /** Папки пользователя уже в порядке [folderSort], без системной «Буду смотреть». */
    val folders: List<MineFolder> = emptyList(),
    val folderSort: FolderSort = FolderSort.UPDATED,
    /** Черновик новой папки; не null — открыта панель создания. */
    val draft: FolderDraft? = null,
    /** Открыт диалог «Очистить историю?». */
    val confirmClearHistory: Boolean = false,
    /** Открыта шторка «Убрать тайтл?» (телефон); на ТВ долгое «ОК» убирает сразу. */
    val confirmRemoval: MineRemoval? = null,
    val error: String? = null,
) {
    /** Счётчик сегмента; null — у сегмента счётчика нет (так в макете у «Продолжить» и «Истории»). */
    fun countOf(segment: MineSegment): Int? = when (segment) {
        MineSegment.WATCHLIST -> watchlist.size
        MineSegment.FOLDERS -> folders.size
        MineSegment.CONTINUE, MineSegment.HISTORY -> null
    }
}

sealed interface MineEvent {
    /** Сегмент в фокусе — на ТВ он выбирается фокусом, без «ОК». */
    data class SelectSegment(val segment: MineSegment) : MineEvent

    /** Следующий порядок «Буду смотреть» по кругу — так работает чип сортировки справа. */
    data object CycleWatchlistSort : MineEvent

    data object CycleFolderSort : MineEvent

    /** Долгое «ОК» на постере «Буду смотреть». */
    data class RemoveFromWatchlist(val itemId: Int) : MineEvent

    /** Долгое «ОК» на карточке «Продолжить»: kino.pub убирает тайтл из истории целиком. */
    data class RemoveFromContinue(val itemId: Int) : MineEvent

    /** Телефон: долгий тап спрашивает, прежде чем убрать; «Убрать» в шторке — [ConfirmRemoval]. */
    data class AskRemoval(val removal: MineRemoval) : MineEvent
    data object ConfirmRemoval : MineEvent
    data object DismissRemoval : MineEvent

    data object AskClearHistory : MineEvent
    data object ConfirmClearHistory : MineEvent
    data object DismissClearHistory : MineEvent

    /** «Создать папку» — открывает панель с пустым черновиком. */
    data object StartCreateFolder : MineEvent
    data class EditFolder(val input: FolderInput) : MineEvent
    data object SubmitFolder : MineEvent
    data object CancelFolder : MineEvent

    data class OpenItem(val itemId: Int) : MineEvent

    /** Карточка «Продолжить» или строка истории — сразу в плеер на недосмотренную серию. */
    data class ContinueWatching(val entry: WatchHistory) : MineEvent
    data class OpenFolder(val folderId: Int) : MineEvent

    /** Поиск из шапки «Моё» (телефон). */
    data object OpenSearch : MineEvent

    /** Экран снова на виду (вернулись из папки, деталей, плеера) — перечитать историю и папки. */
    data object Refresh : MineEvent
}

/** Переходы из «Моё»; исполняет их навигатором экран. */
sealed interface MineSideEffect {
    data class OpenDetails(val itemId: Int) : MineSideEffect

    /** Папка-закладка: на ТВ — свой экран, на телефоне граф откроет её внутри Библиотеки. */
    data class OpenFolder(val folderId: Int) : MineSideEffect
    data object OpenSearch : MineSideEffect

    /** «Продолжить» и история — сразу в плеер; [videoId] — номер серии внутри [season]. */
    data class OpenPlayer(val itemId: Int, val videoId: Int, val season: Int) : MineSideEffect
}
