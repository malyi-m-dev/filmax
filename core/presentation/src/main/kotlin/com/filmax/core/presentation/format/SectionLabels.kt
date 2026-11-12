// Название раздела каталога — подпись, а не вёрстка: его показывает и модель (заголовок открытого
// раздела), и плитки «Разделы». Иконка и оттенок плитки остались в core:ui.
package com.filmax.core.presentation.format

import com.filmax.core.domain.catalog.CatalogSection

/** Название раздела: подпись плитки «Разделы», чипа Главной и заголовок открытого раздела. */
val CatalogSection.label: String
    get() = when (this) {
        CatalogSection.MOVIES -> "Фильмы"
        CatalogSection.SERIES -> "Сериалы"
        CatalogSection.ANIME -> "Аниме"
        CatalogSection.DOCUMENTARY -> "Документальное"
        CatalogSection.TV_SHOWS -> "ТВ-шоу"
        CatalogSection.CONCERTS -> "Концерты"
        CatalogSection.UHD -> "4K и HDR"
        CatalogSection.FRESH -> "Новинки"
    }
