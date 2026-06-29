package com.filmax.core.domain.downloads.model

/** Метаданные скачанного фильма для отображения в библиотеке. */
data class DownloadedItem(
    val id: Int,
    val title: String,
    val posterSmall: String,
    val year: Int,
    val durationMinutes: Int,
)
