package com.filmax.app.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.filmax.app.BuildConfig
import org.koin.androidx.compose.koinViewModel

/**
 * Диалог обновления приложения поверх любого графа (телефон и TV): «Доступна версия X.Y.Z» →
 * скачивание с прогрессом → системный установщик. Появляется только когда GitHub Releases
 * отдал релиз новее установленного, «Позже» прячет его до следующего запуска.
 *
 * Компоненты material3 намеренно: диалог общий для двух дизайн-систем, а фокус на кнопках
 * диалога пульт получает штатно — Dialog перехватывает весь ввод.
 */
@Composable
fun AppUpdatePrompt(screenModel: AppUpdateScreenModel = koinViewModel()) {
    val state by screenModel.collectAsState()
    val context = LocalContext.current

    screenModel.collectSideEffect { effect ->
        when (effect) {
            is AppUpdateSideEffect.LaunchInstaller -> installApk(context, effect.apk)
        }
    }

    val update = state.update
    if (update == null || state.dismissed) return

    AlertDialog(
        onDismissRequest = { if (!state.downloading) screenModel.dispatch(AppUpdateEvent.Dismiss) },
        title = { Text("Доступна версия ${update.version}") },
        text = { UpdateDialogBody(state) },
        confirmButton = { UpdateConfirmButton(state, onEvent = screenModel::dispatch) },
        dismissButton = {
            if (!state.downloading) {
                TextButton(onClick = { screenModel.dispatch(AppUpdateEvent.Dismiss) }) {
                    Text("Позже")
                }
            }
        },
    )
}

@Composable
private fun UpdateDialogBody(state: AppUpdateState) {
    Column {
        when {
            state.downloading -> {
                Text("Скачивание обновления…")
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }

            state.downloadError -> Text("Не удалось скачать обновление. Проверьте сеть и повторите.")

            state.downloadedApk != null -> Text("Обновление скачано — осталось установить.")

            else -> Text("Установлена ${BuildConfig.VERSION_NAME}. Скачать и установить обновление?")
        }
    }
}

@Composable
private fun UpdateConfirmButton(state: AppUpdateState, onEvent: (AppUpdateEvent) -> Unit) {
    if (state.downloading) return
    val (label, event) = when {
        state.downloadedApk != null -> "Установить" to AppUpdateEvent.Install
        state.downloadError -> "Повторить" to AppUpdateEvent.Download
        else -> "Обновить" to AppUpdateEvent.Download
    }
    TextButton(onClick = { onEvent(event) }) {
        Text(label)
    }
}
