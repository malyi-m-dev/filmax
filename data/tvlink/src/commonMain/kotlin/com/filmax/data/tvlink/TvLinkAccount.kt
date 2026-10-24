package com.filmax.data.tvlink

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import com.filmax.data.tvlink.security.LinkCrypto
import com.filmax.data.tvlink.security.TvLinkKey

/**
 * Ключ аккаунта, в который вошло это устройство. Не кэшируется: вышли и вошли в другой аккаунт — и
 * следующий поиск или приём уже идут под новым ключом. Профиль запрашивается раз на сеанс связи.
 */
internal class TvLinkAccount(
    private val users: UserRepository,
    private val crypto: LinkCrypto,
) {

    /** null — профиль не получен (нет входа или сети): узнать свои устройства не по чему. */
    suspend fun key(): TvLinkKey? = when (val profile = users.getProfile()) {
        is RequestResult.Success -> TvLinkKey.of(profile.data, crypto)
        is RequestResult.Error -> null
    }
}
