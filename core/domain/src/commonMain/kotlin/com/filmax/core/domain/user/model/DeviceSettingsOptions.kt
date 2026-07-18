package com.filmax.core.domain.user.model

/**
 * Вариант выбора для настроек устройства (тип потока, сервер раздачи).
 *
 * [id] — то самое целое, что уходит в POST `device/{id}/settings` и приходит в `device/info`;
 * [label] — подпись для UI. Держим маппинг в домене, чтобы mobile и TV показывали одинаковые
 * подписи и не дублировали «магические» числа у себя.
 */
data class DeviceOption(val id: Int, val label: String)

/**
 * Тип потока. Значения kino.pub: HTTP (прогрессивная отдача) и HLS/HLS4 (адаптивный поток).
 * API в этом проекте отдаёт `streaming_type` числом, поэтому варианты пронумерованы по возрастанию
 * id так, как их принимает эндпоинт. Если бэкенд реально присылает строку — менять надо DTO и
 * домен (`streamingType: String`), а не этот список.
 */
val streamingTypeOptions: List<DeviceOption> = listOf(
    DeviceOption(id = 0, label = "HTTP"),
    DeviceOption(id = 1, label = "HLS"),
    DeviceOption(id = 2, label = "HLS4"),
)

/** Подпись типа потока по id; неизвестное значение показываем как есть, не роняя экран. */
fun streamingTypeLabel(streamingType: Int): String =
    streamingTypeOptions.firstOrNull { it.id == streamingType }?.label ?: "Тип $streamingType"

/** «Автоматически» — CDN выбирает сервер раздачи сам. Единственная известная нам локация. */
const val SERVER_LOCATION_AUTO = 0

/**
 * Подпись сервера раздачи. API отдаёт только текущий числовой id локации, без списка доступных,
 * поэтому осмысленно известен лишь 0 = «Автоматически». Прочие значения показываем как «Сервер N»,
 * не выдумывая несуществующих подписей. Полноценный выбор появится, когда DTO начнёт отдавать
 * список локаций (`options`/`allowed`) — тогда сюда добавится список вариантов.
 */
fun serverLocationLabel(serverLocation: Int): String =
    if (serverLocation == SERVER_LOCATION_AUTO) "Автоматически" else "Сервер $serverLocation"
