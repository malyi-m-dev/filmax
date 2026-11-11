package com.filmax.feature.profile.common.di

import com.filmax.feature.profile.common.DeviceSettingsScreenModel
import com.filmax.feature.profile.common.ProfileScreenModel
import com.filmax.feature.profile.common.ProfileSettings
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    factoryOf(::ProfileSettings)
    viewModelOf(::ProfileScreenModel)
    viewModelOf(::DeviceSettingsScreenModel)
}
