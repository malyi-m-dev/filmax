// Ключи фокуса карточек «Моё»: по ним экран помнит, куда вернуть фокус, и знает, куда поставить
// его при первом входе. Ключ — «сегмент:id»: один тайтл бывает и в «Продолжить», и в «Истории».
package com.filmax.feature.library.tv

import com.filmax.feature.library.common.MineSegment
import com.filmax.feature.library.common.MineState

/** Ключ «Создать папку» — первой ячейки сетки папок. */
internal const val CREATE_FOLDER_KEY = "folders:create"

/** Первая карточка текущего сегмента — туда встаёт фокус при первом входе в раздел. */
internal fun startKey(state: MineState): String? = when (state.segment) {
    MineSegment.CONTINUE -> state.continueWatching.firstOrNull()?.let { continueKey(it.itemId) }
    MineSegment.WATCHLIST -> state.watchlist.firstOrNull()?.let { watchlistKey(it.id) }
    MineSegment.FOLDERS -> CREATE_FOLDER_KEY
    MineSegment.HISTORY -> state.history.firstOrNull()?.entries?.firstOrNull()?.let { historyKey(it.entry.itemId) }
}

internal fun continueKey(itemId: Int) = "continue:$itemId"
internal fun watchlistKey(itemId: Int) = "watchlist:$itemId"
internal fun folderKey(folderId: Int) = "folder:$folderId"
internal fun historyKey(itemId: Int) = "history:$itemId"
