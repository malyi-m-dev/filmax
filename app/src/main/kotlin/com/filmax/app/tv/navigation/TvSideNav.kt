@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.app.tv.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.filmax.core.navigation.Destination
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnAccent
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import com.filmax.feature.library.tv.navigation.TvLibraryRoute
import com.filmax.feature.profile.tv.navigation.TvProfileRoute
import com.filmax.feature.search.tv.navigation.TvSearchRoute
import kotlin.reflect.KClass

/** Раздел бокового меню: подпись, иконка, адрес и проверка активности. */
private data class TvSection(
    val label: String,
    val icon: ImageVector,
    val destination: Destination.Tab,
    val match: (NavDestination?) -> Boolean,
)

/**
 * Четыре раздела. «Поиск» уехал внутрь «Каталога» (печатать пультом дорого — каталог даёт
 * способ найти фильм вообще без набора текста), «Подборки» стали контентом каталога,
 * «Библиотека» переименована в «Моё» — так этот раздел называет весь российский рынок.
 * Иконки те же, что у нижнего таб-бара телефона.
 *
 * Маршруты пока прежние: TvSearchRoute отдаёт Каталог, TvLibraryRoute — «Моё».
 */
private val SECTIONS = listOf(
    TvSection("Главная", Icons.Filled.Home, Destination.Home) { it?.hasRoute(TvHomeRoute::class) == true },
    TvSection("Каталог", Icons.Filled.GridView, Destination.Catalog) { it?.hasRoute(TvSearchRoute::class) == true },
    TvSection("Моё", Icons.Outlined.Bookmark, Destination.Mine) { it?.hasRoute(TvLibraryRoute::class) == true },
    TvSection("Профиль", Icons.Filled.Person, Destination.Profile) { it?.hasRoute(TvProfileRoute::class) == true },
)

/** Номер раздела в меню, -1 — экран не раздел. По нему переходы знают, куда двигается меню. */
internal fun sectionIndexOf(destination: NavDestination?): Int = SECTIONS.indexOfFirst { it.match(destination) }

/** Маршруты, на которых показывается меню. Выводится из [SECTIONS] — один источник правды. */
val TOP_LEVEL_ROUTES: List<KClass<*>> = listOf(
    TvHomeRoute::class,
    TvSearchRoute::class,
    TvLibraryRoute::class,
    TvProfileRoute::class,
)

/**
 * Свёрнутое меню целиком помещается в левое поле экрана ([TvMetrics.SafeHorizontal] = 58dp):
 * контент разделов не сдвигается, и сетка каталога, свёрстанная впритык на четыре колонки,
 * остаётся как есть.
 */
private val CollapsedWidth = 56.dp
private val ExpandedWidth = 240.dp

/** Затемнение раскрытого меню шире самой панели: контент за ней гаснет плавно, без жёсткого края. */
private val ExpandedScrimWidth = 520.dp

private val ItemHeight = 44.dp
private val ItemGap = 8.dp
private val IconSize = 22.dp

/**
 * Подсветка перетекает с пружиной чуть мягче дефолтной: на шаг между соседями — без отскока,
 * на прыжок через весь список — с едва заметной инерцией.
 */
private val HighlightSpring = spring<Dp>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

/** Цвет содержимого пункта меняется за время, пока подсветка доезжает до него. */
private const val ITEM_COLOR_MS = 180

/** Фокус-реквестеры меню: вход в меню ([nav]) и возврат к контенту раздела ([content]). */
internal data class TvSideNavFocus(
    val nav: FocusRequester,
    val content: FocusRequester,
)

/**
 * Боковое меню разделов. Свёрнутое — колонка иконок в левом поле поверх контента; фокус в
 * меню раскрывает его подписями и затемняет контент.
 *
 * Сменило верхний таб-бар: чтобы вернуться к вкладкам из глубины раздела, приходилось долго
 * листать наверх. Теперь до меню — «влево» с первой карточки ряда или «назад» из любого места
 * раздела (BackHandler в графе). Раздел открывается по «ОК», а не по фокусу: в вертикальном
 * списке переход по фокусу грузил бы каждый раздел, через который проходит пульт.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TvSideNav(
    currentDestination: NavDestination?,
    onSelect: (section: Destination.Tab, isOpen: Boolean) -> Unit,
    focus: TvSideNavFocus,
    initials: String,
    modifier: Modifier = Modifier,
) {
    val activeIndex = sectionIndexOf(currentDestination).coerceAtLeast(0)
    val itemRequesters = remember { SECTIONS.map { FocusRequester() } }
    var expanded by remember { mutableStateOf(false) }
    var focusedIndex by remember { mutableIntStateOf(activeIndex) }
    val panelWidth by animateDpAsState(if (expanded) ExpandedWidth else CollapsedWidth, label = "sideNavWidth")

    Box(modifier.fillMaxHeight()) {
        NavScrim(expanded = expanded)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(panelWidth)
                .focusRequester(focus.nav)
                // Вход в меню — всегда на открытый раздел.
                .focusProperties { enter = { itemRequesters[activeIndex] } }
                .onFocusChanged { expanded = it.hasFocus }
                .focusGroup()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box {
                // Раскрытое меню подсвечивает пункт под фокусом, свёрнутое — открытый раздел.
                NavHighlight(index = if (expanded) focusedIndex else activeIndex, focused = expanded)
                Column(verticalArrangement = Arrangement.spacedBy(ItemGap)) {
                    SECTIONS.forEachIndexed { index, section ->
                        NavItem(
                            section = section,
                            look = NavItemLook(active = index == activeIndex, expanded = expanded),
                            initials = initials.takeIf { section.destination == Destination.Profile },
                            onClick = { onSelect(section.destination, index == activeIndex) },
                            modifier = Modifier
                                .focusRequester(itemRequesters[index])
                                .onFocusChanged { if (it.isFocused) focusedIndex = index }
                                // Выход вправо — обратно в контент. Задаётся на каждом пункте, не на
                                // группе: направленные свойства Compose берёт только у узла под
                                // фокусом, а D-pad-поиск в контент не попадает — его группа на весь
                                // экран и правее меню не лежит.
                                .focusProperties { right = focus.content },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Подсветка пунктов — одна на всё меню, а не заливка у каждого: при ходе пульта она перетекает
 * от пункта к пункту, а при выборе раздела остаётся на нём и гаснет до тихой подложки открытого
 * раздела. Так смена раздела читается как движение, а не как мигание двух заливок.
 */
