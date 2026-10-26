package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvRemote
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.flow.combine

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
        screenModelScope { _ ->
            combine(remote.session, remote.paired) { session, paired -> session to paired?.id }
                .collect { (session, pairedId) -> updateState { it.withSession(session, pairedId) } }
        }
    }

    override fun onEvent(event: RemoteEvent) {
        when (event) {
            RemoteEvent.TogglePlay -> onTogglePlay()
            is RemoteEvent.SeekBy -> remote.send(RemoteCommand.SeekBy(event.deltaMs))
            is RemoteEvent.Scrub -> onScrub(event.positionMs)
            RemoteEvent.ScrubFinished -> onScrubFinished()
            RemoteEvent.NextEpisode -> remote.send(RemoteCommand.NextEpisode)
            RemoteEvent.PreviousEpisode -> remote.send(RemoteCommand.PreviousEpisode)
            RemoteEvent.StopOnTv -> remote.send(RemoteCommand.Stop)
            RemoteEvent.Reconnect -> onReconnect()
            RemoteEvent.ChooseTv -> sendSideEffect(RemoteSideEffect.ChooseTv)
            RemoteEvent.Forget -> onForget()
            RemoteEvent.Back -> sendSideEffect(RemoteSideEffect.Close)
        }
    }

    private fun onTogglePlay() {
        val playback = state.playback ?: return
        remote.send(if (playback.playing) RemoteCommand.Pause else RemoteCommand.Resume)
    }

    private fun onScrub(positionMs: Long) {
        screenModelScope { _ -> updateState { it.withScrub(positionMs) } }
    }

    private fun onScrubFinished() {
        val target = state.scrubPositionMs ?: return
        remote.send(RemoteCommand.SeekTo(target))
        screenModelScope { _ -> updateState { it.withScrub(null) } }
    }

    private fun onReconnect() {
        val lost = state.session as? RemoteSession.Lost ?: return
        screenModelScope { _ -> remote.connect(lost.device) }
    }

    private fun onForget() {
        remote.forget()
        sendSideEffect(RemoteSideEffect.Close)
    }
}
