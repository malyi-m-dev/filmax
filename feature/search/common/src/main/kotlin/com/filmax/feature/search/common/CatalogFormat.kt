// Общие для mobile и tv справочники каталога: чипы типов, поля сортировки и русские подписи.
// Чистые данные без UI-зависимостей.

package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.ItemType

/** Типы в порядке чипов. null — «Все» (объединение типов, см. SearchScreenModel). */
val TypeOptions = listOf(
    null to "Все",
    ItemType.MOVIE to "Фильмы",
    ItemType.SERIES to "Сериалы",
    ItemType.ANIME to "Аниме",
    ItemType.DOCUMENTARY to "Документальные",
)

/** Поля сортировки в порядке меню/перебора чипа. Русские подписи — из макета. */
val SortOptions = listOf(
    CatalogSort.UPDATED to "Обновлённые",
    CatalogSort.CREATED to "Добавленные",
    CatalogSort.RATING to "Рейтинг Filmax",
    CatalogSort.VIEWS to "Просмотры",
    CatalogSort.YEAR to "Год",
    CatalogSort.KINOPOISK_RATING to "Рейтинг КП",
    CatalogSort.IMDB_RATING to "Рейтинг IMDb",
)

/** Подпись под карточкой: тип по-русски. `serial`/`docuserial` из API зрителю не показываем. */
fun itemTypeLabel(type: ItemType): String = when (type) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY -> "Документальный"
    ItemType.TV -> "ТВ"
}

fun sortLabel(sort: CatalogSort): String =
    SortOptions.firstOrNull { it.first == sort }?.second ?: SortOptions.first().second
