package com.filmax.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

private const val APK_MIME = "application/vnd.android.package-archive"

/** Authority FileProvider из манифеста: `${applicationId}.updates`. */
private fun updateAuthority(context: Context): String = "${context.packageName}.updates"

/**
 * Запускает системный установщик для скачанного APK.
 *
 * Без права «установка из неизвестных источников» сначала открывает его настройку для нашего
 * пакета: диалог обновления остаётся на экране, и после выдачи права пользователь жмёт
 * «Установить» ещё раз — APK уже скачан, повторного скачивания не будет.
 */
fun installApk(context: Context, apk: File) {
    if (!context.packageManager.canRequestPackageInstalls()) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return
    }
    val apkUri = FileProvider.getUriForFile(context, updateAuthority(context), apk)
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
