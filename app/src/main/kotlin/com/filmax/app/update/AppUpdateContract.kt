package com.filmax.app.update

import java.io.File

/** Свежий релиз из GitHub Releases, который новее установленной версии. */
data class UpdateInfo(
    /** Версия релиза без «v»: `1.4.0`. */
    val version: String,
    /** API-URL APK-ассета (`assets[].url`) — работает и для приватного репозитория. */
    val assetUrl: String,
    /** Размер APK в байтах — для прогресса скачивания. */
    val sizeBytes: Long,
)

data class AppUpdateState(
    /** null — обновления нет (или проверка не прошла), диалог не показывается. */
    val update: UpdateInfo? = null,
    /** Пользователь закрыл диалог «Позже» — до перезапуска приложения больше не предлагаем. */
    val dismissed: Boolean = false,
    val downloading: Boolean = false,
    /** Прогресс скачивания 0..1. */
    val progress: Float = 0f,
    /** Скачанный APK готов к установке. */
    val downloadedApk: File? = null,
    /** Ошибка скачивания — показывается в диалоге с кнопкой «Повторить». */
    val downloadError: Boolean = false,
)

sealed interface AppUpdateEvent {
    /** Скачать APK свежего релиза. */
    data object Download : AppUpdateEvent

    /** Запустить установку уже скачанного APK. */
    data object Install : AppUpdateEvent

    /** «Позже» — спрятать диалог до следующего запуска. */
    data object Dismiss : AppUpdateEvent
}

sealed interface AppUpdateSideEffect {
    /** Открыть системный установщик пакетов для [apk]. */
    data class LaunchInstaller(val apk: File) : AppUpdateSideEffect
}
