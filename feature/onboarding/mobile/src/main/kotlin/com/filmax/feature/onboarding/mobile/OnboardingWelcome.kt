// Первый шаг входа — приветствие: стена постеров-плейсхолдеров под скримом, текст и «Войти».
// Переход между шагами и общие логотип и поля — в OnboardingScreen.

package com.filmax.feature.onboarding.mobile

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.designsystem.ShapePoster
import com.filmax.feature.onboarding.common.OnboardingScreenModel

/**
 * Текст прижат к низу — там, где в макете лежит постер под скримом. Самой картинки нет:
 * онбординг не ходит в каталог (у [OnboardingScreenModel] только auth/user), а показывать
 * случайный тайтл до входа — обещание контента, которого может не оказаться в подписке.
 */
@Composable
internal fun WelcomeStep(onLogin: () -> Unit) {
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

private val BottomPadding = 44.dp

/** Кнопка входа выше общей [FilmaxMetrics.PrimaryButtonHeight] на 2dp — из макета. */
private val LoginButtonHeight = 52.dp

/** Стена постеров занимает верхнюю часть экрана; ниже её съедает скрим под текст. */
private const val BACKDROP_HEIGHT_FRACTION = 0.72f
private const val POSTERS_PER_COLUMN = 4
private const val POSTER_ASPECT = 2f / 3f
