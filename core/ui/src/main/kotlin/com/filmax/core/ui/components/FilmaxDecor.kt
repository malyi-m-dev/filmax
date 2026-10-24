package com.filmax.core.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassPanel
import com.filmax.core.designsystem.GlassState
import com.filmax.core.designsystem.ShapeTile
import com.filmax.core.designsystem.glassFill
import com.filmax.core.designsystem.glow

/**
 * Плитка действия под заголовком деталей: «Трейлер», «Буду смотреть», «В папку», «На ТВ».
 * [active] — действие уже совершено («В списке»): иконка залита, подпись — акцентом.
 */
@Composable
fun FilmaxActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val tint = if (active) FilmaxTheme.palette.accentText else FilmaxColors.Text
    Column(
        modifier
            .height(72.dp)
            .glassFill(ShapeTile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            label,
            style = FilmaxType.tag,
            color = if (active) tint else FilmaxColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Точки карусели: текущая — вытянутая акцентная, остальные — светлые кружки. */
@Composable
fun FilmaxPageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    if (count < 2) return
    val palette = FilmaxTheme.palette
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 22.dp else 6.dp, label = "dotWidth")
            val color by animateColorAsState(if (selected) palette.accent else DotColor, label = "dotColor")
            Box(
                Modifier
                    .size(width = width, height = 6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/**
 * Аватар с инициалами: заливка вторым акцентом, зазор цвета фона и тонкое кольцо, мягкое свечение.
 * Фото у kino.pub нет — инициалы и есть лицо аккаунта.
 */
@Composable
fun FilmaxAvatar(initials: String, modifier: Modifier = Modifier, size: Dp = 92.dp) {
    val palette = FilmaxTheme.palette
    Box(
        modifier
            .size(size + RingGap * 2)
            .glow(
                palette.accent2.copy(alpha = AVATAR_GLOW_ALPHA),
                CircleShape,
                blur = 40.dp,
                spread = 0.dp,
                offsetY = 8.dp
            )
            .border(2.dp, palette.accent2.copy(alpha = RING_ALPHA), CircleShape)
            .padding(RingGap)
            .clip(CircleShape)
            .background(palette.accent2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = if (size >= LargeAvatar) FilmaxType.headline else FilmaxType.chip,
            color = palette.onAccent2,
            maxLines = 1,
        )
    }
}

/**
 * Стеклянная шапка раздела поверх ленты: под статус-баром, с тонкой линией снизу. [glass] — лента
 * под шапкой: шапка её размывает, когда та уезжает под неё.
 */
@Composable
fun FilmaxGlassHeader(glass: GlassState?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(state = glass, modifier = modifier.fillMaxWidth(), border = Color.Transparent) {
        Column(Modifier.fillMaxWidth().statusBarsPadding(), content = content)
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(HeaderLine),
        )
    }
}

/**
 * Кадр hero с медленным «Кен Бёрнсом»: приближение и сдвиг туда-обратно за [periodMillis]. Анимация
 * читается в слое отрисовки — кадр не перекомпонуется каждый тик. Выключена, когда анимации в
 * системе отключены (специальные возможности) или [running] = false, — тогда кадр просто стоит.
 */
@Composable
fun KenBurnsImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    running: Boolean = true,
    periodMillis: Int = KEN_BURNS_MILLIS,
) {
    val animate = running && rememberAnimationsEnabled()
    val transition = rememberInfiniteTransition(label = "kenBurns")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Reverse),
        label = "kenBurnsProgress",
    )
    PosterImage(
        url = url,
        contentDescription = contentDescription,
        shape = RectangleShape,
        // Приближение не должно вылезать за кадр: hero и шапка деталей стоят вплотную к ленте.
        modifier = modifier.clipToBounds().graphicsLayer {
            val step = if (animate) progress.value else 0f
            val scale = 1f + KEN_BURNS_ZOOM * step
            scaleX = scale
            scaleY = scale
            translationX = -size.width * KEN_BURNS_SHIFT * step
            translationY = -size.height * KEN_BURNS_SHIFT * step
        },
    )
}

/** Включены ли анимации в системе: «Удалить анимацию» в спецвозможностях ставит масштаб 0. */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

private val DotColor = Color.White.copy(alpha = 0.35f)
private val HeaderLine = Color.White.copy(alpha = 0.1f)
private val RingGap = 4.dp
private val LargeAvatar = 64.dp
private const val RING_ALPHA = 0.6f
private const val AVATAR_GLOW_ALPHA = 0.5f
private const val KEN_BURNS_MILLIS = 18_000
private const val KEN_BURNS_ZOOM = 0.12f
private const val KEN_BURNS_SHIFT = 0.03f

/**
 * Логотип Filmax: точка акцента со свечением и слово. [large] — на экране входа, где он один на
 * экране; в шапке Главной — обычный.
 */
@Composable
fun FilmaxWordmark(modifier: Modifier = Modifier, large: Boolean = false) {
    val palette = FilmaxTheme.palette
    val dot = if (large) 16.dp else 12.dp
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(if (large) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(dot)
                .glow(palette.glow, CircleShape, blur = dot + 2.dp, spread = 0.dp, offsetY = 0.dp)
                .clip(CircleShape)
                .background(palette.accent),
        )
        Text(
            "Filmax",
            style = if (large) FilmaxType.logoLarge else FilmaxType.logo,
            color = FilmaxColors.Text,
        )
    }
}

/**
 * Стекло под статус-баром для экрана без шапки (Профиль, папка): лента уходит под часы и иконки
 * системы — без подложки они сливались бы с текстом. Видно, только пока лента прокручена ([visible]).
 */
@Composable
fun FilmaxStatusBarGlass(glass: GlassState, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        FilmaxGlassHeader(glass = glass) {}
    }
}
