package com.filmax.app.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.filmax.app.tv.navigation.TvRoutes
import com.filmax.core.navigation.AppNavigator
import com.filmax.core.navigation.Destination
import com.filmax.feature.details.common.navigation.DetailsRoute
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * Команды навигации исполняет только активити на экране. Очередь у [AppNavigator] одна на
 * приложение, и скрытая активити (лаунчер открыл приложение второй раз поверх первого) забирала
 * бы каждую вторую команду: на экране нажатие пропадало, а плеер открывался за кадром.
 */
@RunWith(RobolectricTestRunner::class)
// Своё Application: приложение поднимает Koin в onCreate, а тесту Koin не нужен.
@Config(sdk = [34], application = Application::class)
class NavigationCommandsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val navigator = AppNavigator()
    private val screen = ScreenLifecycle()
    private lateinit var navController: NavHostController

    @Test
    fun `hidden activity leaves the command in the queue until it is shown`() {
        showGraph()

        navigator.open(Destination.Details(ITEM_ID))
        compose.waitForIdle()
        assertTrue(isOn<TvHomeRoute>(), "скрытая активити не должна исполнять команду")

        compose.runOnIdle { screen.registry.currentState = Lifecycle.State.STARTED }
        compose.waitForIdle()
        assertTrue(isOn<DetailsRoute>(), "команда должна дождаться активити на экране")
    }

    private fun showGraph() {
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides screen) {
                navController = rememberNavController()
                NavHost(navController, startDestination = TvHomeRoute) {
                    composable<TvHomeRoute> {}
                    composable<DetailsRoute> {}
                }
                NavigationCommands(navController, TvRoutes, navigator)
            }
        }
        compose.waitForIdle()
    }

    private inline fun <reified T : Any> isOn(): Boolean =
        compose.runOnIdle { navController.currentDestination?.hasRoute<T>() == true }

    /** Жизненный цикл активити, которой управляет тест: создана, но не на экране. */
    private class ScreenLifecycle : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.CREATED }
        override val lifecycle: Lifecycle get() = registry
    }

    private companion object {
        const val ITEM_ID = 42
    }
}
