package com.filmax.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled._4k
import androidx.compose.ui.graphics.vector.ImageVector
import com.filmax.core.domain.catalog.CatalogSection

/** Название раздела: подпись плитки «Разделы», чипа Главной и заголовок открытого раздела. */
val CatalogSection.label: String
    get() = when (this) {
        CatalogSection.MOVIES -> "Фильмы"
        CatalogSection.SERIES -> "Сериалы"
        CatalogSection.ANIME -> "Аниме"
        CatalogSection.DOCUMENTARY -> "Документальное"
        CatalogSection.UHD -> "4K и HDR"
        CatalogSection.FRESH -> "Новинки"
    }

/** Иконка раздела на плитке. */
val CatalogSection.icon: ImageVector
    get() = when (this) {
        CatalogSection.MOVIES -> Icons.Filled.Movie
        CatalogSection.SERIES -> Icons.Filled.LiveTv
        CatalogSection.ANIME -> Icons.Filled.AutoAwesome
        CatalogSection.DOCUMENTARY -> Icons.Filled.Public
        CatalogSection.UHD -> Icons.Filled._4k
        CatalogSection.FRESH -> Icons.Filled.NewReleases
    }

/** Оттенок плитки раздела (oklch hue из макета). */
val CatalogSection.hue: Float
    get() = when (this) {
        CatalogSection.MOVIES -> 290f
        CatalogSection.SERIES -> 250f
        CatalogSection.ANIME -> 330f
        CatalogSection.DOCUMENTARY -> 170f
        CatalogSection.UHD -> 200f
        CatalogSection.FRESH -> 50f
    }
