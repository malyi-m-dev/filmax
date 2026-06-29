package com.filmax.shared

import com.filmax.core.domain.usecase.auth.LogoutUseCase
import com.filmax.core.domain.usecase.auth.ObserveAuthStateUseCase
import com.filmax.core.domain.usecase.auth.PollForTokenUseCase
import com.filmax.core.domain.usecase.auth.RequestDeviceCodeUseCase
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchlistUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Фабрики UseCase для Swift: Koin reified-`get()` недоступен из Obj-C/Swift,
 * поэтому iOS-ViewModel получают UseCase через явные методы этого объекта.
 */
object UseCaseProvider : KoinComponent {
    fun getHomeFeedUseCase(): GetHomeFeedUseCase = get()
    fun observeAuthStateUseCase(): ObserveAuthStateUseCase = get()
    fun requestDeviceCodeUseCase(): RequestDeviceCodeUseCase = get()
    fun pollForTokenUseCase(): PollForTokenUseCase = get()
    fun logoutUseCase(): LogoutUseCase = get()
    fun toggleWatchlistUseCase(): ToggleWatchlistUseCase = get()
    fun toggleWatchedUseCase(): ToggleWatchedUseCase = get()
}
