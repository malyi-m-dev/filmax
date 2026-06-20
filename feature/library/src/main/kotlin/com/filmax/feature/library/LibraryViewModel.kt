package com.filmax.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val watching: WatchingRepository,
    private val user: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            coroutineScope {
                val historyDeferred = async { watching.getHistory() }
                val listsDeferred = async { user.getBookmarkFolders() }
                val history = historyDeferred.await()
                val lists = listsDeferred.await()
                _state.update {
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

    fun onTabChange(tab: LibraryTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun removeFromHistory(itemId: Int) {
        viewModelScope.launch {
            watching.clearHistory(itemId)
            _state.update { s -> s.copy(history = s.history.filter { it.itemId != itemId }) }
        }
    }

    fun clearHistory() {
        val ids = _state.value.history.map { it.itemId }
        viewModelScope.launch {
            ids.forEach { id -> watching.clearHistory(id) }
            _state.update { it.copy(history = emptyList()) }
        }
    }
}
