package com.filmax.data.tvlink

import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.core.domain.tvlink.TvDiscovery
import com.filmax.data.tvlink.platform.ServiceBrowser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Поиск по mDNS с отбором по отпечатку аккаунта: в списке только свои телевизоры. */
internal class TvLinkDiscovery(
    private val browser: ServiceBrowser,
    private val account: TvLinkAccount,
) : TvDiscovery {

    override fun devices(): Flow<List<TvDevice>> = flow {
        val fingerprint = account.key()?.fingerprint
        if (fingerprint == null) {
            emit(emptyList())
            return@flow
        }
        val own = browser.browse().map { services ->
            services
                .filter { it.fingerprint == fingerprint }
                .map { TvDevice(id = it.id, name = it.name, host = it.host, port = it.port) }
                .sortedBy { it.name }
        }
        emitAll(own)
    }

    override fun deviceAt(address: String): TvDevice? = TvAddress.parse(address)
}
