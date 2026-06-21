package com.filmax.feature.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.player.navigation.PlayerRoute

class PlayerScreenModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
    context: Context,
) : BaseScreenModel<PlayerState, PlayerSideEffect, PlayerEvent>(PlayerState()) {

    private val route = savedStateHandle.toRoute<PlayerRoute>()

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        onFetchData()
    }

    override fun dispatch(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.SaveProgress -> saveProgress(event.positionMs)
        }
    }

    override fun onFetchData() {
        screenModelScope {
            when (val result = catalog.getItemDetails(route.itemId)) {
                is RequestResult.Success -> {
                    val item = result.data
                    val track = item.tracklist.firstOrNull()
                    val url = track?.files
                        ?.sortedByDescending { it.hls4 != null }
                        ?.firstOrNull()
                        ?.let { it.hls4 ?: it.hls ?: it.http }

                    updateState { it.copy(loading = false, item = item, streamUrl = url) }

                    if (url != null) {
                        player.setMediaItem(MediaItem.fromUri(url))
                        player.prepare()
                        player.playWhenReady = true
                    }
                }

                is RequestResult.Error ->
                    updateState { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun saveProgress(positionMs: Long) {
        val item = state.item ?: return
        val videoId = item.tracklist.firstOrNull()?.id ?: return
        screenModelScope {
            watching.saveProgress(item.id, videoId, (positionMs / 1000).toInt())
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
