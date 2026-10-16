package com.filmax.core.domain.user

import com.filmax.core.domain.feedback.ClientInfo

/**
 * Как устройство подписано в списке устройств аккаунта kino.pub (`device/notify`). Без подписи
 * сервер показывает «unknown / unknown / unknown», и свой телевизор среди них не узнать.
 */
data class DeviceDescription(val title: String, val hardware: String, val software: String)

/** «Xiaomi MiTV-MZTU1» · «Android TV» · «Filmax 2.3.2 · Android 14». */
fun ClientInfo.deviceDescription() = DeviceDescription(
    title = device,
    hardware = if (isTv) "Android TV" else "Android",
    software = "Filmax $appVersion · $os",
)
