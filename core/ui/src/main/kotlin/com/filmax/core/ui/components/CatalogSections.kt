package com.filmax.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled._4k
import androidx.compose.ui.graphics.vector.ImageVector
import com.filmax.core.domain.catalog.CatalogSection

/** Иконка раздела на плитке. */
val CatalogSection.icon: ImageVector
    get() = when (this) {
        CatalogSection.MOVIES -> Icons.Filled.Movie
        CatalogSection.SERIES -> Icons.Filled.LiveTv
        CatalogSection.ANIME -> Icons.Filled.AutoAwesome
        CatalogSection.DOCUMENTARY -> Icons.Filled.Public
        CatalogSection.TV_SHOWS -> Icons.Filled.Mic
        CatalogSection.CONCERTS -> Icons.Filled.MusicNote
        CatalogSection.UHD -> Icons.Filled._4k
        CatalogSection.FRESH -> Icons.Filled.NewReleases
    }

/**
 * Оттенок плитки раздела (oklch hue из макета). У «ТВ-шоу» и «Концертов» макета нет — им отданы
 * свободные жёлтый и зелёный между «Новинками» и «Документальным», чтобы соседние плитки не
 * сливались, а красный оставался за ошибками.
 */
val CatalogSection.hue: Float
    get() = when (this) {
        CatalogSection.MOVIES -> 290f
        CatalogSection.SERIES -> 250f
        CatalogSection.ANIME -> 330f
        CatalogSection.DOCUMENTARY -> 170f
        CatalogSection.TV_SHOWS -> 90f
        CatalogSection.CONCERTS -> 130f
        CatalogSection.UHD -> 200f
        CatalogSection.FRESH -> 50f
    }
