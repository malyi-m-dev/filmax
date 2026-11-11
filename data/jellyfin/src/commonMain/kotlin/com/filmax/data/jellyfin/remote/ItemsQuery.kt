package com.filmax.data.jellyfin.remote

/**
 * Параметры `GET /Items`. Одним объектом — фильтров у выдачи много, и функция с ними списком
 * упёрлась бы в порог detekt. Пустое значение — «не фильтровать».
 */
internal data class ItemsQuery(
    val includeItemTypes: List<String> = emptyList(),
    val sortBy: List<String> = emptyList(),
    val ascending: Boolean = false,
    val startIndex: Int = 0,
    val limit: Int? = null,
    val genreIds: List<String> = emptyList(),
    /** Жанры по имени: так задаются разделы вроде «Аниме», у которых на сервере нет своего типа. */
    val genres: List<String> = emptyList(),
    val searchTerm: String? = null,
    val parentId: String? = null,
    val filters: List<String> = emptyList(),
    val person: String? = null,
    val personTypes: List<String> = emptyList(),
    val is4K: Boolean? = null,
    val minCommunityRating: Double? = null,
    val minPremiereDate: String? = null,
    val maxPremiereDate: String? = null,
    val seriesStatus: List<String> = emptyList(),
    val imageTypes: List<String> = emptyList(),
    /** Рекурсивно по медиатекам: без этого `/Items` отдаёт только корневые папки. */
    val recursive: Boolean = true,
)
