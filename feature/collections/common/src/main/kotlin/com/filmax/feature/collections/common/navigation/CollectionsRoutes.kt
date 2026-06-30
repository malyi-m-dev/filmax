package com.filmax.feature.collections.common.navigation

import kotlinx.serialization.Serializable

/** Раздел «Подборки». */
@Serializable
object CollectionsRoute

/** Экран одной подборки. Живёт в слое логики — его читает CollectionDetailScreenModel. */
@Serializable
data class CollectionDetailRoute(val collectionId: Int, val title: String)
