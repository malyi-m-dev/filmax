package com.filmax.data.tvlink

import com.filmax.data.tvlink.protocol.TvLinkCodec
import com.filmax.data.tvlink.protocol.TvLinkMessage
import com.filmax.data.tvlink.protocol.TvLinkProtocol
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Связь поверх TCP-сокета: сообщения протокола построчно в обе стороны. */
internal class LinkConnection(private val socket: Socket) {

    private val input = socket.openReadChannel()
    private val output = socket.openWriteChannel()

    // Пишут двое — поток команд и поток снимков/пингов; строки не должны перемешаться.
    private val writeLock = Mutex()

    /**
     * Следующее сообщение. null — собеседник закрыл связь или молчал дольше [timeoutMs]. Строки,
     * которых эта версия не понимает, пропускаются.
     */
    suspend fun receive(timeoutMs: Long = TvLinkProtocol.IDLE_TIMEOUT_MS): TvLinkMessage? =
        withTimeoutOrNull(timeoutMs) {
            var message: TvLinkMessage? = null
            while (message == null) {
                val line = input.readUTF8Line(TvLinkProtocol.MAX_LINE_LENGTH) ?: return@withTimeoutOrNull null
                message = TvLinkCodec.decode(line)
            }
            message
        }

    suspend fun send(message: TvLinkMessage) {
        writeLock.withLock {
            output.writeStringUtf8(TvLinkCodec.encode(message) + "\n")
            output.flush()
        }
    }

    fun close() {
        socket.close()
    }
}
