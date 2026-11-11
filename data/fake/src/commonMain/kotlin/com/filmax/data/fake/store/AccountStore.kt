package com.filmax.data.fake.store

import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.data.fake.fixtures.demoProfile
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Аккаунт на сессию: профиль с подпиской и имя устройства, под которым приложение
 * подписалось после входа. Из профиля меняется только подписка — тестам плашки «нужна подписка».
 */
internal class AccountStore(clock: () -> Long) {

    var profile: UserProfile = demoProfile(now = clock())
        private set

    private val device = MutableStateFlow<String?>(null)

    /** Имя устройства из `device/notify`; null — приложение ещё не подписывалось. */
    val deviceTitle: String? get() = device.value

    /** Подписка кончилась: kino.pub отдаёт её неактивной, без срока. */
    fun expireSubscription() {
        profile = profile.copy(subscription = Subscription(active = false, endsAt = null, daysLeft = null))
    }

    /** `device/notify` после входа: сервер запоминает имя устройства. */
    fun registerDevice(title: String) {
        device.value = title
    }
}
