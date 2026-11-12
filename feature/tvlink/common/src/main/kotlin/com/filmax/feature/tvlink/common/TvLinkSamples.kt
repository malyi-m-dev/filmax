// Неизменные образцы пульта и выбора телевизора — для превью телефона.
package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvDevice

private const val PORT = 8765
private const val POSITION_MS = 1_260_000L
private const val DURATION_MS = 3_120_000L

object TvLinkSamples {

    private val livingRoom = TvDevice(id = "tv-1", name = "Гостиная", host = "192.168.1.50", port = PORT)
    private val bedroom = TvDevice(id = "tv-2", name = "Спальня", host = "192.168.1.51", port = PORT)

    private val playback = PlaybackStatus(
        itemId = 1,
        title = "Хроники Пограничья",
        posterUrl = null,
        season = 4,
        episode = 7,
        episodeTitle = "Мост",
        playing = true,
        buffering = false,
        positionMs = POSITION_MS,
        durationMs = DURATION_MS,
        hasNext = true,
        hasPrevious = true,
    )

    /** Играет серия, без связи, связь пропала, другой аккаунт, ТВ ещё не выбран. */
    val remote: List<RemoteState> = listOf(
        RemoteState(session = RemoteSession.Connected(livingRoom, playback), pairedId = livingRoom.id),
        RemoteState(session = RemoteSession.Connected(livingRoom, playback = null), pairedId = livingRoom.id),
        RemoteState(session = RemoteSession.Lost(livingRoom, LinkFailure.Dropped), pairedId = livingRoom.id),
        RemoteState(session = RemoteSession.Lost(bedroom, LinkFailure.OtherAccount)),
        RemoteState(),
    )

    /** Нашлись два ТВ, поиск затянулся, ввод адреса с ошибкой. */
    val picker: List<TvPickerState> = listOf(
        TvPickerState(devices = listOf(livingRoom, bedroom), connectedTo = livingRoom.id, withTitle = true),
        TvPickerState(searchTakesLong = true),
        TvPickerState(manualOpen = true, address = "192.168.1", addressInvalid = true),
    )
}
