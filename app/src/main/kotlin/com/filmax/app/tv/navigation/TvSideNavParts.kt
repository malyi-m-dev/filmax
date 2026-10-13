@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.app.tv.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.filmax.app.navigation.AccountSummary
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.Onest
import com.filmax.core.tv.designsystem.TvFocus
import com.filmax.core.tv.designsystem.TvFocusStyle
import com.filmax.core.tv.designsystem.TvFocusSurface
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvSurfaceColors
import kotlinx.coroutines.launch

// ────────────────────────────── Пункты разделов ──────────────────────────────

/**
 * Пункты разделов и капля под ними. Капля в раскрытом меню — фокус (акцент, белая рамка,
 * свечение), в свёрнутом — открытый раздел (акцентный кружок).
 */
@Composable
internal fun NavSections(
    state: NavSectionsState,
    requesters: List<FocusRequester>,
    onFocused: (Int) -> Unit,
    onSelect: (Int) -> Unit,
    exitRight: FocusRequester,
) {
    val dropIndex = if (state.expanded) state.focusedIndex else state.activeIndex
    Box {
        NavDrop(
            index = dropIndex,
            visible = dropIndex in SECTIONS.indices,
            focused = state.expanded,
            geometry = state.geometry,
            modifier = Modifier.matchParentSize(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(state.geometry.itemGap)) {
            SECTIONS.forEachIndexed { index, section ->
                NavItem(
                    section = section,
                    look = NavItemLook(
                        active = index == state.activeIndex,
                        focused = state.expanded && index == state.focusedIndex,
                        expanded = state.expanded,
                        order = index,
                    ),
                    height = state.geometry.itemHeight,
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .focusRequester(requesters[index])
                        .focusProperties { right = exitRight }
                        .onFocusChanged { if (it.isFocused) onFocused(index) },
                )
            }
        }
    }
}

/** Как выглядит пункт: открытый ли раздел, под фокусом ли, раскрыто ли меню, его место в списке. */
internal data class NavItemLook(val active: Boolean, val focused: Boolean, val expanded: Boolean, val order: Int)

/**
 * Пункт меню. Сам прозрачный — заливку рисует общая капля [NavDrop], пункт перекрашивает
 * содержимое. Открытый раздел в раскрытом меню, когда фокус ушёл на другой пункт, отмечен
 * лёгкой подложкой и точкой акцента справа.
 */
@Composable
private fun NavItem(
    section: TvSection,
    look: NavItemLook,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFxColors.current
    val onDrop = look.focused || (look.active && !look.expanded)
    val content by animateColorAsState(
        when {
            onDrop -> colors.onAccent
            look.active -> colors.text
            else -> colors.textSecondary
        },
        FxMotion.focus(),
        label = "navItemContent",
    )
    val marked = look.active && look.expanded && !look.focused
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(ItemRadius)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (marked) Color.White.copy(alpha = MARKED_ALPHA) else Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent,
            contentColor = content,
            focusedContentColor = content,
            pressedContentColor = content,
        ),
    ) {
        Row(
            Modifier.fillMaxHeight().padding(horizontal = if (look.expanded) 14.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(section.icon, contentDescription = section.label, modifier = Modifier.size(IconSize))
            NavLabel(section.label, look)
            if (marked) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(6.dp).background(colors.accent, CircleShape))
            }
        }
    }
}

