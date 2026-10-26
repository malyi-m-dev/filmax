package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteSession

/**
 * Пульт на телефоне. Сырое — [session], [pairedId], [scrubPositionMs]; всё, что экран рисует
 * ([deviceName], [playbackCaption], [positionMs], [failureMessage]…), модель пересчитывает
 * методами `with*` при каждой смене связи или положения пальца.
 */
data class RemoteState(
    val session: RemoteSession = RemoteSession.Idle,
    /** Id запомненного телевизора; null — телефон ни с одним не знаком. */
    val pairedId: String? = null,
    /**
     * Позиция под пальцем, пока тянут ползунок: снимки с ТВ приходят каждую секунду и иначе
     * дёргали бы его назад. Отпустили — уходит перемотка, и позиция снова с ТВ.
     */
    val scrubPositionMs: Long? = null,
    /** Имя телевизора в шапке; null — телевизор не выбран. */
    val deviceName: String? = null,
    /** Что играет на ТВ; null — нет связи или плеер на ТВ закрыт. */
    val playback: PlaybackStatus? = null,
    /** «Сезон 4 · Серия 7 · Загружается» под названием; null — фильм, и поток идёт. */
    val playbackCaption: String? = null,
    /** Позиция ползунка: под пальцем — своя, иначе — с телевизора. */
    val positionMs: Long = 0,
    /** Почему связь пропала и что делать — человеческим языком; null — связь не терялась. */
    val failureMessage: String? = null,
    /**
     * Связь пропала с запомненным ТВ — телефон вернёт её сам, как только ТВ ответит. ТВ другого
     * аккаунта не вернёт: стучаться к нему бесполезно.
     */
    val reconnectsItself: Boolean = false,
) {
    fun withSession(session: RemoteSession, pairedId: String?): RemoteState =
        copy(session = session, pairedId = pairedId).rendered()

    fun withScrub(positionMs: Long?): RemoteState = copy(scrubPositionMs = positionMs).rendered()

    private fun rendered(): RemoteState {
        val playback = (session as? RemoteSession.Connected)?.playback
        val lost = session as? RemoteSession.Lost
        return copy(
            deviceName = deviceNameOf(session),
            playback = playback,
            playbackCaption = playback?.let(::playbackCaption),
            positionMs = scrubPositionMs ?: playback?.positionMs ?: 0,
            failureMessage = lost?.let { linkFailureMessage(it.failure, it.device.name) },
            reconnectsItself = lost != null && lost.device.id == pairedId && lost.failure != LinkFailure.OtherAccount,
        )
    }
}

private fun deviceNameOf(session: RemoteSession): String? = when (session) {
    is RemoteSession.Connected -> session.device.name
    is RemoteSession.Connecting -> session.device.name
    is RemoteSession.Lost -> session.device.name
    RemoteSession.Idle -> null
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

    /** Отключиться и забыть телевизор: сам он больше не подключится. Пульт закрывается. */
    data object Forget : RemoteEvent
    data object Back : RemoteEvent
}

/** Одноразовых эффектов у пульта нет: выбор ТВ и закрытие ведёт навигатор в модели. */
sealed interface RemoteSideEffect
