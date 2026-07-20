package com.filmax.feature.profile.common

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.presentation.BaseScreenModel

/**
 * Экран «Настройки устройства» (качество/HEVC/HDR/4K/смешанный плейлист/тип потока/сервер раздачи).
 *
 * Общий для mobile и TV: раскладка у платформ своя, а загрузка/редактирование/сохранение — одни.
 * Правки копятся в [DeviceSettingsState.settings] локально и уходят на сервер разом по [Save].
 */
class DeviceSettingsScreenModel(
    private val user: UserRepository,
) : BaseScreenModel<DeviceSettingsState, DeviceSettingsSideEffect, DeviceSettingsEvent>(
    DeviceSettingsState(),
) {

    init {
        onFetchData()
    }

    override fun dispatch(event: DeviceSettingsEvent) {
        when (event) {
            is DeviceSettingsEvent.SetSsl -> edit { it.copy(supportSsl = event.enabled) }
            is DeviceSettingsEvent.SetHevc -> edit { it.copy(supportHevc = event.enabled) }
            is DeviceSettingsEvent.SetHdr -> edit { it.copy(supportHdr = event.enabled) }
            is DeviceSettingsEvent.Set4k -> edit { it.copy(support4k = event.enabled) }
            is DeviceSettingsEvent.SetMixedPlaylist -> edit { it.copy(mixedPlaylist = event.enabled) }
            is DeviceSettingsEvent.SetStreamingType -> edit { it.copy(streamingType = event.streamingType) }
            DeviceSettingsEvent.Save -> save()
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            updateState { it.copy(loading = true, error = null) }
            when (val result = user.getDeviceSettings()) {
                is RequestResult.Success ->
                    updateState { it.copy(loading = false, settings = result.data) }

                is RequestResult.Error ->
                    updateState { it.copy(loading = false, error = result.message) }
            }
        }
    }

    /** Правит рабочую копию настроек; до загрузки (settings == null) события просто игнорируются. */
    private fun edit(transform: (DeviceSettings) -> DeviceSettings) {
        screenModelScope {
            updateState { state ->
                state.settings?.let { state.copy(settings = transform(it)) } ?: state
            }
        }
    }

    private fun save() {
        val settings = state.settings ?: return
        screenModelScope { _ ->
            updateState { it.copy(saving = true, error = null) }
            when (val result = user.updateDeviceSettings(settings)) {
                is RequestResult.Success -> {
                    updateState { it.copy(saving = false) }
                    postSideEffect(DeviceSettingsSideEffect.Saved)
                }

                is RequestResult.Error ->
                    updateState { it.copy(saving = false, error = result.message) }
            }
        }
    }
}
