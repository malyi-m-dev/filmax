package com.filmax.core.domain.usecase.home

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.watching.WatchingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Собирает данные главного экрана из нескольких параллельных запросов.
 * Общая бизнес-логика для Android-ScreenModel и iOS-ViewModel.
 */
class GetHomeFeedUseCase(
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
) {
    suspend operator fun invoke(): HomeFeed = coroutineScope {
        val hotDeferred = async { catalog.getHotItems(ItemType.MOVIE) }
        val trendingDeferred = async { catalog.getItems(ItemType.MOVIE, CatalogSort.VIEWS) }
        val collectionsDeferred = async { catalog.getCollections() }
        val forYouDeferred = async { catalog.getItems(ItemType.SERIES, CatalogSort.RATING) }
        val historyDeferred = async { watching.getHistory() }

        val hot = hotDeferred.await()
        val trending = trendingDeferred.await()
        val collections = collectionsDeferred.await()
        val forYou = forYouDeferred.await()
        val history = historyDeferred.await()

        HomeFeed(
            hero = hot.getOrNull()?.items?.firstOrNull(),
            continueWatching = history.getOrNull()?.take(5) ?: emptyList(),
            collections = collections.getOrNull()?.take(5) ?: emptyList(),
            trending = trending.getOrNull()?.items?.take(10) ?: emptyList(),
            forYou = forYou.getOrNull()?.items?.take(10) ?: emptyList(),
            error = firstErrorMessage(hot, trending, collections, forYou),
        )
    }
}
