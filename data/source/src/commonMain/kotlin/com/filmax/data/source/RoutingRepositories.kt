// Репозитории-переключатели: каждый вызов уходит в реализацию выбранного источника. Логики
// здесь нет намеренно — всё различие источников живёт в их data-модулях.
package com.filmax.data.source

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.auth.model.DeviceCode
import com.filmax.core.domain.auth.model.Token
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.playback.StreamRepository
import com.filmax.core.domain.search.SearchRepository
import kotlinx.coroutines.flow.Flow

internal class RoutingAuthRepository(private val switch: SourceSwitch<AuthRepository>) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> = switch.flow { it.isAuthenticated }

    override suspend fun requestDeviceCode(): RequestResult<DeviceCode> = switch.current.requestDeviceCode()

    override suspend fun pollForToken(code: String, username: String, timestamp: Long): RequestResult<Token> =
        switch.current.pollForToken(code, username, timestamp)

    override suspend fun refreshToken(refreshToken: String): RequestResult<Token> =
        switch.current.refreshToken(refreshToken)

    override suspend fun logout(): RequestResult<Unit> = switch.current.logout()
}

internal class RoutingSearchRepository(private val switch: SourceSwitch<SearchRepository>) : SearchRepository {

    override suspend fun search(query: String, type: ItemType?, perPage: Int): RequestResult<List<Item>> =
        switch.current.search(query, type, perPage)

    override suspend fun searchByActor(actor: String, perPage: Int): RequestResult<List<Item>> =
        switch.current.searchByActor(actor, perPage)

    override suspend fun searchByDirector(director: String, perPage: Int): RequestResult<List<Item>> =
        switch.current.searchByDirector(director, perPage)
}

internal class RoutingFavoritesRepository(
    private val switch: SourceSwitch<FavoritesRepository>,
) : FavoritesRepository {

    override val favorites: Flow<List<FavoriteItem>> = switch.flow { it.favorites }

    override val favoriteIds: Flow<Set<Int>> = switch.flow { it.favoriteIds }

    override fun isFavorite(id: Int): Flow<Boolean> = switch.flow { it.isFavorite(id) }

    override suspend fun toggle(item: FavoriteItem): Boolean = switch.current.toggle(item)

    override suspend fun add(item: FavoriteItem) = switch.current.add(item)

    override suspend fun remove(id: Int) = switch.current.remove(id)
}

internal class RoutingStreamRepository(private val switch: SourceSwitch<StreamRepository>) : StreamRepository {

    override suspend fun playable(itemId: Int, track: MediaTrack): RequestResult<MediaTrack> =
        switch.current.playable(itemId, track)
}
