package com.filmax.app.tv.navigation

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.filmax.app.navigation.AccountSummary
import com.filmax.core.navigation.Destination
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.fxGlass
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import com.filmax.feature.library.tv.navigation.TvLibraryRoute
import com.filmax.feature.profile.tv.navigation.TvProfileRoute
import com.filmax.feature.search.tv.navigation.TvSearchRoute
import kotlin.reflect.KClass

/** Раздел меню: подпись, иконка, адрес и проверка активности. */
internal data class TvSection(
    val label: String,
    val icon: ImageVector,
    val destination: Destination.Tab,
    val match: (NavDestination?) -> Boolean,
)

/**
 * Разделы меню. Профиль — четвёртый раздел, но рисуется отдельно: аватаром внизу свёрнутой
 * колонки и карточкой аккаунта внизу раскрытой панели (решение по редизайну, docs/tv-redesign.md).
 * Маршруты пока прежние: TvSearchRoute отдаёт Каталог, TvLibraryRoute — «Моё».
 */
internal val SECTIONS = listOf(
    TvSection("Главная", Icons.Rounded.Home, Destination.Home) { it?.hasRoute(TvHomeRoute::class) == true },
    TvSection("Каталог", Icons.Rounded.GridView, Destination.Catalog) { it?.hasRoute(TvSearchRoute::class) == true },
    TvSection("Моё", Icons.Rounded.Bookmark, Destination.Mine) { it?.hasRoute(TvLibraryRoute::class) == true },
)

private fun isProfile(destination: NavDestination?): Boolean = destination?.hasRoute(TvProfileRoute::class) == true

/** Номер раздела в меню (Профиль — последним), -1 — экран не раздел. По нему переходы знают направление. */
internal fun sectionIndexOf(destination: NavDestination?): Int = when {
    isProfile(destination) -> SECTIONS.size
    else -> SECTIONS.indexOfFirst { it.match(destination) }
}

/** Маршруты, на которых показывается меню. */
val TOP_LEVEL_ROUTES: List<KClass<*>> = listOf(
    TvHomeRoute::class,
    TvSearchRoute::class,
    TvLibraryRoute::class,
    TvProfileRoute::class,
)

/** Фокус-реквестеры меню: вход в меню ([nav]) и возврат к контенту раздела ([content]). */
internal data class TvSideNavFocus(
    val nav: FocusRequester,
    val content: FocusRequester,
)

/** Геометрия меню в текущий момент перехода свёрнуто ↔ раскрыто — анимируется целиком. */
internal data class NavGeometry(val width: Dp, val itemHeight: Dp, val itemGap: Dp)

/**
 * Боковое меню разделов. Свёрнутое — стеклянная колонка кружков 36dp по центру левого поля:
 * текущий раздел залит акцентом, внизу аватар профиля. Фокус раскрывает его в панель 248dp во
 * всю высоту: логотип, подписи пунктов появляются по очереди, внизу карточка аккаунта, экран
 * затемняется. Подсветка одна на все пункты и перетекает каплей ([NavDrop]).
 *
 * Раздел открывается по «ОК», а не по фокусу: переход по фокусу грузил бы каждый раздел, через
 * который проходит пульт.
 */
// focusProperties.enter — вход в меню на открытый раздел; API помечен экспериментальным.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TvSideNav(
    currentDestination: NavDestination?,
    onSelect: (section: Destination.Tab, isOpen: Boolean) -> Unit,
    focus: TvSideNavFocus,
    account: AccountSummary,
    modifier: Modifier = Modifier,
) {
    val activeIndex = SECTIONS.indexOfFirst { it.match(currentDestination) }
    val profileActive = isProfile(currentDestination)
    val itemRequesters = remember { SECTIONS.map { FocusRequester() } }
    val profileRequester = remember { FocusRequester() }
    var expanded by remember { mutableStateOf(false) }
    var focusedIndex by remember { mutableIntStateOf(activeIndex) }
    val geometry = rememberNavGeometry(expanded)

    Box(modifier.fillMaxHeight()) {
        NavScrim(expanded)
        Column(
            modifier = Modifier
                .padding(PanelMargin)
                .align(Alignment.CenterStart)
                .then(if (expanded) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                .width(geometry.width)
                .fxGlass(RoundedCornerShape(PanelRadius))
                .focusRequester(focus.nav)
                // Вход в меню — всегда на открытый раздел.
                .focusProperties {
                    enter = { if (profileActive) profileRequester else itemRequesters[activeIndex.coerceAtLeast(0)] }
                }
                .onFocusChanged { expanded = it.hasFocus }
                .focusGroup()
                .padding(if (expanded) ExpandedPadding else CollapsedPadding),
            verticalArrangement = Arrangement.spacedBy(geometry.itemGap),
        ) {
            NavLogo(visible = expanded)
            NavSections(
                state = NavSectionsState(activeIndex, focusedIndex, expanded, geometry),
                requesters = itemRequesters,
                onFocused = { index -> focusedIndex = index },
                onSelect = { index -> onSelect(SECTIONS[index].destination, index == activeIndex) },
                exitRight = focus.content,
            )
            if (expanded) Spacer(Modifier.weight(1f))
            NavProfile(
                account = account,
                active = profileActive,
                expanded = expanded,
                onClick = { onSelect(Destination.Profile, profileActive) },
                modifier = Modifier
                    .focusRequester(profileRequester)
                    .focusProperties { right = focus.content },
            )
        }
    }
}

/** Что нужно ряду пунктов, чтобы нарисовать себя и каплю. */
internal data class NavSectionsState(
    val activeIndex: Int,
    val focusedIndex: Int,
    val expanded: Boolean,
    val geometry: NavGeometry,
)

@Composable
private fun rememberNavGeometry(expanded: Boolean): NavGeometry {
    val transition = updateTransition(expanded, label = "sideNav")
    val width by transition.animateDp(
        { FxMotion.panelIn() },
        label = "width"
    ) { if (it) ExpandedWidth else CollapsedWidth }
    val itemHeight by transition.animateDp({ FxMotion.panelIn() }, label = "itemHeight") {
        if (it) ExpandedItemHeight else CollapsedItemHeight
    }
    val gap by transition.animateDp({ FxMotion.panelIn() }, label = "gap") { if (it) ExpandedGap else CollapsedGap }
    return NavGeometry(width, itemHeight, gap)
}

/** Колонка 48 = 6 + 36 + 6: целиком в левом поле, контент разделов начинается с 80dp. */
private val CollapsedWidth = 48.dp
private val ExpandedWidth = 248.dp
private val CollapsedItemHeight = 36.dp
private val ExpandedItemHeight = 46.dp
private val CollapsedGap = 8.dp
private val ExpandedGap = 6.dp
private val PanelMargin = 14.dp
private val PanelRadius = 24.dp
private val CollapsedPadding = 6.dp
private val ExpandedPadding = 14.dp
