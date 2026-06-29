package com.filmax.feature.profile

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel

class ProfileScreenModel(
    private val user: UserRepository,
    private val watching: WatchingRepository,
    private val auth: AuthRepository,
    private val favorites: FavoritesRepository,
) : BaseScreenModel<ProfileState, ProfileSideEffect, ProfileEvent>(ProfileState()) {

    init {
        onFetchData()
        observeFavorites()
    }

    private fun observeFavorites() {
        screenModelScope {
            favorites.favorites.collect { items ->
                updateState { it.copy(favoritesCount = items.size) }
            }
        }
    }

    override fun dispatch(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Logout -> logout()
        }
    }

    override fun onFetchData() {
        screenModelScope {
            // Профиль — основной запрос: от него зависит отрисовка экрана.
            when (val result = user.getProfile()) {
                is RequestResult.Success ->
                    updateState { it.copy(loading = false, profile = result.data) }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    return@screenModelScope
                }
            }

            // Статистика — best-effort: ошибки не блокируют экран, поля остаются по умолчанию.
            (watching.getHistory() as? RequestResult.Success)?.let { history ->
                updateState { it.copy(watchedCount = history.data.size) }
            }
            // «В избранном» — из локального кэша favorites (см. observeFavorites).
            (user.getDeviceSettings() as? RequestResult.Success)?.let { device ->
                updateState { it.copy(quality = device.data.toQualityLabel()) }
            }
        }
    }

    private fun logout() {
        screenModelScope {
            auth.logout()
            postSideEffect(ProfileSideEffect.LoggedOut)
        }
    }
}

private fun DeviceSettings.toQualityLabel(): String = when {
    support4k && supportHdr -> "4K HDR"
    support4k -> "4K"
    supportHevc -> "HEVC"
    else -> "HD"
}
