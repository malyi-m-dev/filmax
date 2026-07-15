package com.filmax.app.tv.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import com.filmax.feature.library.tv.navigation.TvLibraryRoute
import com.filmax.feature.profile.tv.navigation.TvProfileRoute
import com.filmax.feature.search.tv.navigation.TvSearchRoute
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

/** Вкладка верхнего таб-бара: ярлык + маршрут + проверка активности. */
private data class TvTab(val label: String, val route: Any, val match: (NavDestination?) -> Boolean)

/** «Фокуса на вкладках нет» — контент держит фокус, переключать нечего. */
private const val NO_TAB = -1

/**
 * Пауза между наведением на вкладку и открытием раздела. Достаточно мала, чтобы переход
 * ощущался мгновенным, и достаточно велика, чтобы проезд мимо вкладки её не открывал.
 */
private const val TAB_SWITCH_DELAY_MS = 300L

/**
 * Четыре раздела. «Поиск» уехал внутрь «Каталога» (печатать пультом дорого — каталог даёт
 * способ найти фильм вообще без набора текста), «Подборки» стали контентом каталога,
 * «Библиотека» переименована в «Моё» — так этот раздел называет весь российский рынок.
 *
 * Маршруты пока прежние: TvSearchRoute отдаёт Каталог, TvLibraryRoute — «Моё».
 */
private val TABS = listOf(
    TvTab("Главная", TvHomeRoute) { it?.hasRoute(TvHomeRoute::class) == true },
    TvTab("Каталог", TvSearchRoute) { it?.hasRoute(TvSearchRoute::class) == true },
    TvTab("Моё", TvLibraryRoute) { it?.hasRoute(TvLibraryRoute::class) == true },
    TvTab("Профиль", TvProfileRoute) { it?.hasRoute(TvProfileRoute::class) == true },
)

/** Маршруты, на которых показывается таб-бар. Выводится из [TABS] — один источник правды. */
val TOP_LEVEL_ROUTES: List<KClass<*>> = listOf(
    TvHomeRoute::class,
    TvSearchRoute::class,
    TvLibraryRoute::class,
    TvProfileRoute::class,
)

/** Фокус-реквестеры шапки: вход в таб-бар ([navBar]) и переход к контенту ([content]). */
internal data class TvTopNavBarFocus(
    val navBar: FocusRequester,
    val content: FocusRequester,
)

/**
 * Верхний таб-бар. Не боковое меню: и Netflix, и Google TV в 2025 независимо ушли наверх, а
 * при горизонтальных рядах карточек боковое меню перехватывало бы фокус на «влево» из первой
 * карточки — верхний бар лежит на естественной оси «вверх».
 *
 * Фон сплошной, без градиента: серый градиент в монохроме — первый кандидат на бандинг.
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
    var focusedTab by remember { mutableIntStateOf(NO_TAB) }

    // Раздел открывается по наведению, без OK: на пульте лишнее нажатие на каждый переход —
    // это половина всей навигации по приложению.
    //
    // Задержка обязательна. Без неё проезд «Главная → Профиль» открывал бы по дороге Каталог и
    // Моё — три лишних экрана с сетевыми запросами ради одного перехода. Пока фокус едет мимо,
    // LaunchedEffect перезапускается и отменяет предыдущий переход; открывается только та
    // вкладка, на которой фокус реально задержался.
    LaunchedEffect(focusedTab) {
        val target = focusedTab
        if (target == NO_TAB || target == activeIndex) return@LaunchedEffect
        delay(TAB_SWITCH_DELAY_MS)
        onSelectTab(TABS[target].route)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TvMetrics.TopBarHeight)
            .focusRequester(focus.navBar)
            // Любой вход фокуса в таб-бар уводим на активную вкладку — фокус всегда
            // совпадает с открытым разделом.
            .focusProperties { enter = { tabFocusRequesters[activeIndex] } }
            .focusGroup()
            .padding(horizontal = TvMetrics.SafeHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvBrandLabel()
        Spacer(Modifier.weight(1f))
        TvNavTabs(
            activeIndex = activeIndex,
            tabFocusRequesters = tabFocusRequesters,
            contentFocus = focus.content,
            onSelectTab = onSelectTab,
            onTabFocused = { index -> focusedTab = index },
        )
        Spacer(Modifier.weight(1f))
        TvAvatar(initials = initials)
    }
}

/** Бренд-лейбл. В монохроме — разрядка вместо акцентной точки. */
@Composable
private fun TvBrandLabel() {
    Text(
        "FILMAX",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.5.sp,
        color = TvOnSurface,
        maxLines = 1,
        softWrap = false,
    )
}

/** Ряд фокусируемых вкладок таб-бара. */
@Composable
private fun TvNavTabs(
    activeIndex: Int,
    tabFocusRequesters: List<FocusRequester>,
    contentFocus: FocusRequester,
    onSelectTab: (route: Any) -> Unit,
    onTabFocused: (index: Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TABS.forEachIndexed { index, tab ->
            NavTab(
                label = tab.label,
                active = index == activeIndex,
                // OK оставляем рабочим: он открывает раздел сразу, не дожидаясь задержки.
                onClick = { onSelectTab(tab.route) },
                modifier = Modifier
                    .focusRequester(tabFocusRequesters[index])
                    .onFocusChanged { if (it.isFocused) onTabFocused(index) }
                    .focusProperties { down = contentFocus },
            )
        }
    }
}

/** Круглый аватар: инициалы либо иконка-заглушка. Ровная серая заливка вместо градиента. */
@Composable
private fun TvAvatar(initials: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(TvSurfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isNotBlank()) {
            Text(
                initials,
                style = MaterialTheme.typography.labelLarge,
                color = TvOnSurface,
                maxLines = 1,
                softWrap = false,
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = TvOnSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Вкладка: активная — белым текстом с подчёркиванием, неактивная — приглушённая. Заливки нет:
 * заливкой в монохроме отмечается выбор в чипах, а вкладку достаточно подчеркнуть.
 */
@Composable
private fun NavTab(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TvFocusCard(onClick = onClick, shape = TvMetrics.ButtonShape, modifier = modifier) {
        // width(IntrinsicSize.Max) обязателен: без него fillMaxWidth() у подчёркивания раздувает
        // вкладку на всю свободную ширину строки и выталкивает соседние вкладки за экран.
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                color = if (active) TvOnSurface else TvOnSurfaceDim,
            )
            if (active) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(TvAccent),
                )
            }
        }
    }
}
