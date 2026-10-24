package com.filmax.core.domain.user.model

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String?,
    val avatarUrl: String?,
    val subscription: Subscription?,
    /**
     * Дата регистрации (unix-мс). Посторонний её не знает, поэтому из неё и имени выводится ключ,
     * которым телефон и ТВ одного аккаунта узнают друг друга в сети (data:tvlink).
     */
    val registeredAt: Long? = null,
)

/** Инициалы для аватара: 1–2 заглавные буквы из имени (по словам/точкам). */
fun UserProfile.initials(): String =
    username.split(' ', '.', '_').filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")

data class Subscription(
    val active: Boolean,
    val endsAt: Long?,
    val daysLeft: Int?,
)

data class BookmarkFolder(
    val id: Int,
    val title: String,
    val count: Int,
    val updatedAt: Long?,
)

data class DeviceSettings(
    val id: Int,
    val title: String,
    val supportSsl: Boolean,
    val supportHevc: Boolean,
    val supportHdr: Boolean,
    val support4k: Boolean,
    // «Смешанный плейлист»: сервер отдаёт в одном плейлисте дорожки разного качества.
    // Раньше не хранился в домене (updateDeviceSettings слал 0), теперь — редактируемый тумблер.
    val mixedPlaylist: Boolean,
    val streamingType: Int,
    val serverLocation: Int,
)
