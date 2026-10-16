package com.filmax.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.filmax.core.domain.common.ErrorReporting
import com.filmax.core.domain.common.TelemetryText
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.NavCommand
import com.filmax.core.navigation.Navigator
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute
import com.filmax.feature.designsystem.navigation.DesignSystemRoute
import com.filmax.feature.details.common.navigation.DetailsRoute
import com.filmax.feature.feedback.common.navigation.FeedbackRoute
import com.filmax.feature.home.mobile.navigation.HomeRoute
import com.filmax.feature.library.mobile.navigation.LibraryRoute
import com.filmax.feature.onboarding.mobile.navigation.OnboardingRoute
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.common.navigation.TrailerRoute
import com.filmax.feature.profile.mobile.navigation.DeviceSettingsRoute
import com.filmax.feature.profile.mobile.navigation.ProfileRoute
import com.filmax.feature.search.common.navigation.FilmographyRoute
import com.filmax.feature.search.mobile.navigation.SearchRoute
import org.koin.compose.koinInject

/**
 * Что граф знает о себе: как адрес превращается в маршрут и какой маршрут — корень вкладок.
 * Телефонный и ТВ-граф различаются только этим; семантика команд у них одна.
 */
internal class GraphRoutes(
    /** Реальный корень вкладок — до него попапят при переключении раздела (не сплэш). */
    val tabRoot: Any,
    val toRoute: (Destination) -> Any,
)

/**
 * Исполнитель команд навигации: единственное место графа, где абстрактный [Destination]
 * превращается в маршрут и в конкретные NavOptions. Один на телефон и ТВ — граф отдаёт
 * [GraphRoutes], а команды разбираются одинаково.
 *
 * Экраны отправляют команды через [Navigator] и не знают ни маршрутов соседних фич, ни того,
 * сколько в приложении графов. Захочется разложить разделы по вложенным графам со своими
 * стеками — меняется этот файл, а не двадцать экранов.
 */
@Composable
internal fun NavigationCommands(
    navController: NavHostController,
    routes: GraphRoutes,
    navigator: Navigator = koinInject(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(navController, routes, lifecycle) {
        // Команды разбирает только активити на экране. Очередь у Navigator одна на приложение, и
        // скрытая активити (лаунчер открыл приложение второй раз поверх первого) забирала каждую
        // вторую команду: экран не отзывался на нажатие, а плеер открывался в ней и играл за кадром.
        // Пока активити скрыта, команды ждут в очереди.
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navigator.commands.collect { command ->
                ErrorReporting.reporter.log(TelemetryText.line("nav: $command"))
                navController.execute(command, routes)
            }
        }
    }
    ScreenTelemetry(navController)
}

/**
 * Экран для телеметрии: ключ [SCREEN_KEY] у каждого отчёта и крошка на каждую смену экрана. Слушатель
 * графа, а не команды [Navigator]: «Назад» с пульта и системные переходы команд не шлют.
 */
@Composable
private fun ScreenTelemetry(navController: NavHostController) {
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            // Маршрут — serialName класса с шаблоном аргументов: «…TvDetailsRoute/{itemId}».
            val screen = destination.route?.substringBefore('/')?.substringAfterLast('.') ?: "-"
            ErrorReporting.reporter.setKey(SCREEN_KEY, screen)
            ErrorReporting.reporter.log("screen: $screen")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
}

/** Ключ отчёта: экран, открытый в момент события. */
private const val SCREEN_KEY = "screen"

private fun NavHostController.execute(command: NavCommand, routes: GraphRoutes) {
    when (command) {
        is NavCommand.Open -> open(command.destination, routes)
        is NavCommand.Replace -> replaceCurrent(routes.toRoute(command.destination))
        is NavCommand.Root -> makeRoot(routes.toRoute(command.destination))
        NavCommand.Back -> popBackStack()
    }
}

private fun NavHostController.open(destination: Destination, routes: GraphRoutes) {
    if (destination is Destination.Tab) {
        switchTab(routes.toRoute(destination), routes.tabRoot)
        return
    }
    navigate(routes.toRoute(destination))
}

/**
 * Переход по вкладке — механика multiple back stacks: стек покидаемого раздела сохраняется
 * целиком (`saveState`), стек открываемого — восстанавливается (`restoreState`). Поэтому
 * «Каталог → карточка тайтла → Моё → Каталог» возвращает в карточку, а не на корень раздела.
 *
 * Сохранённый стек кладётся под ключ САМОЙ ГЛУБОКОЙ снятой записи, а достаётся по цели перехода —
 * поэтому и попап, и переход адресуют экраны-корни разделов. С вложенными графами (`popUpTo` до
 * графа, переход на граф) ключи не сходятся, и стек молча не восстанавливается.
 *
 * Попап — до [tabRoot], реального корня вкладок: стартовый destination графа это сплэш,
 * которого после логина в стеке нет (popUpTo{inclusive}), и попап по нему молча не срабатывал.
 */
private fun NavHostController.switchTab(route: Any, tabRoot: Any) {
    navigate(route) {
        popUpTo(tabRoot::class) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Заменить текущий экран. Попап по id текущего destination, а не по маршруту: следующая серия —
 * это тот же `PlayerRoute` с другими аргументами, и снимать надо именно открытую запись, иначе
 * перещёлкивание серий копит стек.
 */
private fun NavHostController.replaceCurrent(route: Any) {
    val current = currentDestination?.id
    navigate(route) {
        if (current != null) popUpTo(current) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Новый корень: чистим ВЕСЬ стек. При протухании сессии посреди работы онбординг иначе встаёт
 * ПОВЕРХ авторизованных экранов, и «Назад» возвращает на них.
 */
private fun NavHostController.makeRoot(route: Any) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

/** Маршруты телефонного графа. */
internal val PhoneRoutes = GraphRoutes(tabRoot = HomeRoute, toRoute = Destination::toPhoneRoute)

/**
 * Адрес → маршрут телефонного графа. `SearchRoute` отдаёт Каталог, `LibraryRoute` — «Моё»:
 * маршруты назывались так до переименования разделов.
 */
private fun Destination.toPhoneRoute(): Any = when (this) {
    is Destination.Tab -> toPhoneTabRoute()
    Destination.Onboarding -> OnboardingRoute
    Destination.DeviceSettings -> DeviceSettingsRoute
    is Destination.Feedback -> FeedbackRoute(trigger)
    Destination.DesignSystem -> DesignSystemRoute
    is Destination.Details -> DetailsRoute(itemId)
    is Destination.Player -> PlayerRoute(itemId = itemId, videoId = videoId, season = season)
    is Destination.Trailer -> TrailerRoute(url = url, title = title)
    is Destination.CollectionDetail -> CollectionDetailRoute(collectionId = collectionId, title = title)
    is Destination.Filmography -> FilmographyRoute(name = name, isDirector = isDirector)
    // Папка на телефоне открывается внутри Библиотеки, отдельного экрана у неё нет.
    is Destination.BookmarkFolder -> error("BookmarkFolder не существует в телефонном графе")
}

/** Разделы нижней навигации — отдельно: общая таблица иначе за порогом сложности detekt. */
private fun Destination.Tab.toPhoneTabRoute(): Any = when (this) {
    Destination.Home -> HomeRoute
    Destination.Catalog -> SearchRoute
    Destination.Mine -> LibraryRoute
    Destination.Profile -> ProfileRoute
}
