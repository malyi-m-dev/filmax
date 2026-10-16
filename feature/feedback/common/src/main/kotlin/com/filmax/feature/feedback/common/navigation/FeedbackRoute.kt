package com.filmax.feature.feedback.common.navigation

import kotlinx.serialization.Serializable

/**
 * Окно оценки. [trigger] — имя [com.filmax.core.domain.feedback.FeedbackTrigger]: строкой, потому
 * что адрес `Destination.Feedback` живёт в core:navigation, который не знает типов domain.
 */
@Serializable
data class FeedbackRoute(val trigger: String)
