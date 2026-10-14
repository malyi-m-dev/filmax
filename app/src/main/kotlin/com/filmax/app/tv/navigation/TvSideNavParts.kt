@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.app.tv.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.filmax.core.tv.designsystem.TvKeyHint
import com.filmax.core.tv.designsystem.TvKeyHints

// ────────────────────────────── Пункты разделов ──────────────────────────────

/**
 * Пункты разделов. Подсветку под ними рисует общая капля ([NavDropLayer]) — пункты лишь отдают
 * ей свои границы и перекрашивают содержимое.
 */
@Composable
internal fun NavSections(
    state: NavSectionsState,
    requesters: List<FocusRequester>,
    actions: NavSectionsActions,
    anchors: NavDropAnchors,
) {
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
                onClick = { actions.onSelect(index) },
                modifier = Modifier
                    .then(anchors.target(index))
                    .focusRequester(requesters[index])
                    .focusProperties { right = actions.exitRight }
                    .onFocusChanged { if (it.isFocused) actions.onFocused(index) },
            )
        }
    }
}

/** Что делают пункты: фокус на пункте, «ОК» на нём и куда уводит «вправо». */
internal data class NavSectionsActions(
    val onFocused: (Int) -> Unit,
    val onSelect: (Int) -> Unit,
    val exitRight: FocusRequester,
)

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
            Modifier
                .fillMaxHeight()
                .navFocusScale(look.focused)
                .padding(horizontal = if (look.expanded) 14.dp else 8.dp),
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

// ────────────────────────────── Логотип и профиль ──────────────────────────────

/**
 * Логотип раскрытой панели: точка акцента и «filmax». Отдельный слой поверх панели, высотой
 * [LogoHeight]: он только проявляется и ничего под собой не двигает. Уходит в тот же кадр, что
 * закрывается меню, без затухания: место под ним колонка отдаёт сразу, и гаснущая надпись легла
 * бы на поднявшийся пункт.
 */
@Composable
internal fun NavLogo(visible: Boolean, modifier: Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) tween(LOGO_FADE_MS) else snap(),
        label = "navLogo",
    )
    if (!visible) return
    Row(
        modifier
            .height(LogoHeight)
            .graphicsLayer { this.alpha = alpha }
            .padding(start = 10.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(10.dp).background(LocalFxColors.current.accent, CircleShape))
        // Цвет задан явно: вне Surface у tv-material Text цвет по умолчанию тёмный, и логотип
        // на тёмной панели почти пропадал.
        Text("filmax", style = LogoStyle, color = LocalFxColors.current.text)
    }
}

private const val LOGO_FADE_MS = 300

/** Как выглядит профиль: открыт ли Профиль, раскрыто ли меню, стоит ли на нём капля фокуса. */
internal data class NavProfileLook(val active: Boolean, val expanded: Boolean, val focused: Boolean)

/**
 * Профиль: в свёрнутой колонке — аватар с инициалами (открыт Профиль — с белой рамкой), в
 * раскрытой панели — карточка аккаунта с именем и подпиской. Подсветку фокуса, как и у пунктов,
 * рисует общая капля ([NavDropLayer]): карточка в фокусе прозрачна и лишь перекрашивает текст.
 */
@Composable
internal fun NavProfile(account: AccountSummary, look: NavProfileLook, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalFxColors.current
    val content by animateColorAsState(
        if (look.focused) colors.onAccent else colors.text,
        FxMotion.focus(),
        label = "navProfileContent",
    )
    val card = if (look.expanded && !look.focused) Color.White.copy(alpha = CARD_ALPHA) else Color.Transparent
    Surface(
        onClick = onClick,
        modifier = modifier.then(if (look.expanded) Modifier.fillMaxWidth() else Modifier.size(AvatarSize)),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(if (look.expanded) CardRadius else ItemRadius)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = card,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent,
            contentColor = content,
            focusedContentColor = content,
            pressedContentColor = content,
        ),
    ) {
        if (look.expanded) {
            AccountCard(account, focused = look.focused)
        } else {
            Avatar(account.initials, AvatarSize, ringed = look.active, style = FxType.pill)
        }
    }
}

@Composable
private fun AccountCard(account: AccountSummary, focused: Boolean) {
    val colors = LocalFxColors.current
    Row(
        Modifier.navFocusScale(focused).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(account.initials, CardAvatarSize, ringed = false, style = CardInitialsStyle)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(account.name, style = AccountNameStyle, maxLines = 1)
            Text(
                account.subscription,
                style = FxType.metaOnImage,
                color = if (focused) colors.onAccent.copy(alpha = SUBSCRIPTION_ON_DROP_ALPHA) else colors.accentText,
                maxLines = 1,
            )
        }
    }
}

/** Подписка на капле — цветом текста на акценте, чуть тише имени. */
private const val SUBSCRIPTION_ON_DROP_ALPHA = 0.8f

@Composable
private fun Avatar(initials: String, size: Dp, ringed: Boolean, style: TextStyle) {
    val colors = LocalFxColors.current
    Box(
        Modifier
            .size(size)
            .background(colors.accent2, CircleShape)
            .then(if (ringed) Modifier.border(2.dp, TvFocus, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = style, color = colors.onAccent2)
    }
}

/**
 * Пункт в фокусе чуть крупнее, как и остальные элементы в фокусе. Масштабируется содержимое, а
 * не сам пункт: по границам пункта стоит капля, и они не должны дрожать вместе с масштабом —
 * каплю масштабирует её слой ([NavDropLayer]).
 */
@Composable
internal fun Modifier.navFocusScale(focused: Boolean): Modifier {
    val scale by animateFloatAsState(if (focused) NAV_FOCUS_SCALE else 1f, FxMotion.focus(), label = "navItemScale")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ────────────────────────────── Подсказки клавиш ──────────────────────────────

/** Подсказки клавиш в правом нижнем углу, пока меню раскрыто: как ходить по меню и закрыть его. */
@Composable
internal fun NavKeyHints(visible: Boolean, modifier: Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(FxMotion.panelIn()),
        exit = fadeOut(FxMotion.panelOut()),
    ) {
        TvKeyHints(
            listOf(
                TvKeyHint(listOf(Icons.Rounded.KeyboardArrowUp, Icons.Rounded.KeyboardArrowDown), "раздел"),
                TvKeyHint(emptyList(), "открыть", keyText = "OK"),
                TvKeyHint(listOf(Icons.AutoMirrored.Rounded.KeyboardArrowRight), "закрыть"),
            ),
        )
    }
}

// ────────────────────────────── Затемнение ──────────────────────────────

/**
 * Затемнение раскрытого меню: почти глухое у левого края, к правому — до 0.6. Проявляется и
 * гаснет вместе с панелью.
 */
@Composable
internal fun NavScrim(expanded: Boolean) {
    val alpha by animateFloatAsState(
        if (expanded) 1f else 0f,
        if (expanded) FxMotion.panelIn() else FxMotion.panelOut(),
        label = "navScrim",
    )
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(ExpandedScrim),
    )
}

/** Высота логотипа: поля 8 + 16 и строка «filmax». */
internal val LogoHeight = 50.dp

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

/** Карточка аккаунта в раскрытой панели: имя и инициалы 14, как в макете. */
private val AccountNameStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
)
private val CardInitialsStyle = FxType.pill.copy(fontSize = 14.sp, lineHeight = 18.sp)

private val ItemRadius = 14.dp
private val CardRadius = 16.dp
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
