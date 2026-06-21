package com.filmax.feature.onboarding.di

import com.filmax.feature.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}
