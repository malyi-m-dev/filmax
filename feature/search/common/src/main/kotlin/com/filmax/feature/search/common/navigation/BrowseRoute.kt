package com.filmax.feature.search.common.navigation

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.catalog.CatalogSection
import kotlinx.serialization.Serializable

/**
 * Раздел каталога поверх вкладки: «Фильмы», «4K и HDR» с Главной. [section] — имя [CatalogSection].
 * Живёт в слое логики: его читает SearchScreenModel — выборку раздела задаёт модель, а не экран.
 */
@Serializable
data class BrowseRoute(val section: String)

/**
 * Раздел, с которым открыт экран; у вкладки каталога аргумента нет — null. Ключ — имя свойства
 * [BrowseRoute.section]: так его кладёт в SavedStateHandle навигация.
 */
fun SavedStateHandle.browseSection(): CatalogSection? = CatalogSection.from(get<String>(SECTION_ARG))

private const val SECTION_ARG = "section"
