// Неизменные образцы Профиля — для превью телефона и ТВ.
package com.filmax.feature.profile.common

import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile

private const val DAYS_LEFT = 41

object ProfileSamples {

    private val profile = UserProfile(
        id = 1,
        username = "anna.severova",
        email = "anna@example.com",
        avatarUrl = null,
        subscription = Subscription(active = true, endsAt = null, daysLeft = DAYS_LEFT),
    )

    /** kino.pub с активной подпиской. */
    val loaded = ProfileState(loading = false, profile = profile)

    /** Подписка кончилась — карточка зовёт продлить. */
    val expired = loaded.copy(profile = profile.copy(subscription = Subscription(false, null, null)))

    /** Домашний сервер: подписки нет вовсе. */
    val homeServer = loaded.copy(source = ContentSource.JELLYFIN, profile = profile.copy(subscription = null))

    val loading = ProfileState()

    val all: List<ProfileState> = listOf(loaded, expired, homeServer, loading)
}
