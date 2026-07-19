package com.filmax.app.update

import com.filmax.app.BuildConfig
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.Dispatchers

/** Боевой applicationId: только он обновляется release-APK из GitHub Releases. */
private const val RELEASE_APPLICATION_ID = "com.filmax.app"

/** Шаг публикации прогресса: чаще обновлять state незачем — это прыжок на main на каждый чанк. */
private const val PROGRESS_STEP = 0.01f

/**
 * Проверка и скачивание обновлений приложения (GitHub Releases, см. [GitHubUpdateRepository]).
 *
 * Debug и demo стоят под другими applicationId (`.debug`/`.demo`) — для них release-APK не
 * обновление, а вторая установка, поэтому проверка выключена.
 */
class AppUpdateScreenModel(
    private val repository: GitHubUpdateRepository,
) : BaseScreenModel<AppUpdateState, AppUpdateSideEffect, AppUpdateEvent>(AppUpdateState()) {

    init {
        onFetchData()
    }

    override fun dispatch(event: AppUpdateEvent) {
        when (event) {
            AppUpdateEvent.Download -> download()
            AppUpdateEvent.Install -> install()
            AppUpdateEvent.Dismiss -> dismiss()
        }
    }

    override fun onFetchData() {
        if (BuildConfig.APPLICATION_ID != RELEASE_APPLICATION_ID) return
        screenModelScope(Dispatchers.IO) {
            // Ошибки проверки молчаливые: обновление — фон, а не повод для модалки при старте.
            val update = repository.latestUpdate() ?: return@screenModelScope
            updateState { it.copy(update = update) }
        }
    }

    private fun download() {
        screenModelScope(Dispatchers.IO) { snapshot ->
            val update = snapshot.update ?: return@screenModelScope
            updateState { it.copy(downloading = true, progress = 0f, downloadError = false) }
            var lastReported = 0f
            val result = runCatching {
                repository.downloadApk(update) { fraction ->
                    if (fraction - lastReported >= PROGRESS_STEP) {
                        lastReported = fraction
                        updateState { it.copy(progress = fraction) }
                    }
                }
            }
            result.fold(
                onSuccess = { apk ->
                    updateState { it.copy(downloading = false, progress = 1f, downloadedApk = apk) }
                    postSideEffect(AppUpdateSideEffect.LaunchInstaller(apk))
                },
                onFailure = {
                    updateState { it.copy(downloading = false, downloadError = true) }
                },
            )
        }
    }

    private fun install() {
        screenModelScope { snapshot ->
            snapshot.downloadedApk?.let { postSideEffect(AppUpdateSideEffect.LaunchInstaller(it)) }
        }
    }

    private fun dismiss() {
        screenModelScope {
            updateState { it.copy(dismissed = true) }
        }
    }
}
