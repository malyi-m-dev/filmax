// Вход: состояние экрана, переход между шагами и общее для них — логотип и поля. Приветствие
// со стеной постеров — в OnboardingWelcome, активация устройства по коду — в OnboardingAuth,
// вход на домашний сервер — в HomeServerLogin.

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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxWordmark
import com.filmax.core.ui.preview.PhonePreviews
import com.filmax.feature.onboarding.common.OnboardingEvent
import com.filmax.feature.onboarding.common.OnboardingSamples
import com.filmax.feature.onboarding.common.OnboardingScreenModel
import com.filmax.feature.onboarding.common.OnboardingSideEffect
import com.filmax.feature.onboarding.common.OnboardingState
import com.filmax.feature.onboarding.common.OnboardingStep
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Вход (экран 01 макета): приветствие с единственным действием «Войти», затем активация
 * устройства по device-flow. Регистрация устройства и поллинг живут в [OnboardingScreenModel] —
 * здесь только вёрстка.
 */
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    screenModel: OnboardingScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            // Главная становится единственным корнем: «Назад» с неё закрывает приложение,
            // а не возвращает ко входу.
            OnboardingSideEffect.Authenticated -> navigator.root(Destination.Home)
        }
    }
    OnboardingScreen(state = state, onEvent = screenModel::dispatch, modifier = modifier)
}

/**
 * Вход из состояния — без Koin и навигатора: его рисуют превью. Вход на домашний сервер — свой
 * экран со своей моделью (HomeServerLogin), здесь только развилка на него.
 */
@Composable
internal fun OnboardingScreen(
    state: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FilmaxTheme.palette.background),
    ) {
        if (state.homeServer) {
            HomeServerLogin(onBack = { onEvent(OnboardingEvent.CloseHomeServer) })
            return@Box
        }
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
            label = "onboarding_step",
            modifier = Modifier.fillMaxSize(),
        ) { step ->
            when (step) {
                OnboardingStep.AUTH -> AuthStep(
                    state = state,
                    onRetry = { onEvent(OnboardingEvent.RetryDeviceCode) },
                )
                OnboardingStep.WELCOME -> WelcomeStep(
                    onLogin = { onEvent(OnboardingEvent.Login) },
                    onHomeServer = { onEvent(OnboardingEvent.OpenHomeServer) },
                )
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

private class OnboardingStatePreviews : PreviewParameterProvider<OnboardingState> {
    override val values = OnboardingSamples.all.asSequence()
}

@PhonePreviews
@Composable
private fun OnboardingScreenPreview(@PreviewParameter(OnboardingStatePreviews::class) state: OnboardingState) {
    FilmaxTheme { OnboardingScreen(state = state, onEvent = {}) }
}
