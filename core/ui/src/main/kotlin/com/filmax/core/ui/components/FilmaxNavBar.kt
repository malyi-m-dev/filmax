package com.filmax.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassPanel
import com.filmax.core.designsystem.GlassState
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.designsystem.glow

/**
 * Разделы нижней навигации — те же, что на ТВ, плюс «Поиск» отдельной вкладкой: на телефоне его
 * открывают чаще всего, и поле в шапке Каталога стоило лишнего тапа и прокрутки наверх.
 */
enum class FilmaxTab(
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
) {
    HOME("Главная", Icons.Filled.Home, Icons.Outlined.Home),
    CATALOG("Каталог", Icons.Filled.Explore, Icons.Outlined.Explore),
    SEARCH("Поиск", Icons.Filled.Search, Icons.Filled.Search),
    MINE("Моё", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
    PROFILE("Профиль", Icons.Filled.Person, Icons.Outlined.Person),
}

/**
 * Плавающая стеклянная навигация над экраном. Подпись есть только у текущего раздела — в
 * акцентной пилюле со свечением; остальные — иконки: пять подписей в капсуле не помещаются на
 * узком телефоне, а текущий раздел и так главное, что навигация сообщает.
 *
 * [glass] — слой экрана под навигацией: она его размывает. Ленты оставляют под ней место
 * [FilmaxMetrics.NavBarClearance] (см. LocalNavBarPadding).
 */
@Composable
fun FilmaxNavBar(
    selected: FilmaxTab,
    onSelect: (FilmaxTab) -> Unit,
    glass: GlassState?,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        state = glass,
        shape = ShapeFull,
        modifier = modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.NavBarHeight),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilmaxTab.entries.forEach { tab ->
                NavItem(tab = tab, selected = tab == selected, onClick = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun NavItem(tab: FilmaxTab, selected: Boolean, onClick: () -> Unit) {
    val palette = FilmaxTheme.palette
    val background by animateColorAsState(if (selected) palette.accent else Color.Transparent, label = "navPill")
    val content = if (selected) palette.onAccent else FilmaxColors.TextSecondary
    Row(
        modifier = Modifier
            .then(if (selected) Modifier.glow(palette.glow, ShapeFull, blur = 24.dp, offsetY = 6.dp) else Modifier)
            .heightIn(min = PillHeight)
            .widthIn(min = PillHeight)
            .clip(ShapeFull)
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (selected) 14.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
            // У текущего раздела подпись видна текстом — TalkBack не должен читать её дважды.
            contentDescription = tab.label.takeUnless { selected },
            tint = content,
            modifier = Modifier.size(if (selected) 22.dp else 24.dp),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Text(tab.label, style = FilmaxType.chip, color = content, maxLines = 1)
        }
    }
}

private val PillHeight = 44.dp
