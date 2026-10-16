package com.filmax.feature.feedback.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.filmax.feature.feedback.common.navigation.FeedbackRoute
import com.filmax.feature.feedback.mobile.FeedbackDialog

/** Окно оценки — диалогом поверх текущего экрана: после досмотра под ним остаётся детали тайтла. */
fun NavGraphBuilder.feedbackDialog() {
    dialog<FeedbackRoute> { FeedbackDialog() }
}
