package com.filmax.app.navigation

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.source.ContentSourceRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.deviceDescription
import com.filmax.core.domain.user.model.initials
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.profile.common.accountCaption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class RootScreenModel(
    private val auth: AuthRepository,
    private val user: UserRepository,
    private val client: ClientInfo,
    private val sources: ContentSourceRepository,
) : BaseScreenModel<RootState, RootSideEffect, RootEvent>(RootState()) {

    init {
        onFetchData()
    }

    override fun onEvent(event: RootEvent) = Unit

    /**
     * Вход — парой с источником: граф возвращается к корню и по входу/выходу, и по смене
     * источника, когда вход есть в обоих (иначе на экране остались бы тайтлы прежнего).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onFetchData() {
        screenModelScope {
            sources.source
                .flatMapLatest { source -> auth.isAuthenticated.map { signedIn -> source to signedIn } }
                .distinctUntilChanged()
                .collect { (source, signedIn) ->
                    updateState { it.copy(isAuthenticated = signedIn, source = source, account = AccountSummary()) }
                    // Авторизованы — подтягиваем аккаунт для меню и подписываем устройство (best-effort).
                    if (signedIn) {
                        fetchAccount(source)
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

    private suspend fun fetchAccount(source: ContentSource) {
        (user.getProfile() as? RequestResult.Success)?.let { result ->
            val profile = result.data
            updateState {
                it.copy(
                    account = AccountSummary(
                        name = profile.username,
                        subscription = accountCaption(source, profile.subscription),
                        initials = profile.initials(),
                    ),
                )
            }
        }
    }
}
