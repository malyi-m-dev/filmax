package com.filmax.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.ShapeFull

/**
 * Разделы нижней навигации. Их четыре, а не пять: «Поиск» уехал внутрь «Каталога» (поле в
 * шапке — тап и так один), «Подборки» стали контентом каталога, «Библиотека» переименована в
 * «Моё» — так этот раздел называет весь российский рынок, а «библиотека» это калька с Plex.
 */
enum class FilmaxTab(
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
) {
    HOME("Главная", Icons.Filled.Home, Icons.Outlined.Home),
    CATALOG("Каталог", Icons.Filled.GridView, Icons.Outlined.GridView),
    MINE("Моё", Icons.Outlined.Bookmark, Icons.Outlined.BookmarkBorder),
    PROFILE("Профиль", Icons.Filled.Person, Icons.Outlined.Person),
}

/**
 * Нижняя навигация. Плоская панель во всю ширину с тонкой линией сверху — не «остров»:
 * плавающая пилюля забирала внимание на себя и требовала от каждого экрана помнить про
 * отступ снизу.
 */
@Composable
fun FilmaxTabBar(
    selected: FilmaxTab,
    onSelect: (FilmaxTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilmaxTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: FilmaxTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Пилюля растёт под активной иконкой — единственная анимация в баре; ярлык виден всегда,
    // иначе вкладка опознаётся только по иконке (Google: ярлыки не прячем).
    val pillWidth by animateDpAsState(
        targetValue = if (selected) PillWidth else PillCollapsedWidth,
        label = "tabPill",
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        InactiveTabColor
    }

    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(PillHeight)
                .clip(ShapeFull)
                .background(
                    if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier.size(23.dp),
            )
        }
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
        )
    }
}

private val PillWidth = 56.dp
private val PillCollapsedWidth = 40.dp
private val PillHeight = 30.dp

/** Неактивная вкладка приглушена сильнее вторичного текста — она не должна спорить с активной. */
private val InactiveTabColor = Color(0xFF6E6E6E)
