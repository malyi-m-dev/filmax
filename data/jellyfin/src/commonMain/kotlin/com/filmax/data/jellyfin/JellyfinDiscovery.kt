package com.filmax.data.jellyfin

import com.filmax.data.jellyfin.remote.dto.DiscoveryResponseDto
import com.filmax.data.jellyfin.remote.jellyfinJson
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.core.writeText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Поиск Jellyfin в локальной сети: широковещательный UDP-запрос на порт 7359, на который сервер
 * отвечает своим адресом и именем — так находят сервер и официальные клиенты. На ТВ это вход без
 * набора адреса пультом. Ничего не нашлось (роутер режет широковещание, сервер в другой подсети)
 * — пустой список, адрес вводят вручную.
 */
internal class JellyfinDiscovery(private val dispatcher: CoroutineDispatcher) {

    // Граница ошибок поиска: любой сбой сокета значит «никого не нашли», а не падение экрана, и
    // причина сбоя экрану не нужна — ему нечего с ней делать, кроме как предложить ввести адрес.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun find(): List<DiscoveryResponseDto> = try {
        listen()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        emptyList()
    }

    private suspend fun listen(): List<DiscoveryResponseDto> = SelectorManager(dispatcher).use { selector ->
        aSocket(selector).udp().bind(InetSocketAddress(ANY_HOST, 0)) { broadcast = true }.use { socket ->
            socket.send(Datagram(buildPacket { writeText(QUESTION) }, InetSocketAddress(BROADCAST, PORT)))
            val found = mutableListOf<DiscoveryResponseDto>()
            withTimeoutOrNull(LISTEN_MS) {
                while (true) {
                    val answer = socket.receive().packet.readText()
                    runCatching { jellyfinJson.decodeFromString(DiscoveryResponseDto.serializer(), answer) }
                        .getOrNull()
                        ?.takeIf { it.address != null }
                        ?.let(found::add)
                }
            }
            found.distinctBy { it.id ?: it.address }
        }
    }

    private companion object {
        const val QUESTION = "Who is JellyfinServer?"
        const val ANY_HOST = "0.0.0.0"
        const val BROADCAST = "255.255.255.255"
        const val PORT = 7359

        /** Серверы в своей сети отвечают за десятки миллисекунд; дольше ждать — тянуть экран. */
        const val LISTEN_MS = 1_500L
    }
}
