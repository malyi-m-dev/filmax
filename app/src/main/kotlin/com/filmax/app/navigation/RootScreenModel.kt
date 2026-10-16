package com.filmax.app.navigation

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.deviceDescription
import com.filmax.core.domain.user.model.initials
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.profile.common.label

class RootScreenModel(
    private val auth: AuthRepository,
    private val user: UserRepository,
    private val client: ClientInfo,
) : BaseScreenModel<RootState, RootSideEffect, RootEvent>(RootState()) {

    init {
        onFetchData()
    }

    override fun onEvent(event: RootEvent) = Unit

    override fun onFetchData() {
        screenModelScope {
            auth.isAuthenticated.collect { value ->
                updateState { it.copy(isAuthenticated = value) }
                // Авторизованы — подтягиваем аккаунт для меню и подписываем устройство (best-effort).
                if (value == true) {
                    fetchAccount()
                    describeDevice()
                }
            }
        }
    }

    /**
     * Подпись устройства в аккаунте kino.pub — при каждом запуске с входом. Раньше её слал экран
     * входа, но навигация снимала его, как только сохранялся токен, и запрос отменялся раньше,
     * чем доходил до сервера: устройства в аккаунте оставались «unknown». Этот ScreenModel живёт
     * всё время работы приложения, а каждый запуск заодно освежает версию в подписи.
     */
    private suspend fun describeDevice() {
        val description = client.deviceDescription()
        user.registerDevice(description.title, description.hardware, description.software)
    }

    private suspend fun fetchAccount() {
        (user.getProfile() as? RequestResult.Success)?.let { result ->
            val profile = result.data
            updateState {
                it.copy(
                    account = AccountSummary(
                        name = profile.username,
                        subscription = profile.subscription.label(),
                        initials = profile.initials(),
                    ),
                )
            }
        }
    }
}
