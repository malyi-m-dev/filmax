package com.filmax.app.tv.navigation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
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
import com.filmax.core.tv.designsystem.TvMetrics
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

/**
 * Геометрия меню в текущий момент перехода свёрнуто ↔ раскрыто — анимируется целиком.
 * [opened] — меню раскрылось до конца и размеры пунктов больше не меняются.
 */
internal data class NavGeometry(val width: Dp, val itemHeight: Dp, val itemGap: Dp, val opened: Boolean)

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
    val targets = remember { NavTargets() }
    var expanded by remember { mutableStateOf(false) }
    var focusedIndex by remember { mutableIntStateOf(activeIndex) }
    val geometry = rememberNavGeometry(expanded)

    Box(modifier.fillMaxHeight()) {
        NavScrim(expanded)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .navPanel(expanded, geometry.width)
                .focusRequester(focus.nav)
                // Вход в меню — всегда на открытый раздел.
                .focusProperties { enter = { targets.entry(activeIndex, profileActive) } }
                .onFocusChanged { expanded = it.hasFocus }
                .focusGroup()
                .padding(if (expanded) ExpandedPadding else CollapsedPadding),
        ) {
            val state = NavSectionsState(activeIndex, focusedIndex, expanded, geometry)
            NavDropLayer(targets.anchors, dropTarget(state), Modifier.matchParentSize())
            NavColumn(
                state = state,
                account = account,
                targets = targets,
                actions = NavSectionsActions(
                    onFocused = { index -> focusedIndex = index },
                    onSelect = { index -> onSelect(SECTIONS[index].destination, index == activeIndex) },
                    exitRight = focus.content,
                ),
                onProfile = { onSelect(Destination.Profile, profileActive) },
            )
            NavLogo(visible = expanded, modifier = Modifier.align(Alignment.TopStart))
        }
        NavKeyHints(
            visible = expanded,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = TvMetrics.SectionEnd, bottom = HintsBottom),
        )
    }
}

/**
 * Колонка меню: пункты разделов, внизу профиль. Границы целей забирает капля. Логотип — не в
 * колонке, а отдельным слоем над ней ([NavLogo]): раньше его появление сдвигало пункты вниз, и
 * подсветка на раскрытии сначала стояла на «filmax». Колонка лишь оставляет под него место.
 */
@Composable
private fun NavColumn(
    state: NavSectionsState,
    account: AccountSummary,
    targets: NavTargets,
    actions: NavSectionsActions,
    onProfile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .then(if (state.expanded) Modifier.fillMaxHeight() else Modifier)
            .then(targets.anchors.containerModifier)
            // Отступ — внутри границ колонки: от них капля отсчитывает места пунктов.
            .then(if (state.expanded) Modifier.padding(top = LogoSpace) else Modifier),
        verticalArrangement = Arrangement.spacedBy(state.geometry.itemGap),
    ) {
        NavSections(state, targets.items, actions, targets.anchors)
        if (state.expanded) Spacer(Modifier.weight(1f))
        NavProfile(
            account = account,
            look = NavProfileLook(
                // Меню видно только на разделах: ни один пункт не открыт — значит, открыт Профиль.
                active = state.activeIndex < 0,
                expanded = state.expanded,
                focused = state.expanded && state.focusedIndex == PROFILE_INDEX,
            ),
            onClick = onProfile,
            modifier = Modifier
                .then(targets.anchors.target(PROFILE_INDEX))
                .focusRequester(targets.profile)
                .focusProperties { right = actions.exitRight }
                .onFocusChanged { if (it.isFocused) actions.onFocused(PROFILE_INDEX) },
        )
    }
}

/** Фокус-цели меню и их границы для капли. */
private class NavTargets {
    val items: List<FocusRequester> = SECTIONS.map { FocusRequester() }
    val profile = FocusRequester()
    val anchors = NavDropAnchors()

    /** Вход в меню: открытый раздел — его пункт, открыт Профиль — карточка профиля. */
    fun entry(activeIndex: Int, profileActive: Boolean): FocusRequester =
        if (profileActive) profile else items[activeIndex.coerceAtLeast(0)]
}

/**
 * На ком капля. Раскрытое меню — на пункте или карточке профиля в фокусе. Свёрнутое — на
 * открытом разделе; открыт Профиль — капли нет: в колонке он отмечен кольцом вокруг аватара.
 */
private fun dropTarget(state: NavSectionsState): NavDropTarget {
    val index = if (state.expanded) state.focusedIndex else state.activeIndex
    val visible = index in SECTIONS.indices || (index == PROFILE_INDEX && state.expanded)
    return NavDropTarget(index, visible, focused = state.expanded, glowing = state.geometry.opened)
}

/** Стеклянная колонка меню: поле от края экрана, высота и ширина по состоянию. */
@Composable
private fun Modifier.navPanel(expanded: Boolean, width: Dp): Modifier = this
    .padding(PanelMargin)
    .then(if (expanded) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
    .width(width)
    .fxGlass(RoundedCornerShape(PanelRadius))

/** Что нужно пунктам и капле: открытый раздел, фокус, раскрыто ли меню и его геометрия. */
internal data class NavSectionsState(
    val activeIndex: Int,
    val focusedIndex: Int,
    val expanded: Boolean,
    val geometry: NavGeometry,
)

/** Раскрытие — 400 мс по Standard, закрытие быстрее (220 мс по Exit): меню уступает дорогу контенту. */
@Composable
private fun rememberNavGeometry(expanded: Boolean): NavGeometry {
    val transition = updateTransition(expanded, label = "sideNav")
    val width by transition.animateDp({ panelSpec() }, label = "width") { if (it) ExpandedWidth else CollapsedWidth }
    val itemHeight by transition.animateDp({ panelSpec() }, label = "itemHeight") {
        if (it) ExpandedItemHeight else CollapsedItemHeight
    }
    val gap by transition.animateDp({ panelSpec() }, label = "gap") { if (it) ExpandedGap else CollapsedGap }
    val opened = transition.currentState && transition.targetState
    return NavGeometry(width, itemHeight, gap, opened)
}

private fun Transition.Segment<Boolean>.panelSpec(): FiniteAnimationSpec<Dp> =
    if (targetState) FxMotion.panelIn() else FxMotion.panelOut()

/** Колонка 48 = 6 + 36 + 6: целиком в левом поле, контент разделов начинается с 80dp. */
private val CollapsedWidth = 48.dp
private val ExpandedWidth = 248.dp
private val CollapsedItemHeight = 36.dp
private val ExpandedItemHeight = 46.dp
private val CollapsedGap = 8.dp
private val ExpandedGap = 6.dp

/** Место над пунктами раскрытого меню под логотип и привычный зазор до первого пункта. */
private val LogoSpace = LogoHeight + ExpandedGap
private val PanelMargin = 14.dp
private val PanelRadius = 24.dp
private val CollapsedPadding = 6.dp
private val ExpandedPadding = 14.dp
private val HintsBottom = 24.dp
