package com.filmax.core.domain.user.model

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String?,
    val avatarUrl: String?,
    val subscription: Subscription?,
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
    val streamingType: Int,
    val serverLocation: Int,
)
