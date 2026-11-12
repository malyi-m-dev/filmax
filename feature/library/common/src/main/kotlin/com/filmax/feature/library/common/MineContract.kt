package com.filmax.feature.library.common

import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.presentation.cards.WatchCard
import com.filmax.core.presentation.cards.WatchlistCard
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
 * Карточка папки: папка с оформлением, постеры веера, когда её меняли («изменена вчера»), счётчик
 * «6 тайтлов» и подпись ТВ «6 тайтлов · изменена вчера».
 */
data class MineFolder(
    val folder: UserFolder,
    val posters: List<String>,
    val changed: String?,
    val count: String,
    val caption: String,
)

/** День истории: «Сегодня», «Вчера», «12 октября» или «Раньше» — для записей без времени. */
data class HistoryDay(val label: String, val entries: List<WatchCard>)

/** Что «Моё» просит подтвердить — убрать тайтл; [title] — для текста шторки. */
sealed interface MineConfirm {
    val itemId: Int
    val title: String

    data class Watchlist(override val itemId: Int, override val title: String) : MineConfirm
    data class History(override val itemId: Int, override val title: String) : MineConfirm
}

data class MineState(
    val loading: Boolean = true,
    /** Сегменты, которые есть у источника: без папок — без «Папок». */
    val segments: List<MineSegment> = MineSegment.entries,
    val segment: MineSegment = MineSegment.CONTINUE,
    /** Начатое и не досмотренное — сегмент «Продолжить». */
    val continueWatching: List<WatchCard> = emptyList(),
    val history: List<HistoryDay> = emptyList(),
    /** «Буду смотреть» уже в порядке [watchlistSort]. */
    val watchlist: List<WatchlistCard> = emptyList(),
    val watchlistSort: WatchlistSort = WatchlistSort.NEWEST,
    /** Папки пользователя уже в порядке [folderSort], без системной «Буду смотреть». */
    val folders: List<MineFolder> = emptyList(),
    val folderSort: FolderSort = FolderSort.UPDATED,
    /** Черновик новой папки; не null — открыта панель создания. */
    val draft: FolderDraft? = null,
    /** Открыт диалог «Очистить историю?». */
    val confirmClearHistory: Boolean = false,
    /** Открыта шторка «Убрать тайтл?» (телефон: на ТВ долгое «ОК» убирает сразу). */
    val confirm: MineConfirm? = null,
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

    /** Убрать тайтл: на ТВ — сразу долгим «ОК», на телефоне — после вопроса. */
    sealed interface Removal : MineEvent

    /** Долгое «ОК» на постере «Буду смотреть». */
    data class RemoveFromWatchlist(val itemId: Int) : Removal

    /** Долгое «ОК» на карточке «Продолжить»: kino.pub убирает тайтл из истории целиком. */
    data class RemoveFromContinue(val itemId: Int) : Removal

    /** Долгий тап на телефоне: спросить, убрать ли тайтл. */
    data class AskRemove(val confirm: MineConfirm) : Removal
    data object ConfirmRemove : Removal
    data object DismissRemove : Removal

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
