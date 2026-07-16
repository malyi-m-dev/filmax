package com.filmax.data.watching.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryResponseDto(
    val items: List<HistoryItemDto>,
    val pagination: PaginationDto? = null,
)

/**
 * Ответ `api/v1/history` — единственный источник прогресса списком.
 *
 * `watching/{type}` отдаёт только id/title/posters без `watching`, поэтому «Продолжить» на нём
 * построить нельзя: доля просмотра всегда выходила нулевой.
 */
@Serializable
data class HistoryListResponseDto(
    val history: List<HistoryEntryDto> = emptyList(),
    val pagination: PaginationDto? = null,
)

@Serializable
data class HistoryEntryDto(
    /** Просмотрено секунд — по конкретному [media], а не по тайтлу целиком. */
    val time: Int = 0,
    val item: HistoryEntryItemDto,
    val media: HistoryMediaDto? = null,
)

@Serializable
data class HistoryEntryItemDto(
    val id: Int,
    val title: String = "",
    val type: String = "",
    val posters: PostersDto? = null,
    val duration: HistoryDurationDto? = null,
)

/** Конкретное видео: серия сериала или единственная дорожка фильма. */
@Serializable
data class HistoryMediaDto(
    /** Номер видео — им же kino.pub принимает и отдаёт прогресс (`marktime?video=`). */
    val number: Int = 0,
    /** Номер сезона; 0 — у фильма. */
    val snumber: Int = 0,
    /** Кадр серии 16:9 — лучшая картинка для широкой карточки. */
    val thumbnail: String = "",
    val duration: Int = 0,
)

@Serializable
data class HistoryDurationDto(
    /** Средняя длительность серии; у фильма — его длительность. */
    val average: Double = 0.0,
    val total: Int = 0,
)

@Serializable
data class HistoryItemDto(
    val id: Int,
    val title: String,
    val type: String = "",
    val posters: PostersDto? = null,
    val watching: WatchProgressDto? = null,
)

@Serializable
data class WatchProgressDto(
    val status: Int = 0,
    val time: Int? = null,
    val duration: Int? = null,
    val video: Int? = null,
    val season: Int? = null,
)

@Serializable
data class PostersDto(
    val small: String = "",
    val medium: String = "",
    val big: String = "",
    // Кадр 16:9. Карточки «Продолжить»/«История» — широкие, и вертикальный постер 2:3 в них
    // обрезается по центру в кашу. Пустая строка — если бэкенд кадра не отдал.
    val wide: String = "",
)

@Serializable
data class PaginationDto(
    val total: Int = 0,
    val current: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
)

@Serializable
data class NotificationsDto(
    val notifications: List<NotificationDto>? = null,
    val unread: Int = 0,
)

@Serializable
data class NotificationDto(
    val id: Int,
    val title: String? = null,
    val text: String? = null,
    @SerialName("created_at") val createdAt: Int? = null,
    val read: Boolean = false,
    val type: String? = null,
    @SerialName("item_id") val itemId: Int? = null,
)
