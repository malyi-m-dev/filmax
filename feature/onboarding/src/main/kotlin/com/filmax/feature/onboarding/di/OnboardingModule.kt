package com.filmax.feature.onboarding.di

import com.filmax.feature.onboarding.OnboardingScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::OnboardingScreenModel)
}
