package com.filmax.data.tvlink.android

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.filmax.data.tvlink.linkTrail
import com.filmax.data.tvlink.platform.LinkService
import com.filmax.data.tvlink.platform.ServiceBrowser
import com.filmax.data.tvlink.protocol.TvLinkProtocol
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.InetAddress
import kotlin.coroutines.resume

/**
 * Поиск ТВ через системный mDNS. Найденный сервис ещё надо «разрешить» до адреса и TXT-записи;
 * разрешаем по одному: до Android 14 NsdManager не берёт второе разрешение, пока идёт первое.
 */
internal class NsdBrowser(context: Context) : ServiceBrowser {

    private val nsd: NsdManager = context.getSystemService(NsdManager::class.java)

    override fun browse(): Flow<List<LinkService>> = discoveryEvents()
        .runningFold(emptyMap<String, LinkService>()) { found, event ->
            when (event) {
                is BrowseEvent.Found -> resolve(event.info)?.let { found + (it.id to it) } ?: found
                is BrowseEvent.Lost -> found - event.info.serviceName
            }
        }
        .map { it.values.toList() }

    private fun discoveryEvents(): Flow<BrowseEvent> = callbackFlow {
        val discovery = Discovery { event -> trySend(event) }
        nsd.discoverServices(TvLinkProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discovery)
        awaitClose { discovery.stop(nsd) }
    }
        // Пока разрешается один сервис, остальные события ждут, а не теряются.
        .buffer(Channel.UNLIMITED)

    // resolveService устарел в Android 14 ради registerServiceInfoCallback, но тот есть только с 14-го,
    // а minSdk — 26: на старых ТВ-приставках другого способа нет.
    @Suppress("DEPRECATION")
    private suspend fun resolve(info: NsdServiceInfo): LinkService? = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            nsd.resolveService(
                info,
                object : NsdManager.ResolveListener {
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        continuation.resume(serviceInfo.toLinkService())
                    }

                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        linkTrail("ТВ «${serviceInfo.serviceName}» не разрешён, ошибка NSD $errorCode")
                        continuation.resume(null)
                    }
                },
            )
        }
    }

    private fun NsdServiceInfo.toLinkService(): LinkService? {
        val address = preferredAddress()?.hostAddress ?: return null
        return LinkService(
            id = serviceName,
            name = serviceName,
            host = address,
            port = port,
            fingerprint = attributes[TvLinkProtocol.ATTR_FINGERPRINT]?.decodeToString(),
        )
    }

    /** IPv4, если есть: адрес IPv6 с зоной интерфейса («fe80::…%wlan0») подключается не везде. */
    // host устарел в Android 14 ради hostAddresses, которого до 14-го нет.
    @Suppress("DEPRECATION")
    private fun NsdServiceInfo.preferredAddress(): InetAddress? {
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hostAddresses
        } else {
            listOfNotNull(host)
        }
        return addresses.firstOrNull { it is Inet4Address } ?: addresses.firstOrNull()
    }

    private sealed interface BrowseEvent {
        val info: NsdServiceInfo

        data class Found(override val info: NsdServiceInfo) : BrowseEvent
        data class Lost(override val info: NsdServiceInfo) : BrowseEvent
    }

    private class Discovery(private val onEvent: (BrowseEvent) -> Unit) : NsdManager.DiscoveryListener {

        /** Поиск не запустился — останавливать нечего, и stopServiceDiscovery упал бы. */
        @Volatile
        private var failed = false

        fun stop(nsd: NsdManager) {
            if (!failed) nsd.stopServiceDiscovery(this)
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) = onEvent(BrowseEvent.Found(serviceInfo))

        override fun onServiceLost(serviceInfo: NsdServiceInfo) = onEvent(BrowseEvent.Lost(serviceInfo))

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            failed = true
            linkTrail("поиск ТВ не запустился, ошибка NSD $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            linkTrail("поиск ТВ не остановился, ошибка NSD $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String) = Unit

        override fun onDiscoveryStopped(serviceType: String) = Unit
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MS = 5_000L
    }
}
