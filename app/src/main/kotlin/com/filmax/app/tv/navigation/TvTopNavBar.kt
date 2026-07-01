package com.filmax.app.tv.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.feature.collections.tv.navigation.TvCollectionsRoute
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import com.filmax.feature.library.tv.navigation.TvLibraryRoute
import com.filmax.feature.profile.tv.navigation.TvProfileRoute
import com.filmax.feature.search.tv.navigation.TvSearchRoute
import kotlin.reflect.KClass

/** Вкладка верхнего таб-бара: ярлык + маршрут + проверка активности. */
private data class TvTab(val label: String, val route: Any, val match: (NavDestination?) -> Boolean)

private val TABS = listOf(
    TvTab("Главная", TvHomeRoute) { it?.hasRoute(TvHomeRoute::class) == true },
    TvTab("Поиск", TvSearchRoute) { it?.hasRoute(TvSearchRoute::class) == true },
    TvTab("Подборки", TvCollectionsRoute) { it?.hasRoute(TvCollectionsRoute::class) == true },
    TvTab("Библиотека", TvLibraryRoute) { it?.hasRoute(TvLibraryRoute::class) == true },
    TvTab("Профиль", TvProfileRoute) { it?.hasRoute(TvProfileRoute::class) == true },
)

/** Маршруты, на которых показывается таб-бар (5 основных разделов). */
val TOP_LEVEL_ROUTES: List<KClass<*>> = listOf(
    TvHomeRoute::class,
    TvSearchRoute::class,
    TvCollectionsRoute::class,
    TvLibraryRoute::class,
    TvProfileRoute::class,
)

/** Фокус-реквестеры шапки: вход в таб-бар ([navBar]) и переход к контенту ([content]). */
internal data class TvTopNavBarFocus(
    val navBar: FocusRequester,
    val content: FocusRequester,
)

/**
 * Верхний таб-бар Filmax TV (как в макете): бренд «Filmax.», 5 разделов и аватар.
 * Вкладки фокусируемы пультом; выбор переходит на соответствующий маршрут.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TvTopNavBar(
    currentDestination: NavDestination?,
    onSelectTab: (route: Any) -> Unit,
    focus: TvTopNavBarFocus,
    initials: String,
    modifier: Modifier = Modifier,
) {
    val activeIndex = TABS.indexOfFirst { it.match(currentDestination) }.coerceAtLeast(0)
    // Стабильный requester на каждую вкладку (не «переезжает» между нодами).
    val tabFocusRequesters = remember { TABS.map { FocusRequester() } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), Color.Transparent)
                )
            )
            .focusRequester(focus.navBar)
            // Любой вход фокуса в таб-бар уводим на активную вкладку — фокус всегда
            // совпадает с открытым разделом.
            .focusProperties { enter = { tabFocusRequesters[activeIndex] } }
            .focusGroup()
            .padding(horizontal = 48.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvBrandLabel()
        Spacer(Modifier.width(32.dp))
        TvNavTabs(
            activeIndex = activeIndex,
            tabFocusRequesters = tabFocusRequesters,
            contentFocus = focus.content,
            onSelectTab = onSelectTab,
        )
        Spacer(Modifier.weight(1f))
        TvAvatar(initials = initials)
    }
}

/** Бренд-лейбл «Filmax» + акцентная точка. */
@Composable
private fun TvBrandLabel() {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "Filmax",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
        Text(
            ".",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Ряд фокусируемых вкладок таб-бара. */
@Composable
private fun TvNavTabs(
    activeIndex: Int,
    tabFocusRequesters: List<FocusRequester>,
    contentFocus: FocusRequester,
    onSelectTab: (route: Any) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TABS.forEachIndexed { index, tab ->
            NavTab(
                label = tab.label,
                active = index == activeIndex,
                onClick = { onSelectTab(tab.route) },
                modifier = Modifier
                    .focusRequester(tabFocusRequesters[index])
                    .focusProperties { down = contentFocus },
            )
        }
    }
}

/** Круглый аватар: инициалы пользователя либо иконка-заглушка. */
@Composable
private fun TvAvatar(initials: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFB4305A), Color(0xFFF4B792)))),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isNotBlank()) {
            Text(
                initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                softWrap = false
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NavTab(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(percent = 50)
    val labelColor = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    TvFocusCard(onClick = onClick, shape = shape, modifier = modifier) {
        Box(
            Modifier
                .clip(shape)
                .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                color = labelColor,
            )
        }
    }
}