@Composable
private fun NavHighlight(index: Int, focused: Boolean) {
    val offset by animateDpAsState((ItemHeight + ItemGap) * index, HighlightSpring, label = "sideNavHighlight")
    val color by animateColorAsState(
        if (focused) TvAccent else TvOnSurface.copy(alpha = 0.14f),
        tween(ITEM_COLOR_MS),
        label = "sideNavHighlightColor",
    )
    Box(
        Modifier
            .offset { IntOffset(0, offset.roundToPx()) }
            .fillMaxWidth()
            .height(ItemHeight)
            .background(color, CircleShape),
    )
}

/**
 * Подложка меню. Свёрнутое лежит поверх полноэкранных бэкдропов Главной, поэтому и ему нужна
 * лёгкая тень слева — иначе белые иконки теряются на светлом кадре. Тень не шире левого поля
 * ([TvMetrics.SafeHorizontal]): заходя дальше, она темнила кнопки и карточки экранов. Раскрытое
 * гасит контент сильнее и шире панели. Ноль — TvSurface с нулевой альфой, а не Color.Transparent: тот тянет
 * RGB к чёрному и даёт грязный хвост.
 */
@Composable
private fun NavScrim(expanded: Boolean) {
    // Раскрытое затемнение проявляется и гаснет вместе с подписями: переключённое разом, оно
    // исчезало раньше них, и подписи на кадр-другой повисали над светлым контентом.
    val expandedAlpha by animateFloatAsState(if (expanded) 1f else 0f, tween(SCRIM_FADE_MS), label = "sideNavScrim")
    Box(
        Modifier
            .fillMaxHeight()
            .width(TvMetrics.SafeHorizontal)
            .background(CollapsedScrim),
    )
    Box(
        Modifier
            .fillMaxHeight()
            .width(ExpandedScrimWidth)
            .graphicsLayer { alpha = expandedAlpha }
            .background(ExpandedScrim),
    )
}

private const val SCRIM_FADE_MS = 250

private val CollapsedScrim = Brush.horizontalGradient(
    0f to TvSurface.copy(alpha = 0.7f),
    0.75f to TvSurface.copy(alpha = 0.4f),
    1f to TvSurface.copy(alpha = 0f),
)

private val ExpandedScrim = Brush.horizontalGradient(
    0f to TvSurface.copy(alpha = 0.97f),
    0.45f to TvSurface.copy(alpha = 0.9f),
    0.75f to TvSurface.copy(alpha = 0.5f),
    1f to TvSurface.copy(alpha = 0f),
)

/** Как выглядит пункт: открытый ли это раздел и раскрыто ли меню. */
private data class NavItemLook(val active: Boolean, val expanded: Boolean)

/**
 * Пункт меню. Сам он прозрачный — белую заливку под фокусом рисует общая [NavHighlight], пункт
 * лишь перекрашивает содержимое в тёмное. Открытый раздел — белая иконка, остальные приглушены.
 * Без увеличения: пункты стоят столбиком вплотную, и раздутый на 8% задевал бы соседей.
 */
@Composable
private fun NavItem(
    section: TvSection,
    look: NavItemLook,
    initials: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val contentColor by animateColorAsState(
        when {
            focused -> TvOnAccent
            look.active -> TvOnSurface
            else -> TvOnSurfaceDim
        },
        tween(ITEM_COLOR_MS),
        label = "sideNavItemColor",
    )
    val clear = TvSurface.copy(alpha = 0f)
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(ItemHeight)
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = clear,
            focusedContainerColor = clear,
            pressedContainerColor = clear,
            contentColor = contentColor,
            focusedContentColor = contentColor,
            pressedContentColor = contentColor,
        ),
    ) {
        Row(
            Modifier.fillMaxHeight().padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (initials.isNullOrBlank()) {
                Icon(section.icon, contentDescription = section.label, modifier = Modifier.size(IconSize))
            } else {
                InitialsBadge(initials)
            }
            AnimatedVisibility(visible = look.expanded, enter = fadeIn(), exit = fadeOut()) {
                Row {
                    Spacer(Modifier.width(14.dp))
                    Text(
                        section.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (look.active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** Профиль — инициалами вместо иконки, как аватар в шапке раньше. */
@Composable
private fun InitialsBadge(initials: String) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(TvSurfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = TvOnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
