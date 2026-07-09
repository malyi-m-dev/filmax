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
    /**
     * true — данные отданы из кэша при недоступной сети (issue #42): показываем ранее
     * загруженный контент + ненавязчивый баннер «нет сети» вместо блокирующей модалки.
     */
    val fromCache: Boolean = false,
) {
    /** Есть ли что показать — хотя бы одна секция непуста (иначе экран считается пустым). */
    val hasContent: Boolean
        get() = hero != null ||
            continueWatching.isNotEmpty() ||
            collections.isNotEmpty() ||
            trending.isNotEmpty() ||
            forYou.isNotEmpty()
}
