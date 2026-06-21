package com.filmax.feature.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.feature.player.navigation.PlayerRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loading: Boolean = true,
    val item: Item? = null,
    val streamUrl: String? = null,
    val error: String? = null,
)

class PlayerViewModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
    context: Context,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PlayerRoute>()
    private val _state = MutableStateFlow(PlayerUiState())
    val state = _state.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        loadAndPlay()
    }

    private fun loadAndPlay() {
        viewModelScope.launch {
            when (val result = catalog.getItemDetails(route.itemId)) {
                is RequestResult.Success -> {
                    val item = result.data
                    val track = item.tracklist.firstOrNull()
                    val url = track?.files
                        ?.sortedByDescending { it.hls4 != null }
                        ?.firstOrNull()
                        ?.let { it.hls4 ?: it.hls ?: it.http }

                    _state.update { it.copy(loading = false, item = item, streamUrl = url) }

                    if (url != null) {
                        player.setMediaItem(MediaItem.fromUri(url))
                        player.prepare()
                        player.playWhenReady = true
                    }
                }

                is RequestResult.Error ->
                    _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun saveProgress(positionMs: Long) {
        val item = _state.value.item ?: return
        val videoId = item.tracklist.firstOrNull()?.id ?: return
        viewModelScope.launch {
            watching.saveProgress(item.id, videoId, (positionMs / 1000).toInt())
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
