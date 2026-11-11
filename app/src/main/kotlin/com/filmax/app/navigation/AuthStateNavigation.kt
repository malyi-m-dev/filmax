package com.filmax.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.filmax.core.domain.source.ContentSource

/**
 * Маршруты графа, которые трогает смена входа: куда вести с входом и без него и корни вкладок,
 * чьи сохранённые стеки надо стереть. Телефонный и ТВ-граф передают свои.
 */
internal data class AuthRoutes(
    val home: Any,
    val onboarding: Any,
    val tabRoots: List<Any>,
)

/**
 * Единая реакция графа на смену auth-состояния (телефонный и TV-граф передают свои маршруты).
 * Смена источника ([source]) — такая же смена: у нового источника свой вход и свои тайтлы.
 *
 * popUpTo(0) — чистим ВЕСЬ стек, а не только сплэш: при протухании сессии посреди работы
 * онбординг вставал ПОВЕРХ авторизованных экранов («Назад» возвращал на них), а после logout
 * к онбордингу колбэка профиля добавлялся второй экземпляр от этого эффекта. Онбординг/главная
 * всегда единственный корень — «Назад» с онбординга закрывает приложение (гостевого режима нет).
 */
@Composable
internal fun AuthStateNavigation(
    isAuthenticated: Boolean?,
    source: ContentSource,
    navController: NavHostController,
    routes: AuthRoutes,
) {
    LaunchedEffect(isAuthenticated, source) {
        val authenticated = isAuthenticated ?: return@LaunchedEffect
        // Сохранённые стеки вкладок (switchTab, saveState) живут отдельно от текущего стека и
        // popUpTo(0) не снимает их: «Каталог» восстановился бы с тайтлами прежнего источника или
        // аккаунта. Стираем их вместе с их ScreenModel'ями.
        routes.tabRoots.forEach { root -> navController.clearBackStack(root) }
        navController.navigate(if (authenticated) routes.home else routes.onboarding) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
}
