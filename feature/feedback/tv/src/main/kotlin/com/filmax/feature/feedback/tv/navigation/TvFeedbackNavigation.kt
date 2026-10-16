package com.filmax.feature.feedback.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.feedback.common.navigation.FeedbackRoute
import com.filmax.feature.feedback.tv.TvFeedbackScreen

/** Окно оценки — экран на общем [FeedbackRoute] (его читает FeedbackScreenModel). */
fun NavGraphBuilder.tvFeedbackScreen() {
    composable<FeedbackRoute> { TvFeedbackScreen() }
}
