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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.feature.onboarding.common.OnboardingEvent
import com.filmax.feature.onboarding.common.OnboardingScreenModel
import com.filmax.feature.onboarding.common.OnboardingSideEffect
import com.filmax.feature.onboarding.common.OnboardingState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Вход (экран 01 макета): приветствие с единственным действием «Войти», затем активация
 * устройства по device-flow. Регистрация устройства и поллинг живут в [OnboardingScreenModel] —
 * здесь только вёрстка.
 *
 * Шага «фичи» и индикаторов шагов больше нет: три экрана до входа рассказывали то, что видно
 * на главной за секунду. Промежуточный шаг модели проматываем, чтобы не трогать общий контракт.
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

    LaunchedEffect(state.step) {
        if (state.step == STEP_FEATURES) screenModel.dispatch(OnboardingEvent.NextStep)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AnimatedContent(
            targetState = state.step >= STEP_AUTH,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
            label = "onboarding_step",
            modifier = Modifier.fillMaxSize(),
        ) { onAuthStep ->
            if (onAuthStep) {
                AuthStep(
                    state = state,
                    onRetry = { screenModel.dispatch(OnboardingEvent.RetryDeviceCode) },
                )
            } else {
                WelcomeStep(onLogin = { screenModel.dispatch(OnboardingEvent.NextStep) })
            }
        }
    }
}

@Composable
internal fun Wordmark() {
    Text(
        "FILMAX",
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 5.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** Поля экрана входа шире общих 20dp: на нём один блок текста и ему нужен воздух (макет). */
internal val SidePadding = 30.dp

/** Шаги [OnboardingState]: 1 — «фичи» (проматываем), 2 — активация устройства. */
private const val STEP_FEATURES = 1
private const val STEP_AUTH = 2
