package com.filmax.feature.onboarding.common.di

import com.filmax.feature.onboarding.common.OnboardingScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::OnboardingScreenModel)
}
