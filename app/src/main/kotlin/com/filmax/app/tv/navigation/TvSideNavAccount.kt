// Аккаунт в боковой навигации ТВ: аватар с инициалами внизу свёрнутой панели и карточка с именем и
// подпиской — в раскрытой. Пункты разделов, логотип и подсказки пульта — в TvSideNavParts.
@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.app.tv.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.filmax.app.navigation.AccountSummary
import com.filmax.core.designsystem.Onest
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvFocus

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

/** Карточка аккаунта в раскрытой панели: имя и инициалы 14, как в макете. */
private val AccountNameStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
)
private val CardInitialsStyle = FxType.pill.copy(fontSize = 14.sp, lineHeight = 18.sp)

private val CardRadius = 16.dp
private val AvatarSize = 36.dp
private val CardAvatarSize = 40.dp
private const val CARD_ALPHA = 0.06f
