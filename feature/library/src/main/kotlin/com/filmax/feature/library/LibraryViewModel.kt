package com.filmax.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
            try {
                //val historyDeferred = async { watching.getHistory() }
                val listsDeferred = async { user.getBookmarkFolders() }
                //val history = historyDeferred.await()
                val lists = listsDeferred.await()
                _state.update { it.copy(loading = false, /*history = history*/ lists = lists) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun onTabChange(tab: LibraryTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun removeFromHistory(itemId: Int) {
        viewModelScope.launch {
            try {
                watching.clearHistory(itemId)
                _state.update { s -> s.copy(history = s.history.filter { it.itemId != itemId }) }
            } catch (_: Exception) {}
        }
    }

    fun clearHistory() {
        val ids = _state.value.history.map { it.itemId }
        viewModelScope.launch {
            ids.forEach { id ->
                try { watching.clearHistory(id) } catch (_: Exception) {}
            }
            _state.update { it.copy(history = emptyList()) }
        }
    }
}
