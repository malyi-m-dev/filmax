package com.filmax.feature.player.common.navigation

import kotlinx.serialization.Serializable

/**
 * Маршрут плеера. Живёт в слое логики — его читает PlayerScreenModel через SavedStateHandle.
 *
 * [videoId] — НОМЕР видео (`number` из API), а не id трека: тем же числом kino.pub принимает
 * и отдаёт прогресс. `-1` (по умолчанию) = первый/единственный трек (фильм).
 *
 * [season] обязателен для сериала: номера серий уникальны только ВНУТРИ сезона, и без него
 * «S3E2» находил бы вторую серию первого сезона. `-1` — не сериал/сезон неизвестен.
 *
 * [startSeconds] — с какой секунды начать, когда просмотр передали с телефона: телефон знает
 * позицию точнее, чем kino.pub (прогресс туда уходит шагами). `-1` — с сохранённого прогресса.
 */
@Serializable
data class PlayerRoute(val itemId: Int, val videoId: Int = -1, val season: Int = -1, val startSeconds: Int = -1)
