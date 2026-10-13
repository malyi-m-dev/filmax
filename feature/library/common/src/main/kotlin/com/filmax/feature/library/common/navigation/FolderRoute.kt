package com.filmax.feature.library.common.navigation

import kotlinx.serialization.Serializable

/** Маршрут открытой папки-закладки. Живёт в слое логики — его читает FolderScreenModel. */
@Serializable
data class FolderRoute(val folderId: Int)