/** Подпись пункта: появляется по очереди — сдвиг 28dp и проявление, шаг 50 мс. */
@Composable
private fun NavLabel(label: String, look: NavItemLook) {
    val shift = with(LocalDensity.current) { LabelShift.roundToPx() }
    AnimatedVisibility(
        visible = look.expanded,
        enter = fadeIn(tween(LABEL_FADE_MS, delayMillis = look.order * LABEL_STAGGER_MS)) +
            slideInHorizontally(tween(LABEL_SLIDE_MS, delayMillis = look.order * LABEL_STAGGER_MS)) { -shift },
        exit = fadeOut(tween(LABEL_EXIT_MS)),
    ) {
        Text(
            label,
            modifier = Modifier.padding(start = 14.dp),
            style = FxType.button.copy(
                fontWeight = if (look.focused || look.active) FontWeight.SemiBold else FontWeight.Medium
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ────────────────────────────── Капля ──────────────────────────────

/**
 * Подсветка, перетекающая каплей: край со стороны движения быстро тянется к новому пункту,
 * хвост догоняет мягче и чуть покачивается. Пока хвост в пути, капля вытянута на оба пункта.
 */
@Composable
private fun NavDrop(index: Int, visible: Boolean, focused: Boolean, geometry: NavGeometry, modifier: Modifier) {
    val colors = LocalFxColors.current
    val step = geometry.itemHeight + geometry.itemGap
    val drop = rememberLiquidDrop(top = step * index.coerceAtLeast(0), height = geometry.itemHeight)
    val alpha by animateFloatAsState(if (visible) 1f else 0f, FxMotion.focus(), label = "dropAlpha")
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .drawBehind {
                val top = drop.top.value.toPx()
                val height = drop.bottom.value.toPx() - top
                val radius = CornerRadius(minOf(ItemRadius.toPx(), height / 2, size.width / 2))
                drawRoundRect(colors.accent, Offset(0f, top), Size(size.width, height), radius)
                if (focused) {
                    drawRoundRect(
                        TvFocus,
                        Offset(0f, top),
                        Size(size.width, height),
                        radius,
                        style = Stroke(TvMetrics.FocusBorderWidth.toPx()),
                    )
                }
            },
    )
}

/** Верхний и нижний края капли — каждый на своей пружине. */
private class LiquidDrop(val top: Animatable<Dp, AnimationVector1D>, val bottom: Animatable<Dp, AnimationVector1D>)

@Composable
private fun rememberLiquidDrop(top: Dp, height: Dp): LiquidDrop {
    val drop = remember {
        LiquidDrop(
            Animatable(top, Dp.VectorConverter),
            Animatable(top + height, Dp.VectorConverter)
        )
    }
    LaunchedEffect(top, height) {
        val movingDown = top > drop.top.value
        launch { drop.top.animateTo(top, if (movingDown) TrailingEdge else LeadingEdge) }
        launch { drop.bottom.animateTo(top + height, if (movingDown) LeadingEdge else TrailingEdge) }
    }
    return drop
}

private val LeadingEdge = spring<Dp>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
private val TrailingEdge = spring<Dp>(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)

// ────────────────────────────── Логотип и профиль ──────────────────────────────

/** Логотип раскрытой панели: точка акцента и «filmax». */
@Composable
internal fun NavLogo(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            Modifier.padding(start = 10.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(10.dp).background(LocalFxColors.current.accent, CircleShape))
            Text("filmax", style = LogoStyle)
        }
    }
}

/**
 * Профиль: в свёрнутой колонке — аватар с инициалами (открыт Профиль — с белой рамкой), в
 * раскрытой панели — карточка аккаунта с именем и подпиской.
 */
@Composable
internal fun NavProfile(
    account: AccountSummary,
    active: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.then(if (expanded) Modifier.fillMaxWidth() else Modifier.size(AvatarSize)),
        style = if (expanded) TvFocusStyle.Row else TvFocusStyle.Circle,
        colors = TvSurfaceColors(
            container = if (expanded) Color.White.copy(alpha = CARD_ALPHA) else Color.Transparent,
            content = colors.text,
            focusedContainer = if (expanded) colors.accent else Color.Transparent,
            focusedContent = colors.onAccent,
        ),
    ) {
        if (expanded) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(account.initials, CardAvatarSize, ringed = false)
                Column {
                    Text(account.name, style = FxType.button, maxLines = 1)
                    Text(account.subscription, style = FxType.metaOnImage, color = colors.textSecondary, maxLines = 1)
                }
            }
        } else {
            Avatar(account.initials, AvatarSize, ringed = active)
        }
    }
}

@Composable
private fun Avatar(initials: String, size: Dp, ringed: Boolean) {
    val colors = LocalFxColors.current
    Box(
        Modifier
            .size(size)
            .background(colors.accent2, CircleShape)
            .then(if (ringed) Modifier.border(2.dp, TvFocus, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = FxType.pill, color = colors.onAccent2)
    }
}

// ────────────────────────────── Затемнение ──────────────────────────────

/**
 * Затемнение раскрытого меню: почти глухое у левого края, к правому — до 0.6. Проявляется и
 * гаснет вместе с панелью.
 */
@Composable
internal fun NavScrim(expanded: Boolean) {
    val alpha by animateFloatAsState(if (expanded) 1f else 0f, FxMotion.panelIn(), label = "navScrim")
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(ExpandedScrim),
    )
}

private val LogoStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 20.sp,
    letterSpacing = (-0.02).em
)

private val ScrimColor = Color(0xFF05040A)
private val ExpandedScrim = Brush.horizontalGradient(
    0f to ScrimColor.copy(alpha = 0.97f),
    0.3f to ScrimColor.copy(alpha = 0.88f),
    1f to ScrimColor.copy(alpha = 0.6f),
)

private val ItemRadius = 14.dp
private val IconSize = 20.dp
private val AvatarSize = 36.dp
private val CardAvatarSize = 40.dp
private const val MARKED_ALPHA = 0.08f
private const val CARD_ALPHA = 0.06f
private const val LABEL_FADE_MS = 200
private const val LABEL_SLIDE_MS = 320
private const val LABEL_EXIT_MS = 120
private const val LABEL_STAGGER_MS = 50
private val LabelShift = 28.dp
