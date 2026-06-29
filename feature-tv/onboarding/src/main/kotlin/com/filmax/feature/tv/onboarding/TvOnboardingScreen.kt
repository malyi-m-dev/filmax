package com.filmax.feature.tv.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.designsystem.ShapeAsymA
import com.filmax.core.designsystem.ShapeAsymB
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.feature.onboarding.OnboardingEvent
import com.filmax.feature.onboarding.OnboardingScreenModel
import com.filmax.feature.onboarding.OnboardingSideEffect
import com.filmax.feature.onboarding.OnboardingState
import org.koin.androidx.compose.koinViewModel

/**
 * TV-экран входа. Десятифутовая компоновка поверх общего [OnboardingScreenModel]:
 * шаг приветствия с одной фокус-кнопкой «Войти», затем экран активации с device-кодом
 * (поллинг и регистрация устройства живут в общей модели — здесь только верстка под пульт).
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
            .background(MaterialTheme.colorScheme.background),
    ) {
        TvBlobDecorations()

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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 80.dp, vertical = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left — branding + features + CTA
        Column(modifier = Modifier.weight(1f)) {
            Wordmark()
            Spacer(Modifier.height(28.dp))
            Text(
                "Кино и сериалы\nвсегда под рукой",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Тысячи фильмов, сериалов и аниме в одном приложении",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
            FeatureRow("📺", "Все форматы", "Фильмы, сериалы, аниме, документалки")
            Spacer(Modifier.height(16.dp))
            FeatureRow("🎯", "Умные рекомендации", "Персональная подборка по вашим вкусам")
            Spacer(Modifier.height(16.dp))
            FeatureRow("⚡", "Быстрый стриминг", "HLS адаптивный поток до 4K качества")
            Spacer(Modifier.height(48.dp))
            TvButton(
                text = "Войти",
                onClick = onLogin,
                leadingIcon = Icons.AutoMirrored.Filled.Login,
                focusRequester = loginFocus,
            )
        }

        Spacer(Modifier.width(64.dp))

        // Right — decorative expressive panel
        Box(
            modifier = Modifier
                .size(420.dp)
                .clip(ShapeAsymB)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎬", fontSize = 160.sp)
        }
    }
}

// ── Step 2: активация устройства ────────────────────────────────────────────
@Composable
private fun TvAuthStep(state: OnboardingState, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 80.dp, vertical = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left — instructions
        Column(modifier = Modifier.weight(1f)) {
            Wordmark()
            Spacer(Modifier.height(28.dp))
            Text(
                "Активируйте устройство",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(40.dp))
            AuthStep(1, "Откройте", state.verificationUri ?: DEFAULT_VERIFICATION_URI)
            Spacer(Modifier.height(24.dp))
            AuthStep(2, "Войдите", "в свой аккаунт KinoPub")
            Spacer(Modifier.height(24.dp))
            AuthStep(3, "Введите", "код активации справа")
        }

        Spacer(Modifier.width(64.dp))

        // Right — code card / spinner / error
        Box(
            modifier = Modifier
                .width(520.dp)
                .height(440.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Локальные копии: smart-cast по полям из другого модуля невозможен.
            val error = state.error
            val userCode = state.userCode
            when {
                error != null -> AuthError(error = error, onRetry = onRetry)
                userCode != null -> AuthCodeCard(
                    userCode = userCode,
                    verificationUri = state.verificationUri ?: DEFAULT_VERIFICATION_URI,
                )
                else -> GeneratingCode()
            }
        }
    }
}

@Composable
private fun AuthCodeCard(userCode: String, verificationUri: String) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "borderAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f),
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                )
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(32.dp),
            )
            .padding(horizontal = 48.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "КОД АКТИВАЦИИ",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            userCode,
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 10.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                verificationUri,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
        }
        Spacer(Modifier.height(28.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
            Text(
                "Ожидаем подтверждение…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun GeneratingCode() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Генерируем код…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun AuthError(error: String, onRetry: () -> Unit) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { retryFocus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.30f))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⚠️", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            error,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(28.dp))
        TvButton(
            text = "Попробовать снова",
            onClick = onRetry,
            leadingIcon = Icons.Filled.Refresh,
            focusRequester = retryFocus,
        )
    }
}

// ── Shared pieces ─────────────────────────────────────────────────────────────
@Composable
private fun Wordmark() {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "Filmax",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            ".",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun FeatureRow(emoji: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 28.sp)
        }
        Spacer(Modifier.width(20.dp))
        Column {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                description,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuthStep(number: Int, action: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }
        Spacer(Modifier.width(20.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$action ",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
            )
        }
    }
}

@Composable
private fun TvBlobDecorations() {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .clip(ShapeCookie)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.10f))
                .align(Alignment.TopEnd),
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(ShapeAsymA)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.06f))
                .align(Alignment.BottomStart),
        )
    }
}

private const val DEFAULT_VERIFICATION_URI = "kinopub.me/device"
