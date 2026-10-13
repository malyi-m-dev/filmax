// Шаг активации устройства: адрес, код активации с копированием, ожидание подтверждения и
// ошибка с повтором. Переход между шагами и общие логотип и поля — в OnboardingScreen.

package com.filmax.feature.onboarding.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.feature.onboarding.common.OnboardingState
import kotlinx.coroutines.delay

@Composable
internal fun AuthStep(state: OnboardingState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = SidePadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark()
        Spacer(Modifier.height(30.dp))
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
    Spacer(Modifier.height(26.dp))
    CodeBox(userCode = userCode)
    Spacer(Modifier.height(16.dp))
    CopyCodeButton(userCode = userCode)
    Spacer(Modifier.height(22.dp))
    PollingStatus()
}

/** Подсказка со ссылкой. Адрес поднят до onSurface — в монохроме вес и яркость вместо цвета. */
@Composable
private fun ActivationHint(verificationUri: String) {
    val hint = buildAnnotatedString {
        append("Откройте ")
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        ) {
            append(verificationUri)
        }
        append("\nи введите код активации")
    }
    Text(
        hint,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Код активации. Кегль и моноширинный шрифт — не украшение: код переписывают в браузер
 * руками, и важнее всего различить 0/O и 1/I.
 */
@Composable
private fun CodeBox(userCode: String) {
    Box(
        modifier = Modifier
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Text(
            userCode,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 6.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CopyCodeButton(userCode: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPY_RESET_DELAY_MILLIS)
            copied = false
        }
    }

    Row(
        modifier = Modifier
            .height(FilmaxMetrics.SecondaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable {
                clipboard.setText(AnnotatedString(userCode))
                copied = true
            }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (copied) "Скопировано" else "Скопировать код",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PollingStatus() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
        )
        Text(
            "Ожидаем подтверждение…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GeneratingCode() {
    CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp),
    )
    Spacer(Modifier.height(18.dp))
    Text(
        "Генерируем код…",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AuthError(error: String, onRetry: () -> Unit) {
    Text(
        error,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Box(
        modifier = Modifier
            .height(FilmaxMetrics.PrimaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onRetry)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Попробовать снова",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private const val DEFAULT_VERIFICATION_URI = "kinopub.me/device"

private const val COPY_RESET_DELAY_MILLIS = 2000L
