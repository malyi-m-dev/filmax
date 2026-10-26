package com.filmax.core.presentation.labels

import com.filmax.core.domain.catalog.CatalogSection

/**
 * Название раздела каталога: подпись плитки «Разделы», чипа Главной и заголовок открытого
 * раздела. Живёт рядом с другими подписями, а не в core:ui: заголовок раздела считает модель
 * каталога, а иконка и оттенок плитки остаются вещами вёрстки.
 */
fun sectionLabel(section: CatalogSection): String = when (section) {
    CatalogSection.MOVIES -> "Фильмы"
    CatalogSection.SERIES -> "Сериалы"
    CatalogSection.ANIME -> "Аниме"
    CatalogSection.DOCUMENTARY -> "Документальное"
    CatalogSection.TV_SHOWS -> "ТВ-шоу"
    CatalogSection.CONCERTS -> "Концерты"
    CatalogSection.UHD -> "4K и HDR"
    CatalogSection.FRESH -> "Новинки"
}
