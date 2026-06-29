package com.filmax.feature.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.error.AppError
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
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                screenModelScope { showError(AppError.Playback) }
            }
        })
        onFetchData()
    }

    override fun dispatch(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.SaveProgress -> saveProgress(event.positionMs)
            is PlayerEvent.SelectQuality -> selectQuality(event.label)
        }
    }

    override fun onFetchData() {
        screenModelScope {
            when (val result = catalog.getItemDetails(route.itemId)) {
                is RequestResult.Success -> {
                    val item = result.data
                    val track = item.tracklist.firstOrNull()
                    // Доступные качества — из файлов трека (метка + лучшая ссылка).
                    val qualities = track?.files.orEmpty().mapNotNull { file ->
                        (file.hls4 ?: file.hls ?: file.http)?.let { StreamQuality(file.quality, it) }
                    }
                    val initial = qualities.firstOrNull()

                    updateState {
                        it.copy(
                            loading = false,
                            item = item,
                            streamUrl = initial?.url,
                            qualities = qualities,
                            currentQuality = initial?.label,
                        )
                    }

                    if (initial != null) {
                        player.setMediaItem(MediaItem.fromUri(initial.url))
                        player.prepare()
                        player.playWhenReady = true
                    }
                }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    showError(result)
                }
            }
        }
    }

    private fun selectQuality(label: String) {
        val quality = state.qualities.firstOrNull { it.label == label } ?: return
        if (label == state.currentQuality) return
        val position = player.currentPosition
        val wasPlaying = player.playWhenReady
        player.setMediaItem(MediaItem.fromUri(quality.url))
        player.prepare()
        player.seekTo(position)
        player.playWhenReady = wasPlaying
        screenModelScope { updateState { it.copy(currentQuality = label, streamUrl = quality.url) } }
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
