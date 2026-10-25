package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteSession

data class RemoteState(
    val session: RemoteSession = RemoteSession.Idle,
    /** Id запомненного телевизора; null — телефон ни с одним не знаком. */
    val pairedId: String? = null,
    /**
     * Позиция под пальцем, пока тянут ползунок: снимки с ТВ приходят каждую секунду и иначе
     * дёргали бы его назад. Отпустили — уходит перемотка, и позиция снова с ТВ.
     */
    val scrubPositionMs: Long? = null,
) {
    /** Что играет на ТВ; null — нет связи или плеер на ТВ закрыт. */
    val playback: PlaybackStatus? get() = (session as? RemoteSession.Connected)?.playback

    /** Позиция ползунка: под пальцем — своя, иначе — с телевизора. */
    val positionMs: Long get() = scrubPositionMs ?: playback?.positionMs ?: 0

    /**
     * Связь пропала с запомненным ТВ — телефон вернёт её сам, как только ТВ ответит. ТВ другого
     * аккаунта не вернёт: стучаться к нему бесполезно.
     */
    val reconnectsItself: Boolean
        get() {
            val lost = session as? RemoteSession.Lost ?: return false
            return lost.device.id == pairedId && lost.failure != LinkFailure.OtherAccount
        }
}

sealed interface RemoteEvent {
    data object TogglePlay : RemoteEvent

    /** Шаг перемотки: отрицательный — назад. */
    data class SeekBy(val deltaMs: Long) : RemoteEvent

    /** Ползунок тянут — на экране позиция пальца, на ТВ ещё ничего не происходит. */
    data class Scrub(val positionMs: Long) : RemoteEvent
    data object ScrubFinished : RemoteEvent

    data object NextEpisode : RemoteEvent
    data object PreviousEpisode : RemoteEvent

    /** Закрыть плеер на телевизоре — связь остаётся. */
    data object StopOnTv : RemoteEvent

    /** Связь оборвалась — подключиться к тому же ТВ заново. */
    data object Reconnect : RemoteEvent

    /** Выбрать другой (или первый) телевизор. */
    data object ChooseTv : RemoteEvent

    /** Отключиться и забыть телевизор: сам он больше не подключится. */
    data object Forget : RemoteEvent
}

sealed interface RemoteSideEffect {
    data object OpenPicker : RemoteSideEffect
    data object Close : RemoteSideEffect
}
