package com.filmax.data.tvlink.platform

import kotlinx.coroutines.flow.Flow

/** Что ТВ объявляет о себе в сети: имя, порт приёмника и отпечаток аккаунта. */
internal data class LinkAdvert(val name: String, val port: Int, val fingerprint: String)

/**
 * ТВ, найденный в сети. [fingerprint] — отпечаток аккаунта из TXT-записи; null — ТВ его не объявил
 * (сторонний сервис с тем же типом или сбой разбора записи).
 */
internal data class LinkService(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val fingerprint: String?,
)

/** Объявление ТВ в локальной сети (на Android — NsdManager). */
internal fun interface ServiceAdvertiser {

    /** Объявлять, пока корутина жива; отмена снимает объявление. */
    suspend fun advertise(advert: LinkAdvert)
}

/** Поиск ТВ в локальной сети (на Android — NsdManager). */
internal fun interface ServiceBrowser {

    /** Найденные сервисы; поиск идёт, пока поток собирают. */
    fun browse(): Flow<List<LinkService>>
}

/** Это устройство в сети: как его назвать и по какому адресу до него достучаться. */
internal interface LocalDevice {

    /** Имя из настроек устройства («Гостиная», «Galaxy S25») — его видит другая сторона. */
    val name: String

    /** IPv4-адрес в локальной сети; null — Wi-Fi/Ethernet нет. */
    fun localAddress(): String?
}
