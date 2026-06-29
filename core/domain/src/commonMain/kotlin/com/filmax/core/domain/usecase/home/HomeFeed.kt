package com.filmax.core.domain.usecase.home

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory

/**
 * Готовые к показу данные главного экрана. Композитная модель — собирается из нескольких
 * запросов каталога/истории в [GetHomeFeedUseCase]; [error] непустой, если часть запросов упала
 * (частичная выдача сохраняется, как в текущем поведении Android).
 */
data class HomeFeed(
    val hero: Item?,
    val continueWatching: List<WatchHistory>,
    val collections: List<Collection>,
    val trending: List<Item>,
    val forYou: List<Item>,
    val error: String?,
)
