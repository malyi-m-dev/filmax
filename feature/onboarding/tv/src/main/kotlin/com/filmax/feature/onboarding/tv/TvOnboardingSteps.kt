// Шаги входа на ТВ: приветствие с «Войти» и активация устройства — код крупно, куда его вводить,
// ожидание подтверждения и ошибка с «Повторить». Модель и развилка шагов — в TvOnboardingScreen.
package com.filmax.feature.onboarding.tv

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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.filmax.core.tv.designsystem.TvButtonStyle
import com.filmax.core.tv.designsystem.TvError
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.feature.onboarding.common.OnboardingState

private const val DEFAULT_VERIFICATION_URI = "kinopub.me/device"

/** Ширина текстового блока. Строка длиннее ~460dp заставляет глаз искать начало следующей. */
private val TextMaxWidth = 460.dp

/** «Войти» — kino.pub по коду; «Домашний сервер» — своя медиатека Jellyfin в этой сети. */
@Composable
internal fun TvWelcomeStep(onLogin: () -> Unit, onHomeServer: () -> Unit) {
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
        Row(Modifier.tvFocusGroup(fallback = loginFocus), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvButton(
                text = "Войти",
                onClick = onLogin,
                leadingIcon = Icons.AutoMirrored.Filled.Login,
                focusRequester = loginFocus,
            )
            TvButton(
                text = "Домашний сервер",
                onClick = onHomeServer,
                style = TvButtonStyle.Secondary,
                leadingIcon = Icons.Filled.Dns,
            )
        }
    }
}

@Composable
internal fun TvAuthStep(state: OnboardingState, onRetry: () -> Unit) {
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
    ActivationCode(userCode)
    Spacer(Modifier.height(20.dp))
    PollingStatus()
}

/** Код активации крупно — его же показывает вход на домашний сервер по Quick Connect. */
@Composable
internal fun ActivationCode(userCode: String) {
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
internal fun PollingStatus() {
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
