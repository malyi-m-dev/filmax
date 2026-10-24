package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.TvLinkReceiver
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold

/**
 * Приёмник телефонов на ТВ, как его видит граф навигации: поднимает приём, пока Filmax на экране,
 * открывает плеер на тайтл, отданный с телефона, и объявляет подключившиеся телефоны. Пауза,
 * перемотка и серии идут мимо него — их исполняет сам плеер.
 */
class TvLinkHostScreenModel(
    private val receiver: TvLinkReceiver,
) : BaseScreenModel<TvLinkHostState, TvLinkHostSideEffect, TvLinkHostEvent>(TvLinkHostState()) {

    private var serving: Job? = null

    init {
        onFetchData()
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            receiver.commands.collect { command -> (command as? RemoteCommand.Play)?.let { openPlayer(it) } }
        }
        screenModelScope { _ -> announcePhones() }
    }

    override fun onEvent(event: TvLinkHostEvent) {
        when (event) {
            TvLinkHostEvent.Shown -> if (serving?.isActive != true) {
                serving = screenModelScope { _ -> receiver.serve() }
            }
            TvLinkHostEvent.Hidden -> {
                serving?.cancel()
                serving = null
            }
        }
    }

    private suspend fun openPlayer(play: RemoteCommand.Play) {
        // Снимок есть только у плеера на экране: значит, новый тайтл встаёт на его место.
        postSideEffect(TvLinkHostSideEffect.OpenPlayer(play, replacePlayer = receiver.playback.value != null))
    }

    /** Новый телефон в списке подключённых — плашка с его именем на [ANNOUNCE_MS]. */
    private suspend fun announcePhones() {
        receiver.state
            .map { it.phones }
            .distinctUntilChanged()
            .runningFold(PhonesSeen()) { seen, phones ->
                PhonesSeen(all = phones, joined = (phones - seen.all.toSet()).firstOrNull())
            }
            .mapNotNull { it.joined }
            .collectLatest { phone ->
                updateState { it.copy(joinedPhone = phone) }
                delay(ANNOUNCE_MS)
                updateState { it.copy(joinedPhone = null) }
            }
    }

    private data class PhonesSeen(val all: List<String> = emptyList(), val joined: String? = null)

    private companion object {
        const val ANNOUNCE_MS = 4_000L
    }
}
