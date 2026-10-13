package com.filmax.feature.library.common

import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput

/**
 * Сегменты ТВ-раздела «Моё» — четыре непересекающихся ответа на вопрос «что у меня есть».
 * На телефоне вкладки другие ([LibraryTab]): там есть «Загрузки», а «Буду смотреть» зовётся
 * «Избранным».
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

/** Карточка папки: папка с оформлением, постеры веера и когда её меняли («изменена вчера»). */
data class MineFolder(
    val folder: UserFolder,
    val posters: List<String>,
    val changed: String?,
)

/** Запись истории и время просмотра «21:40» — подпись под карточкой. */
data class HistoryEntry(val entry: WatchHistory, val time: String?)

/** День истории: «Сегодня», «Вчера», «12 октября» или «Раньше» — для записей без времени. */
data class HistoryDay(val label: String, val entries: List<HistoryEntry>)

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

    data object AskClearHistory : MineEvent
    data object ConfirmClearHistory : MineEvent
    data object DismissClearHistory : MineEvent

    /** «Создать папку» — открывает панель с пустым черновиком. */
    data object StartCreateFolder : MineEvent
    data class EditFolder(val input: FolderInput) : MineEvent
    data object SubmitFolder : MineEvent
    data object CancelFolder : MineEvent

    /** Экран снова на виду (вернулись из папки, деталей, плеера) — перечитать историю и папки. */
    data object Refresh : MineEvent
}

sealed interface MineSideEffect
