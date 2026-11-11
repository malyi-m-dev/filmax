package com.filmax.core.network

import kotlinx.serialization.json.Json

/**
 * JSON всех клиентов: API присылают лишние поля и null там, где у модели значение по умолчанию —
 * одно такое поле не должно ронять разбор всего ответа.
 */
val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
