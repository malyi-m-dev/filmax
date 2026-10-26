// Вход: состояние экрана, переход между шагами и общее для них — логотип и поля. Приветствие
// со стеной постеров — в OnboardingWelcome, активация устройства по коду — в OnboardingAuth.

package com.filmax.feature.onboarding.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.ui.components.FilmaxWordmark
import com.filmax.feature.onboarding.common.OnboardingEvent
import com.filmax.feature.onboarding.common.OnboardingScreenModel
import com.filmax.feature.onboarding.common.OnboardingStep
import org.koin.androidx.compose.koinViewModel

/**
 * Вход (экран 01 макета): приветствие с единственным действием «Войти», затем активация
 * устройства по device-flow. Регистрация устройства, поллинг и переход на Главную живут в
 * [OnboardingScreenModel] — здесь только вёрстка.
 */
@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, screenModel: OnboardingScreenModel = koinViewModel()) {
    val state by screenModel.collectAsState()
    val onEvent: (OnboardingEvent) -> Unit = screenModel::dispatch

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FilmaxTheme.palette.background),
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
            label = "onboarding_step",
            modifier = Modifier.fillMaxSize(),
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(onEvent = onEvent)
                OnboardingStep.AUTH -> AuthStep(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
internal fun Wordmark() {
    FilmaxWordmark(large = true)
}

/** Поля экрана входа шире общих 20dp: на нём один блок текста и ему нужен воздух (макет). */
internal val SidePadding = 30.dp
