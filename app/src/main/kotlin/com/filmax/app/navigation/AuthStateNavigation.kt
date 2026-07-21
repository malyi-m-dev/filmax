package com.filmax.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController

/**
 * Единая реакция графа на смену auth-состояния (телефонный и TV-граф передают свои маршруты).
 *
 * popUpTo(0) — чистим ВЕСЬ стек, а не только сплэш: при протухании сессии посреди работы
 * онбординг вставал ПОВЕРХ авторизованных экранов («Назад» возвращал на них), а после logout
 * к онбордингу колбэка профиля добавлялся второй экземпляр от этого эффекта. Онбординг/главная
 * всегда единственный корень — «Назад» с онбординга закрывает приложение (гостевого режима нет).
 */
@Composable
internal fun AuthStateNavigation(
    isAuthenticated: Boolean?,
    navController: NavHostController,
    homeRoute: Any,
    onboardingRoute: Any,
) {
    LaunchedEffect(isAuthenticated) {
        val authenticated = isAuthenticated ?: return@LaunchedEffect
        navController.navigate(if (authenticated) homeRoute else onboardingRoute) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
}
