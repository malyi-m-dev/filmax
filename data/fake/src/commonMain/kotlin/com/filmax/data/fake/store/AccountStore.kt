package com.filmax.data.fake.store

import com.filmax.core.domain.user.model.UserProfile
import com.filmax.data.fake.fixtures.demoProfile
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Аккаунт на сессию: профиль с подпиской и имя устройства, под которым приложение
 * подписалось после входа. Профиль не меняется.
 */
internal class AccountStore(clock: () -> Long) {

    val profile: UserProfile = demoProfile(now = clock())

    private val device = MutableStateFlow<String?>(null)

    /** Имя устройства из `device/notify`; null — приложение ещё не подписывалось. */
    val deviceTitle: String? get() = device.value

    /** `device/notify` после входа: сервер запоминает имя устройства. */
    fun registerDevice(title: String) {
        device.value = title
    }
}
