package com.filmax.data.tvlink.android

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.filmax.data.tvlink.platform.LocalDevice
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Имя — то, что человек сам дал устройству в настройках («Гостиная», «Galaxy S25 Ultra»); без него —
 * производитель и модель. Адрес — через NetworkInterface: ConnectivityManager потребовал бы
 * разрешение ACCESS_NETWORK_STATE ради одной строки на экране профиля.
 */
internal class AndroidLocalDevice(private val context: Context) : LocalDevice {

    override val name: String
        get() = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: "${Build.MANUFACTURER} ${Build.MODEL}"

    override fun localAddress(): String? = NetworkInterface.getNetworkInterfaces()
        ?.toList()
        .orEmpty()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        // Адрес домашней сети (192.168.x.x, 10.x.x.x), а не публичный адрес мобильного интернета.
        .firstOrNull { it.isSiteLocalAddress }
        ?.hostAddress
}
