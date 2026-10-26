package com.filmax.feature.profile.common

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.streamingTypeOptions
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

    override fun onEvent(event: DeviceSettingsEvent) {
        when (event) {
            is DeviceSettingsEvent.Edit -> onEdit(event)
            DeviceSettingsEvent.CycleStreamingType -> onCycleStreamingType()
            DeviceSettingsEvent.OpenStreamingSheet -> onStreamingSheet(open = true)
            DeviceSettingsEvent.CloseStreamingSheet -> onStreamingSheet(open = false)
            DeviceSettingsEvent.Save -> onSave()
            DeviceSettingsEvent.Back -> sendSideEffect(DeviceSettingsSideEffect.Close)
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            updateState { it.copy(loading = true, error = null) }
            when (val result = user.getDeviceSettings()) {
                is RequestResult.Success -> updateState { it.copy(loading = false, settings = result.data) }
                is RequestResult.Error -> updateState { it.copy(loading = false, error = result.message) }
            }
        }
    }

    /** Выбор типа потока в шторке телефона закрывает её: вариантов немного, сравнивать нечего. */
    private fun onEdit(event: DeviceSettingsEvent.Edit) {
        screenModelScope { current ->
            val settings = current.settings ?: return@screenModelScope
            val edited = edited(settings, event)
            val sheetOpen = current.streamingSheetOpen && event !is DeviceSettingsEvent.SetStreamingType
            updateState { it.copy(settings = edited, streamingSheetOpen = sheetOpen) }
        }
    }

    private fun edited(settings: DeviceSettings, event: DeviceSettingsEvent.Edit): DeviceSettings = when (event) {
        is DeviceSettingsEvent.SetSsl -> settings.copy(supportSsl = event.enabled)
        is DeviceSettingsEvent.SetHevc -> settings.copy(supportHevc = event.enabled)
        is DeviceSettingsEvent.SetHdr -> settings.copy(supportHdr = event.enabled)
        is DeviceSettingsEvent.Set4k -> settings.copy(support4k = event.enabled)
        is DeviceSettingsEvent.SetMixedPlaylist -> settings.copy(mixedPlaylist = event.enabled)
        is DeviceSettingsEvent.SetStreamingType -> settings.copy(streamingType = event.streamingType)
    }

    private fun onCycleStreamingType() {
        screenModelScope { current ->
            val settings = current.settings ?: return@screenModelScope
            val ids = streamingTypeOptions.map { it.id }
            val next = ids[(ids.indexOf(settings.streamingType) + 1).mod(ids.size)]
            updateState { it.copy(settings = settings.copy(streamingType = next)) }
        }
    }

    private fun onStreamingSheet(open: Boolean) {
        screenModelScope { _ -> updateState { it.copy(streamingSheetOpen = open) } }
    }

    private fun onSave() {
        screenModelScope { current ->
            val settings = current.settings ?: return@screenModelScope
            updateState { it.copy(saving = true, error = null) }
            when (val result = user.updateDeviceSettings(settings)) {
                is RequestResult.Success -> {
                    updateState { it.copy(saving = false) }
                    postSideEffect(DeviceSettingsSideEffect.Close)
                }

                is RequestResult.Error -> updateState { it.copy(saving = false, error = result.message) }
            }
        }
    }
}
