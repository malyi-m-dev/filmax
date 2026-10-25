package com.filmax.data.tvlink

import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.data.tvlink.protocol.TvLinkMessage
import com.filmax.data.tvlink.protocol.TvLinkProtocol
import com.filmax.data.tvlink.protocol.toDomain
import com.filmax.data.tvlink.protocol.toMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Связь телефона после рукопожатия: команды — телевизору (а без них — ping), снимки плеера с ТВ — в
 * [onPlayback] (null — плеер закрыт). Держится, пока ТВ не замолчит дольше тайм-аута.
 */
internal suspend fun LinkConnection.exchangeAsPhone(
    commands: Channel<RemoteCommand>,
    onPlayback: (PlaybackStatus?) -> Unit,
) = coroutineScope {
    val sender = launch {
        while (true) {
            val command = withTimeoutOrNull(TvLinkProtocol.HEARTBEAT_MS) { commands.receive() }
            send(command?.toMessage() ?: TvLinkMessage.Ping)
        }
    }
    var message = receive()
    while (message != null) {
        when (message) {
            is TvLinkMessage.Status -> onPlayback(message.toDomain())
            TvLinkMessage.Idle -> onPlayback(null)
            else -> Unit
        }
        message = receive()
    }
    sender.cancel()
}
