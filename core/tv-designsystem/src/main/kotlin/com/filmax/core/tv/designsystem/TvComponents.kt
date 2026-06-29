@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/**
 * Крупная pill-кнопка для пульта на базе tv-material3 [Button] — фокус/масштаб/обводка
 * берутся из коробки. [primary] — брендовая заливка, иначе «вторичная» поверхность.
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    leadingIcon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
) {
    val colors = if (primary) {
        ButtonDefaults.colors(
            containerColor = TvPrimaryContainer,
            contentColor = TvOnPrimaryContainer,
            focusedContainerColor = TvPrimary,
            focusedContentColor = TvOnPrimary,
        )
    } else {
        ButtonDefaults.colors(
            containerColor = TvSurfaceContainerHigh,
            contentColor = TvOnSurface,
            focusedContainerColor = TvOnSurface,
            focusedContentColor = TvSurface,
        )
    }

    Button(
        onClick = onClick,
        colors = colors,
        shape = ButtonDefaults.shape(RoundedCornerShape(percent = 50)),
        modifier = modifier.then(
            if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Фокусируемая карточка-контейнер на базе tv-material3 clickable [Surface]. Содержимое
 * (постер, плитка и т.п.) передаётся слотом. Контейнер прозрачный — на фокусе срабатывают
 * нативные масштаб + жёлтая обводка (`colorScheme.border == TvFocus`).
 */
@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    focusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent,
            contentColor = TvOnSurface,
            focusedContentColor = TvOnSurface,
        ),
        modifier = modifier.then(
            if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
        ),
    ) {
        content()
    }
}
