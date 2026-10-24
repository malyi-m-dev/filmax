// Шаг активации устройства: адрес, код активации с копированием, ожидание подтверждения и
// ошибка с повтором. Переход между шагами и общие логотип и поля — в OnboardingScreen.

package com.filmax.feature.onboarding.mobile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.glassFill
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonSize
import com.filmax.core.ui.components.FilmaxButtonStyle
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

/** Подсказка со ссылкой: адрес — акцентом, его набирают в браузере. */
@Composable
private fun ActivationHint(verificationUri: String) {
    val hint = buildAnnotatedString {
        append("Откройте ")
        withStyle(SpanStyle(color = FilmaxTheme.palette.accentText, fontWeight = FontWeight.SemiBold)) {
            append(verificationUri)
        }
        append("\nи введите код активации")
    }
    Text(hint, style = FilmaxType.body, color = FilmaxColors.TextSecondary, textAlign = TextAlign.Center)
}

/**
 * Код активации. Кегль и моноширинный шрифт — не украшение: код переписывают в браузер
 * руками, и важнее всего различить 0/O и 1/I.
 */
@Composable
private fun CodeBox(userCode: String) {
    Box(
        modifier = Modifier
            .glassFill(CodeShape)
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Text(userCode, style = CodeStyle, color = FilmaxColors.Text)
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
    FilmaxButton(
        text = if (copied) "Скопировано" else "Скопировать код",
        onClick = {
            clipboard.setText(AnnotatedString(userCode))
            copied = true
        },
        icon = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
        style = FilmaxButtonStyle.Secondary,
        size = FilmaxButtonSize.Compact,
    )
}

@Composable
private fun PollingStatus() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(
            color = FilmaxTheme.palette.accent,
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp
        )
        Text("Ожидаем подтверждение…", style = FilmaxType.body, color = FilmaxColors.TextSecondary)
    }
}

@Composable
private fun GeneratingCode() {
    CircularProgressIndicator(color = FilmaxTheme.palette.accent, modifier = Modifier.size(36.dp))
    Spacer(Modifier.height(18.dp))
    Text("Генерируем код…", style = FilmaxType.body, color = FilmaxColors.TextSecondary)
}

@Composable
private fun AuthError(error: String, onRetry: () -> Unit) {
    Text(error, style = FilmaxType.body, color = FilmaxColors.Error, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    FilmaxButton(text = "Попробовать снова", onClick = onRetry, icon = Icons.Filled.Refresh)
}

private const val DEFAULT_VERIFICATION_URI = "kinopub.me/device"
private const val COPY_RESET_DELAY_MILLIS = 2000L

private val CodeShape = RoundedCornerShape(22.dp)
private val CodeStyle = TextStyle(
    fontSize = 40.sp,
    fontWeight = FontWeight.ExtraBold,
    fontFamily = FontFamily.Monospace,
    letterSpacing = 6.sp,
)
