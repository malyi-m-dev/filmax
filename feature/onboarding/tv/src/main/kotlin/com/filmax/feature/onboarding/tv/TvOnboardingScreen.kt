package com.filmax.feature.onboarding.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvError
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.feature.onboarding.common.OnboardingEvent
import com.filmax.feature.onboarding.common.OnboardingScreenModel
import com.filmax.feature.onboarding.common.OnboardingSideEffect
import com.filmax.feature.onboarding.common.OnboardingState
import org.koin.androidx.compose.koinViewModel

private const val DEFAULT_VERIFICATION_URI = "kinopub.me/device"

/** Ширина текстового блока. Строка длиннее ~460dp заставляет глаз искать начало следующей. */
private val TextMaxWidth = 460.dp

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
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: OnboardingScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    screenModel.collectSideEffect { effect ->
        when (effect) {
            OnboardingSideEffect.Authenticated -> onAuthenticated()
        }
    }

    // На TV пропускаем промежуточный шаг «фичи» (1) мобильного флоу — сразу к активации.
    LaunchedEffect(state.step) {
        if (state.step == 1) screenModel.dispatch(OnboardingEvent.NextStep)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface),
    ) {
        AnimatedContent(
            targetState = state.step == 2,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
            label = "tv_onboarding_step",
            modifier = Modifier.fillMaxSize(),
        ) { onAuthStep ->
            if (onAuthStep) {
                TvAuthStep(
                    state = state,
                    onRetry = { screenModel.dispatch(OnboardingEvent.RetryDeviceCode) },
                )
            } else {
                TvWelcomeStep(onLogin = { screenModel.dispatch(OnboardingEvent.NextStep) })
            }
        }
    }
}

// ── Step 0: приветствие ───────────────────────────────────────────────────────

@Composable
private fun TvWelcomeStep(onLogin: () -> Unit) {
    val loginFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { loginFocus.requestFocus() }

    CenteredStep {
        Wordmark()
        Spacer(Modifier.height(26.dp))
        Text(
            "Кино и сериалы\nвсегда под рукой",
            style = MaterialTheme.typography.displaySmall,
            color = TvOnSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Тысячи фильмов, сериалов и аниме в одном приложении",
            style = MaterialTheme.typography.bodyLarge,
            color = TvOnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = TextMaxWidth),
        )
        Spacer(Modifier.height(34.dp))
        TvButton(
            text = "Войти",
            onClick = onLogin,
            leadingIcon = Icons.AutoMirrored.Filled.Login,
            focusRequester = loginFocus,
        )
    }
}

// ── Step 2: активация устройства ─────────────────────────────────────────────

@Composable
private fun TvAuthStep(state: OnboardingState, onRetry: () -> Unit) {
    CenteredStep {
        Wordmark()
        Spacer(Modifier.height(26.dp))
        // Локальные копии: smart-cast по полям из другого модуля невозможен.
        val error = state.error
        val userCode = state.userCode
        when {
            error != null -> AuthError(error = error, onRetry = onRetry)
            userCode != null -> AuthCode(
                userCode = userCode,
                verificationUri = state.verificationUri ?: DEFAULT_VERIFICATION_URI,
            )

            else -> GeneratingCode()
        }
    }
}

@Composable
private fun AuthCode(userCode: String, verificationUri: String) {
    ActivationHint(verificationUri = verificationUri)
    Spacer(Modifier.height(34.dp))
    Box(
        modifier = Modifier
            .background(TvSurfaceContainer, TvMetrics.PanelShape)
            .border(1.dp, TvSurfaceContainerHigh, TvMetrics.PanelShape)
            .padding(horizontal = 40.dp, vertical = 22.dp),
    ) {
        // Кегль и моноширинный шрифт — не украшение: код диктуют голосом или переписывают
        // на телефон, поэтому важнее всего различить 0/O и 1/I с дивана.
        Text(
            userCode,
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 10.sp,
            color = TvOnSurface,
        )
    }
    Spacer(Modifier.height(20.dp))
    PollingStatus()
}

/** Подсказка со ссылкой. Адрес поднят до [TvOnSurface] — в монохроме вес и яркость вместо цвета. */
@Composable
private fun ActivationHint(verificationUri: String) {
    val hint = buildAnnotatedString {
        append("Откройте ")
        withStyle(SpanStyle(color = TvOnSurface, fontWeight = FontWeight.SemiBold)) {
            append(verificationUri)
        }
        append(" на телефоне\nи введите код активации")
    }
    Text(
        hint,
        style = MaterialTheme.typography.bodyLarge,
        color = TvOnSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = TextMaxWidth),
    )
}

@Composable
private fun PollingStatus() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            color = TvOnSurfaceVariant,
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
        Text(
            "Ожидаем подтверждение…",
            style = MaterialTheme.typography.bodyLarge,
            color = TvOnSurfaceVariant,
        )
    }
}

@Composable
private fun GeneratingCode() {
    CircularProgressIndicator(color = TvOnSurface, modifier = Modifier.size(40.dp))
    Spacer(Modifier.height(20.dp))
    Text(
        "Генерируем код…",
        style = MaterialTheme.typography.bodyLarge,
        color = TvOnSurfaceVariant,
    )
}

@Composable
private fun AuthError(error: String, onRetry: () -> Unit) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { retryFocus.requestFocus() }

    Text(
        error,
        style = MaterialTheme.typography.bodyLarge,
        color = TvError,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = TextMaxWidth),
    )
    Spacer(Modifier.height(28.dp))
    TvButton(
        text = "Попробовать снова",
        onClick = onRetry,
        leadingIcon = Icons.Filled.Refresh,
        focusRequester = retryFocus,
    )
}

// ── Общее ────────────────────────────────────────────────────────────────────

/** Колонка по центру экрана — единственная раскладка онбординга на обоих шагах. */
@Composable
private fun CenteredStep(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TvMetrics.SafeHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun Wordmark() {
    Text(
        "FILMAX",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 4.sp,
        color = TvOnSurface,
    )
}
