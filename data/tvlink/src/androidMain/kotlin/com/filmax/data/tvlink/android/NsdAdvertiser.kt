package com.filmax.data.tvlink.android

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.filmax.data.tvlink.linkTrail
import com.filmax.data.tvlink.platform.LinkAdvert
import com.filmax.data.tvlink.platform.ServiceAdvertiser
import com.filmax.data.tvlink.protocol.TvLinkProtocol
import kotlinx.coroutines.awaitCancellation

/** Объявление ТВ через системный mDNS (NsdManager): разрешений не требует. */
internal class NsdAdvertiser(context: Context) : ServiceAdvertiser {

    private val nsd: NsdManager = context.getSystemService(NsdManager::class.java)

    override suspend fun advertise(advert: LinkAdvert) {
        val registration = Registration()
        nsd.registerService(advert.toServiceInfo(), NsdManager.PROTOCOL_DNS_SD, registration)
        try {
            awaitCancellation()
        } finally {
            registration.unregister(nsd)
        }
    }

    private fun LinkAdvert.toServiceInfo() = NsdServiceInfo().apply {
        // Совпадёт с соседом — mDNS сам переименует в «Гостиная (2)», имя лишь укорачиваем до предела.
        serviceName = name.take(MAX_SERVICE_NAME)
        serviceType = TvLinkProtocol.SERVICE_TYPE
        port = this@toServiceInfo.port
        setAttribute(TvLinkProtocol.ATTR_FINGERPRINT, fingerprint)
        setAttribute(TvLinkProtocol.ATTR_VERSION, TvLinkProtocol.VERSION.toString())
    }

    private class Registration : NsdManager.RegistrationListener {

        /** Регистрация не удалась — снимать нечего, и unregisterService упал бы. */
        @Volatile
        private var failed = false

        fun unregister(nsd: NsdManager) {
            if (!failed) nsd.unregisterService(this)
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            linkTrail("ТВ объявлен в сети как «${serviceInfo.serviceName}»")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            failed = true
            linkTrail("ТВ не объявлен в сети, ошибка NSD $errorCode")
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            linkTrail("объявление ТВ не снято, ошибка NSD $errorCode")
        }
    }

    private companion object {
        /** Предел метки DNS — 63 байта; кириллица в UTF-8 вдвое длиннее, поэтому с запасом. */
        const val MAX_SERVICE_NAME = 30
    }
}
