// Общие для mobile и tv подписи шапки Профиля: инициалы аватара и строка подписки.
// Чистые функции без UI-зависимостей.

package com.filmax.feature.profile.common

import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.domain.user.model.initials

/** Инициалы для аватара; профиль ещё не загружен или имя пустое — «?». */
fun UserProfile?.initialsOrFallback(): String = this?.initials()?.ifEmpty { "?" } ?: "?"

/** Строка подписки под именем: Premium с остатком дней, просто Premium или бесплатный аккаунт. */
fun Subscription?.label(): String = when {
    this?.active == true && daysLeft != null -> "Filmax Premium · ещё $daysLeft дн."
    this?.active == true -> "Filmax Premium"
    else -> "Бесплатный аккаунт"
}
