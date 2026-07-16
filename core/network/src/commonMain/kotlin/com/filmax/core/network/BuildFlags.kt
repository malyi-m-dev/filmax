package com.filmax.core.network

/**
 * Отладочная ли это сборка. Нужен сетевому слою, чтобы включать HTTP-логи только в debug:
 * логи пишут URL, параметры и тела ответов — в проде это лишний шум и лишние данные в logcat.
 */
internal expect val isDebugBuild: Boolean
