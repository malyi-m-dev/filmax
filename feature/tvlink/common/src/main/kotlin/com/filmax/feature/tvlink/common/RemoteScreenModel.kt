package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvRemote
import com.filmax.core.presentation.BaseScreenModel

/**
 * Пульт телевизора на телефоне. Связь не его — она одна на приложение ([TvRemote]): закрыли пульт, а
 * телевизор остался подключённым, и пульт откроется на том же месте.
 */
class RemoteScreenModel(
    private val remote: TvRemote,
) : BaseScreenModel<RemoteState, RemoteSideEffect, RemoteEvent>(RemoteState()) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        screenModelScope { _ -> remote.session.collect { session -> updateState { it.copy(session = session) } } }
    }

    override fun onEvent(event: RemoteEvent) {
        when (event) {
            RemoteEvent.TogglePlay -> togglePlay()
            is RemoteEvent.SeekBy -> remote.send(RemoteCommand.SeekBy(event.deltaMs))
            is RemoteEvent.Scrub -> scrub(event.positionMs)
            RemoteEvent.ScrubFinished -> finishScrub()
            RemoteEvent.NextEpisode -> remote.send(RemoteCommand.NextEpisode)
            RemoteEvent.PreviousEpisode -> remote.send(RemoteCommand.PreviousEpisode)
            RemoteEvent.StopOnTv -> remote.send(RemoteCommand.Stop)
            RemoteEvent.Reconnect -> reconnect()
            RemoteEvent.ChooseTv -> screenModelScope { _ -> postSideEffect(RemoteSideEffect.OpenPicker) }
            RemoteEvent.Disconnect -> {
                remote.disconnect()
                screenModelScope { _ -> postSideEffect(RemoteSideEffect.Close) }
            }
        }
    }

    private fun togglePlay() {
        val playback = state.playback ?: return
        remote.send(if (playback.playing) RemoteCommand.Pause else RemoteCommand.Resume)
    }

    private fun scrub(positionMs: Long) {
        screenModelScope { _ -> updateState { it.copy(scrubPositionMs = positionMs) } }
    }

    private fun finishScrub() {
        val target = state.scrubPositionMs ?: return
        remote.send(RemoteCommand.SeekTo(target))
        screenModelScope { _ -> updateState { it.copy(scrubPositionMs = null) } }
    }

    private fun reconnect() {
        val lost = state.session as? RemoteSession.Lost ?: return
        screenModelScope { _ -> remote.connect(lost.device) }
    }
}
