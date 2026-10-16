package com.filmax.feature.feedback.common.di

import com.filmax.feature.feedback.common.FeedbackScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** FeedbackPrompts и ClientInfo даёт `:app`: он знает версию приложения и устройство. */
val feedbackFeatureModule = module {
    viewModelOf(::FeedbackScreenModel)
}
