package com.filmax.data.user.remote

// Параметры запроса обновления настроек устройства (api/v1/device/{id}/settings).
// Группируют длинный список form-полей в одну модель.
internal data class UpdateDeviceSettingsParams(
    val id: Int,
    val supportSsl: Int,
    val supportHevc: Int,
    val supportHdr: Int,
    val support4k: Int,
    val mixedPlaylist: Int,
    val streamingType: Int,
    val serverLocation: Int,
)
