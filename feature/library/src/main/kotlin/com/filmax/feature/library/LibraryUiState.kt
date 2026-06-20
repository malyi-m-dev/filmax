package com.filmax.feature.library

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory

enum class LibraryTab(val label: String) {
    FAVORITES("Избранное"),
    HISTORY("История"),
    LISTS("Списки"),
}

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.FAVORITES,
    val favorites: List<Item> = emptyList(),
    val history: List<WatchHistory> = emptyList(),
    val lists: List<com.filmax.core.domain.user.model.BookmarkFolder> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)
