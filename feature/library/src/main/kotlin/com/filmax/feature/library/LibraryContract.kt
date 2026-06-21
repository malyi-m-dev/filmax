package com.filmax.feature.library

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.watching.model.WatchHistory

enum class LibraryTab(val label: String) {
    FAVORITES("Избранное"),
    HISTORY("История"),
    LISTS("Списки"),
}

data class LibraryState(
    val tab: LibraryTab = LibraryTab.FAVORITES,
    val favorites: List<Item> = emptyList(),
    val history: List<WatchHistory> = emptyList(),
    val lists: List<BookmarkFolder> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

sealed interface LibraryEvent {
    data class TabChange(val tab: LibraryTab) : LibraryEvent
    data class RemoveFromHistory(val itemId: Int) : LibraryEvent
    data object ClearHistory : LibraryEvent
}

sealed interface LibrarySideEffect
