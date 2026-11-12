// Вход на ТВ: модель, развилка шагов и домашний сервер. Сами шаги — приветствие и код устройства —
// в TvOnboardingSteps.
package com.filmax.feature.onboarding.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.sp
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.preview.TvPreviews
import com.filmax.feature.onboarding.common.OnboardingEvent
import com.filmax.feature.onboarding.common.OnboardingSamples
import com.filmax.feature.onboarding.common.OnboardingScreenModel
import com.filmax.feature.onboarding.common.OnboardingSideEffect
import com.filmax.feature.onboarding.common.OnboardingState
import com.filmax.feature.onboarding.common.OnboardingStep
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * TV-экран входа. Центрированная колонка поверх общего [OnboardingScreenModel]: шаг приветствия
 * с одной фокус-кнопкой «Войти», затем экран активации с device-кодом. Поллинг и регистрация
 * устройства живут в общей модели — здесь только вёрстка под пульт.
 *
 * На экране нет ни одной картинки: с 3 метров работает только типографика, а системный
 * emoji-шрифт в крупном кегле на TV-боксах растрируется в кашу.
 */
@Composable
fun TvOnboardingScreen(
    modifier: Modifier = Modifier,
    screenModel: OnboardingScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val onAuthenticated: () -> Unit = { navigator.root(Destination.Home) }
    val state by screenModel.collectAsState()

    screenModel.collectSideEffect { effect ->
        when (effect) {
            OnboardingSideEffect.Authenticated -> onAuthenticated()
        }
    }

    TvOnboardingScreen(state = state, onEvent = screenModel::dispatch, modifier = modifier)
}

/** Вход из состояния — без Koin и навигатора: его рисуют превью. Домашний сервер — свой экран. */
@Composable
internal fun TvOnboardingScreen(
    state: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface),
    ) {
        if (state.homeServer) {
            TvHomeServerStep(onBack = { onEvent(OnboardingEvent.CloseHomeServer) })
            return@Box
        }
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
            label = "tv_onboarding_step",
            modifier = Modifier.fillMaxSize(),
        ) { step ->
            when (step) {
                OnboardingStep.AUTH -> TvAuthStep(
                    state = state,
                    onRetry = { onEvent(OnboardingEvent.RetryDeviceCode) },
                )
                OnboardingStep.WELCOME -> TvWelcomeStep(
                    onLogin = { onEvent(OnboardingEvent.Login) },
                    onHomeServer = { onEvent(OnboardingEvent.OpenHomeServer) },
                )
            }
        }
    }
}

// ── Step 0: приветствие ───────────────────────────────────────────────────────

// ── Step 2: активация устройства ─────────────────────────────────────────────

// ── Общее ────────────────────────────────────────────────────────────────────

@Composable
internal fun Wordmark() {
    Text(
        "FILMAX",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 4.sp,
        color = TvOnSurface,
    )
}

private class OnboardingStatePreviews : PreviewParameterProvider<OnboardingState> {
    override val values = OnboardingSamples.all.asSequence()
}

@TvPreviews
@Composable
private fun TvOnboardingScreenPreview(@PreviewParameter(OnboardingStatePreviews::class) state: OnboardingState) {
    FilmaxTvTheme { TvOnboardingScreen(state = state, onEvent = {}) }
}
