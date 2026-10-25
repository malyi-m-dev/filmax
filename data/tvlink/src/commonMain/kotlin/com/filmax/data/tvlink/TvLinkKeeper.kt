package com.filmax.data.tvlink

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.data.tvlink.platform.ServiceBrowser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Связь с запомненным ТВ: пропала — вернуть. Отдельно от [TvLinkClient], потому что это политика
 * повторов поверх связи, а не сама связь: клиент умеет подключиться один раз, хранитель решает,
 * когда и куда пробовать снова.
 */
internal class TvLinkKeeper(
    private val client: TvLinkClient,
    private val browser: ServiceBrowser,
) {

    /** Держать связь, пока корутина жива; другой запомненный ТВ — начать заново с ним. */
    suspend fun keep() {
        // По id, а не по устройству целиком: новый адрес того же ТВ — не повод начинать заново.
        client.paired.map { it?.id }.distinctUntilChanged().collectLatest { tvId ->
            if (tvId != null) keepLinkTo(tvId)
        }
    }

    private suspend fun keepLinkTo(tvId: String) {
        while (true) {
            client.session.first { it is RemoteSession.Idle || it is RemoteSession.Lost }
            if (!regain(tvId)) return
        }
    }

    /**
     * Вернуть связь с [tvId]: сразу, потом с паузами 2 → 4 → 8 → 15 с, а ТВ, появившийся в mDNS (на нём
     * открыли Filmax или роутер выдал ему новый адрес), — без ожидания и по свежему адресу. true —
     * связь есть; false — стучаться бесполезно: ТВ вошёл в другой аккаунт или его забыли.
     */
    private suspend fun regain(tvId: String): Boolean = coroutineScope {
        val sightings = Channel<TvDevice>(Channel.CONFLATED)
        val watcher = launch { sightingsOf(tvId).collect { sightings.send(it) } }
        var pause = RETRY_FIRST_MS
        var sighted: TvDevice? = null
        var regained: Boolean? = null
        while (regained == null) {
            // ТВ забыли, пока ждали, — возвращать нечего.
            val target = sighted ?: client.paired.value?.takeIf { it.id == tvId } ?: break
            regained = tryLink(target)
            if (regained == null) {
                sighted = withTimeoutOrNull(pause) { sightings.receive() }
                pause = (pause * 2).coerceAtMost(RETRY_MAX_MS)
            }
        }
        watcher.cancel()
        regained ?: false
    }

    /** Одна попытка: true — связь есть, false — повторять бесполезно (чужой аккаунт), null — пробовать ещё. */
    private suspend fun tryLink(target: TvDevice): Boolean? {
        // Первая попытка после запуска — с «Подключаемся…», повторы после обрыва — тихо.
        if (client.connect(target, quiet = client.session.value is RemoteSession.Lost)) return true
        val failure = (client.session.value as? RemoteSession.Lost)?.failure
        return if (failure == LinkFailure.OtherAccount) false else null
    }

    /** Запомненный ТВ в mDNS — каждый раз, как он появляется в сети или меняет адрес. */
    private fun sightingsOf(tvId: String): Flow<TvDevice> = browser.browse()
        .map { services -> services.firstOrNull { it.id == tvId } }
        .distinctUntilChanged()
        .filterNotNull()
        .map { TvDevice(id = it.id, name = it.name, host = it.host, port = it.port) }

    private companion object {
        /**
         * Повтор к выключенному ТВ: через 2 с, дальше реже — до раза в 15 с. Где роутер режет mDNS,
         * вернувшийся ТВ замечают только повтором, и ждать его дольше уже заметно.
         */
        const val RETRY_FIRST_MS = 2_000L
        const val RETRY_MAX_MS = 15_000L
    }
}
