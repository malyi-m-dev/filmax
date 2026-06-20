package com.filmax.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FilmaxTab(
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
) {
    HOME("Главная", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("Поиск", Icons.Filled.Search, Icons.Outlined.Search),
    CATEGORIES("Жанры", Icons.Filled.GridView, Icons.Outlined.GridView),
    LIBRARY("Библио", Icons.Filled.LocalLibrary, Icons.Outlined.LocalLibrary),
    PROFILE("Профиль", Icons.Filled.Person, Icons.Outlined.Person),
}

@Composable
fun FilmaxTabBar(
    selected: FilmaxTab,
    onSelect: (FilmaxTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 0.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilmaxTab.entries.forEach { tab ->
                TabItem(
                    tab      = tab,
                    selected = tab == selected,
                    onClick  = { onSelect(tab) },
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
    val pillWidth by animateDpAsState(
        if (selected) 52.dp else 32.dp,
        spring(stiffness = 300f, dampingRatio = 0.7f),
        label = "pillWidth",
    )
    val pillColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "pillColor",
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabColor",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text       = tab.label,
            color      = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize   = 10.sp,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
        )
    }
}
