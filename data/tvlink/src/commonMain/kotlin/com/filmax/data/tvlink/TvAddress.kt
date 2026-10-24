package com.filmax.data.tvlink

import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.data.tvlink.protocol.TvLinkProtocol

/**
 * Адрес приёмника строкой: «192.168.1.50» — на стандартном порту, «192.168.1.50:47101» — на другом
 * (стандартный был занят). Так его показывает ТВ, так его вводят на телефоне.
 */
internal object TvAddress {

    fun format(host: String, port: Int): String =
        if (port == TvLinkProtocol.DEFAULT_PORT) host else "$host:$port"

    /** null — строка не похожа на адрес: пустая, с пробелами внутри или с портом вне диапазона. */
    fun parse(text: String): TvDevice? {
        val address = text.trim()
        val host = address.substringBefore(':')
        val port = if (':' in address) address.substringAfter(':').toIntOrNull() else TvLinkProtocol.DEFAULT_PORT
        if (!HOST.matches(host) || port == null || port !in PORTS) return null
        val formatted = format(host, port)
        return TvDevice(id = formatted, name = formatted, host = host, port = port)
    }

    /** Имя хоста или IPv4: буквы, цифры, точки и дефисы. */
    private val HOST = Regex("[A-Za-z0-9.-]+")
    private val PORTS = 1..65_535
}
