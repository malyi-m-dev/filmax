package com.filmax.data.fake.store

import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.domain.watching.model.Notification
import com.filmax.data.fake.fixtures.DEMO_DEVICE_SETTINGS
import com.filmax.data.fake.fixtures.demoNotifications
import com.filmax.data.fake.fixtures.demoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/**
 * Аккаунт на сессию: профиль с подпиской, настройки устройства и уведомления. Профиль не
 * меняется, остальное правится экранами и видно при следующем чтении.
 */
internal class AccountStore(clock: () -> Long) {

    val profile: UserProfile = demoProfile(now = clock())

    private val device = MutableStateFlow(DEMO_DEVICE_SETTINGS)
    private val notifications = MutableStateFlow(demoNotifications(now = clock()))

    fun deviceSettings(): DeviceSettings = device.value

    /** Сохранение с экрана «Настройки устройства»: id у устройства свой, его форма не меняет. */
    fun updateDeviceSettings(settings: DeviceSettings) {
        device.update { current -> settings.copy(id = current.id) }
    }

    /** `device/notify` после входа: сервер запоминает имя устройства — оно видно в настройках. */
    fun registerDevice(title: String) {
        device.update { it.copy(title = title) }
    }

    fun notifications(): List<Notification> = notifications.value

    /** false — такого уведомления нет. */
    fun markRead(id: Int): Boolean {
        val before = notifications.getAndUpdate { list ->
            list.map { notification -> if (notification.id == id) notification.copy(read = true) else notification }
        }
        return before.any { it.id == id }
    }

    fun markAllRead() {
        notifications.update { list -> list.map { it.copy(read = true) } }
    }
}
