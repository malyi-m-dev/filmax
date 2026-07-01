package com.filmax.data.user

import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.data.user.remote.dto.DeviceInfoDto

internal fun DeviceInfoDto.toDomain() = DeviceSettings(
    id = id,
    title = title,
    supportSsl = supportSsl == 1,
    supportHevc = supportHevc == 1,
    supportHdr = supportHdr == 1,
    support4k = support4k == 1,
    streamingType = streamingType,
    serverLocation = serverLocation,
)
