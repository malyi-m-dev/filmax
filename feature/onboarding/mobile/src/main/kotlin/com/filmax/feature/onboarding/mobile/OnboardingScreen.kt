package com.filmax.feature.onboarding.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.designsystem.ShapePoster
import com.filmax.feature.onboarding.common.OnboardingEvent
import com.filmax.feature.onboarding.common.OnboardingScreenModel
import com.filmax.feature.onboarding.common.OnboardingSideEffect
import com.filmax.feature.onboarding.common.OnboardingState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

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

// ── Шаг 0: приветствие ────────────────────────────────────────────────────

/**
 * Текст прижат к низу — там, где в макете лежит постер под скримом. Самой картинки нет:
 * онбординг не ходит в каталог (у [OnboardingScreenModel] только auth/user), а показывать
 * случайный тайтл до входа — обещание контента, которого может не оказаться в подписке.
 */
@Composable
private fun WelcomeStep(onLogin: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        PosterWallBackdrop(Modifier.fillMaxSize())
        WelcomeContent(onLogin = onLogin)
    }
}

/**
 * Фон приветствия — приглушённая «стена постеров». До входа каталог недоступен (нет токена),
 * поэтому это единственный способ дать экрану картинку. Плейсхолдеры абстрактные и монохромные:
 * тянуть в ассеты чужие постеры конкретных фильмов нельзя, а приём из макета (кадр за скримом)
 * сохранён.
 */
@Composable
private fun PosterWallBackdrop(modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(BACKDROP_HEIGHT_FRACTION)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Колонки сдвинуты по вертикали — стена смотрится живой, а не таблицей.
            PosterWallColumn(topOffset = 0.dp, seed = 0, modifier = Modifier.weight(1f))
            PosterWallColumn(topOffset = 44.dp, seed = 1, modifier = Modifier.weight(1f))
            PosterWallColumn(topOffset = 18.dp, seed = 2, modifier = Modifier.weight(1f))
        }
        // Скрим: сверху лёгкий (стена видна), снизу глухой surface — на нём лежат текст и кнопка.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to surface.copy(alpha = 0.4f),
                        0.42f to surface.copy(alpha = 0.72f),
                        0.72f to surface,
                        1f to surface,
                    ),
                ),
        )
    }
}

/** Одна вертикальная лента постеров-плейсхолдеров. */
@Composable
private fun PosterWallColumn(topOffset: Dp, seed: Int, modifier: Modifier = Modifier) {
    val tones = listOf(
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Column(
        modifier = modifier.offset(y = topOffset),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(POSTERS_PER_COLUMN) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(POSTER_ASPECT)
                    .clip(ShapePoster)
                    .background(tones[(index + seed) % tones.size].copy(alpha = 0.6f)),
            )
        }
    }
}

@Composable
private fun WelcomeContent(onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = SidePadding),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Wordmark()
        Text(
            "Кино и сериалы\nбез лишнего шума",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 22.dp),
        )
        Text(
            "Тысячи тайтлов в оригинале и дубляже. Смотрите на телефоне и на ТВ.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        LoginButton(onClick = onLogin)
        Spacer(Modifier.height(BottomPadding))
    }
}

/** Главное и единственное действие экрана — единственная белая заливка в монохроме. */
@Composable
private fun LoginButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = 28.dp)
            .fillMaxWidth()
            .height(LoginButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Войти",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

// ── Шаг 2: активация устройства ───────────────────────────────────────────

@Composable
private fun AuthStep(state: OnboardingState, onRetry: () -> Unit) {
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

// ── Общее ─────────────────────────────────────────────────────────────────

@Composable
private fun Wordmark() {
    Text(
        "FILMAX",
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 5.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

// ── Размеры и константы ───────────────────────────────────────────────────

/** Поля экрана входа шире общих 20dp: на нём один блок текста и ему нужен воздух (макет). */
private val SidePadding = 30.dp

private val BottomPadding = 44.dp

/** Кнопка входа выше общей [FilmaxMetrics.PrimaryButtonHeight] на 2dp — из макета. */
private val LoginButtonHeight = 52.dp

/** Стена постеров занимает верхнюю часть экрана; ниже её съедает скрим под текст. */
private const val BACKDROP_HEIGHT_FRACTION = 0.72f
private const val POSTERS_PER_COLUMN = 4
private const val POSTER_ASPECT = 2f / 3f

private const val DEFAULT_VERIFICATION_URI = "kinopub.me/device"

/** Шаги [OnboardingState]: 1 — «фичи» (проматываем), 2 — активация устройства. */
private const val STEP_FEATURES = 1
private const val STEP_AUTH = 2

private const val COPY_RESET_DELAY_MILLIS = 2000L
