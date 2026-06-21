package com.filmax.feature.library

import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class LibraryScreenModel(
    private val watching: WatchingRepository,
    private val user: UserRepository,
) : BaseScreenModel<LibraryState, LibrarySideEffect, LibraryEvent>(LibraryState()) {

    init {
        onFetchData()
    }

    override fun dispatch(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.TabChange -> screenModelScope {
                updateState { it.copy(tab = event.tab) }
            }
            is LibraryEvent.RemoveFromHistory -> removeFromHistory(event.itemId)
            LibraryEvent.ClearHistory -> clearHistory()
        }
    }

    override fun onFetchData() {
        screenModelScope {
            coroutineScope {
                val historyDeferred = async { watching.getHistory() }
                val listsDeferred = async { user.getBookmarkFolders() }
                val history = historyDeferred.await()
                val lists = listsDeferred.await()
                updateState {
                    it.copy(
                        loading = false,
                        history = history.getOrNull() ?: emptyList(),
                        lists = lists.getOrNull() ?: emptyList(),
                        error = firstErrorMessage(history, lists),
                    )
                }
            }
        }
    }

    private fun removeFromHistory(itemId: Int) {
        screenModelScope {
            watching.clearHistory(itemId)
            updateState { s -> s.copy(history = s.history.filter { it.itemId != itemId }) }
        }
    }

    private fun clearHistory() {
        val ids = state.history.map { it.itemId }
        screenModelScope {
            ids.forEach { id -> watching.clearHistory(id) }
            updateState { it.copy(history = emptyList()) }
        }
    }
}
