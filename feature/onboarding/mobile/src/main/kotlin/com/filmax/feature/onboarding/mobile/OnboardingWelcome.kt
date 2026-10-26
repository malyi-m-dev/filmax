// Первый шаг входа — приветствие: стена постеров-плейсхолдеров оттенков палитры под скримом,
// текст и «Войти». Переход между шагами и общие логотип и поля — в OnboardingScreen.

package com.filmax.feature.onboarding.mobile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapePoster
import com.filmax.core.designsystem.oklch
import com.filmax.core.designsystem.radialWash
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.POSTER_ASPECT
import com.filmax.feature.onboarding.common.OnboardingEvent
import com.filmax.feature.onboarding.common.OnboardingScreenModel

/** Оттенки плейсхолдеров стены — те же, что у плиток разделов: стена узнаётся как «каталог». */
private val WallHues = listOf(290f, 250f, 330f, 170f, 50f, 200f)

/**
 * Текст прижат к низу — там, где в макете лежит кадр под скримом. Самой картинки нет:
 * онбординг не ходит в каталог (у [OnboardingScreenModel] только auth/user), а показывать
 * случайный тайтл до входа — обещание контента, которого может не оказаться в подписке.
 */
@Composable
internal fun WelcomeStep(onEvent: (OnboardingEvent) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        PosterWallBackdrop(Modifier.fillMaxSize())
        WelcomeContent(onLogin = { onEvent(OnboardingEvent.Login) })
    }
}

/**
 * Фон приветствия — «стена постеров» из цветных плашек. До входа каталог недоступен (нет токена),
 * а тянуть в ассеты чужие постеры конкретных фильмов нельзя — поэтому абстрактные плашки.
 */
@Composable
private fun PosterWallBackdrop(modifier: Modifier = Modifier) {
    val palette = FilmaxTheme.palette
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
            PosterWallColumn(topOffset = 44.dp, seed = 2, modifier = Modifier.weight(1f))
            PosterWallColumn(topOffset = 18.dp, seed = 4, modifier = Modifier.weight(1f))
        }
        // Скрим: сверху лёгкий (стена видна), снизу глухой фон — на нём лежат текст и кнопка.
        Box(
            Modifier
                .fillMaxSize()
                .radialWash(palette.wash, centerX = 0.5f, centerY = 0.35f, radiusX = 0.9f, radiusY = 0.45f)
                .background(
                    Brush.verticalGradient(
                        0f to palette.background.copy(alpha = 0.35f),
                        0.42f to palette.background.copy(alpha = 0.7f),
                        0.72f to palette.background,
                        1f to palette.background,
                    ),
                ),
        )
    }
}

/** Одна вертикальная лента плейсхолдеров. */
@Composable
private fun PosterWallColumn(topOffset: Dp, seed: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.offset(y = topOffset), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(POSTERS_PER_COLUMN) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(POSTER_ASPECT)
                    .clip(ShapePoster)
                    .background(
                        oklch(
                            lightness = 0.42f,
                            chroma = 0.12f,
                            hue = WallHues[(index + seed) % WallHues.size],
                            alpha = 0.7f
                        )
                    ),
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
            style = FilmaxType.collectionTitle,
            color = FilmaxColors.Text,
            modifier = Modifier.padding(top = 22.dp),
        )
        Text(
            "Тысячи тайтлов в оригинале и дубляже. Смотрите на телефоне и на ТВ.",
            style = FilmaxType.body,
            color = FilmaxColors.TextSecondary,
            modifier = Modifier.padding(top = 12.dp),
        )
        FilmaxButton(text = "Войти", onClick = onLogin, modifier = Modifier.padding(top = 28.dp).fillMaxWidth())
        Spacer(Modifier.height(BottomPadding))
    }
}

private val BottomPadding = 44.dp

/** Стена постеров занимает верхнюю часть экрана; ниже её съедает скрим под текст. */
private const val BACKDROP_HEIGHT_FRACTION = 0.72f
private const val POSTERS_PER_COLUMN = 4
