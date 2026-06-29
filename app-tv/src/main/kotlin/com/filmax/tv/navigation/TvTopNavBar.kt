package com.filmax.tv.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.feature.tv.categories.navigation.TvCategoriesRoute
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import com.filmax.feature.tv.library.navigation.TvLibraryRoute
import com.filmax.feature.tv.profile.navigation.TvProfileRoute
import com.filmax.feature.tv.search.navigation.TvSearchRoute
import kotlin.reflect.KClass

/** Вкладка верхнего таб-бара: ярлык + маршрут + проверка активности. */
private data class TvTab(val label: String, val route: Any, val match: (NavDestination?) -> Boolean)

private val TABS = listOf(
    TvTab("Главная", TvHomeRoute) { it?.hasRoute(TvHomeRoute::class) == true },
    TvTab("Поиск", TvSearchRoute) { it?.hasRoute(TvSearchRoute::class) == true },
    TvTab("Жанры", TvCategoriesRoute) { it?.hasRoute(TvCategoriesRoute::class) == true },
    TvTab("Библиотека", TvLibraryRoute) { it?.hasRoute(TvLibraryRoute::class) == true },
    TvTab("Профиль", TvProfileRoute) { it?.hasRoute(TvProfileRoute::class) == true },
)

/** Маршруты, на которых показывается таб-бар (5 основных разделов). */
val TOP_LEVEL_ROUTES: List<KClass<*>> = listOf(
    TvHomeRoute::class, TvSearchRoute::class, TvCategoriesRoute::class,
    TvLibraryRoute::class, TvProfileRoute::class,
)

/**
 * Верхний таб-бар Filmax TV (как в макете): бренд «Filmax.», 5 разделов и аватар.
 * Вкладки фокусируемы пультом; выбор переходит на соответствующий маршрут.
 */
@Composable
fun TvTopNavBar(
    currentDestination: NavDestination?,
    onSelectTab: (route: Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), Color.Transparent)
                )
            )
            .padding(horizontal = 72.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Filmax", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(".", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TABS.forEach { tab ->
                NavTab(label = tab.label, active = tab.match(currentDestination), onClick = { onSelectTab(tab.route) })
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFB4305A), Color(0xFFF4B792)))),
            contentAlignment = Alignment.Center,
        ) {
            Text("АК", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun NavTab(label: String, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    TvFocusCard(onClick = onClick, shape = shape) {
        Box(
            Modifier
                .clip(shape)
                .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(horizontal = 28.dp, vertical = 12.dp),
        ) {
            Text(
                label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
