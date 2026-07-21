package com.filmax.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Подпись версии приложения («Filmax 1.2.3») для низа Профиля и экранов настроек.
 * versionName читается из PackageManager: BuildConfig app-модуля фичам недоступен,
 * а туда версия попадает из git-тега при сборке. [color] отдан вызывающему — у mobile
 * и TV разные палитры приглушённого текста.
 */
@Composable
fun FilmaxVersionLabel(color: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Свой пакет всегда установлен, но контракт метода — throws NameNotFoundException;
    // на гипотетический сбой просто не показываем подпись.
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
            .getOrDefault("")
    }
    if (versionName.isEmpty()) return
    Text(
        "Filmax $versionName",
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier,
    )
}
