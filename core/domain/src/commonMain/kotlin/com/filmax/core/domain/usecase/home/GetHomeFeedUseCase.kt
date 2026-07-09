package com.filmax.core.domain.usecase.home

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.LastValueCache
import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.watching.WatchingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Собирает данные главного экрана из нескольких параллельных запросов.
 * Общая бизнес-логика для Android-ScreenModel и iOS-ViewModel.
 *
 * Офлайн-устойчивость (issue #42): последняя успешная лента кэшируется ([cache]); если очередной
 * запрос ничего не вернул (сеть/сбой) — отдаём кэш с флагом [HomeFeed.fromCache], чтобы UI показал
 * прежний контент + баннер «нет сети» вместо пустого экрана с ошибкой.
 */
class GetHomeFeedUseCase(
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
    private val cache: LastValueCache<HomeFeed>,
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

        val feed = HomeFeed(
            hero = hot.getOrNull()?.items?.firstOrNull(),
            continueWatching = history.getOrNull()?.take(CONTINUE_WATCHING_LIMIT) ?: emptyList(),
            collections = collections.getOrNull()?.take(COLLECTIONS_LIMIT) ?: emptyList(),
            trending = trending.getOrNull()?.items?.take(ROW_LIMIT) ?: emptyList(),
            forYou = forYou.getOrNull()?.items?.take(ROW_LIMIT) ?: emptyList(),
            error = firstErrorMessage(hot, trending, collections, forYou),
        )

        when {
            // Полный успех — кэшируем целиком (fromCache уже false).
            feed.error == null && feed.hasContent -> {
                cache.put(feed)
                feed
            }
            // Частичный успех: показываем, что пришло, но НЕ портим более полный прежний кэш.
            feed.hasContent -> feed
            // Пусто (офлайн/сбой): есть кэш → отдаём его как stale + баннер; нет → ошибка-модалка.
            else -> cache.get()?.copy(error = feed.error, fromCache = true) ?: feed
        }
    }

    private companion object {
        /** Сколько последних тайтлов показать в блоке «Продолжить просмотр». */
        const val CONTINUE_WATCHING_LIMIT = 5

        /** Сколько подборок показать в горизонтальном ряду. */
        const val COLLECTIONS_LIMIT = 5

        /** Сколько тайтлов показать в горизонтальных рядах («В тренде», «Для вас»). */
        const val ROW_LIMIT = 10
    }
}
