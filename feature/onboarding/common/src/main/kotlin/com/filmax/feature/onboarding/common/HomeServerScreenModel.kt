package com.filmax.feature.onboarding.common

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.error.toAppError
import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.source.ContentSourceRepository
import com.filmax.core.domain.source.HomeServer
import com.filmax.core.domain.source.HomeServerRepository
import com.filmax.core.domain.source.QuickConnectCode
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.core.presentation.input.SPACE_KEY
import com.filmax.core.presentation.input.erasedText
import com.filmax.core.presentation.input.typedRaw
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Вход на домашний сервер Jellyfin: найти сервер в сети или ввести адрес, затем войти по коду
 * Quick Connect или паролем. Общий для телефона и ТВ: телефон печатает в обычные поля, ТВ —
 * экранной клавиатурой в поле [HomeServerState.field].
 *
 * После входа источник переключается на сервер: граф сам уводит на Главную нового источника.
 */
// По короткому обработчику на событие: поиск, подключение, ввод, два способа входа.
@Suppress("TooManyFunctions")
class HomeServerScreenModel(
    private val servers: HomeServerRepository,
    private val sources: ContentSourceRepository,
    private val client: ClientInfo,
) : BaseScreenModel<HomeServerState, HomeServerSideEffect, HomeServerEvent>(HomeServerState()) {

    /** Ожидание подтверждения кода; отменяется уходом на пароль или к выбору сервера. */
    private var polling: Job? = null

    init {
        onFetchData()
    }

    override fun onFetchData() = search()

    override fun onEvent(event: HomeServerEvent) {
        when (event) {
            HomeServerEvent.Search -> search()
            is HomeServerEvent.Choose -> connect(event.server.address)
            HomeServerEvent.Connect -> connect(state.address)
            is HomeServerEvent.SetAddress -> update { it.copy(address = event.text, error = null) }
            is HomeServerEvent.SetUsername -> update { it.copy(username = event.text, error = null) }
            is HomeServerEvent.SetPassword -> update { it.copy(password = event.text, error = null) }
            is HomeServerEvent.Focus -> update { it.copy(field = event.field) }
            is HomeServerEvent.Key -> update { it.typed(event.input) }
            HomeServerEvent.SignIn -> signIn()
            HomeServerEvent.UseCode -> state.server?.let(::startCode)
            HomeServerEvent.UsePassword -> usePassword()
            HomeServerEvent.ChangeServer -> changeServer()
        }
    }

    private fun search() {
        screenModelScope { _ ->
            updateState { it.copy(searching = true) }
            val found = servers.discover()
            updateState { current ->
                // Один найденный сервер и пустое поле — адрес подставляется: на ТВ останется «ОК».
                val address = current.address.ifEmpty { found.singleOrNull()?.address.orEmpty() }
                current.copy(searching = false, found = found, address = address)
            }
        }
    }

    private fun connect(address: String) {
        screenModelScope { _ ->
            updateState { it.copy(busy = true, error = null) }
            when (val result = servers.connect(address)) {
                is RequestResult.Success -> onConnected(result.data)
                is RequestResult.Error -> updateState { it.copy(busy = false, error = connectError(result)) }
            }
        }
    }

    /**
     * На ТВ по умолчанию — код: пароль пультом набирать долго, а подтвердить вход в Jellyfin на
     * телефоне — пара нажатий. На телефоне — пароль: печатать удобно, второго устройства не нужно.
     */
    private suspend fun onConnected(server: HomeServer) {
        val code = server.quickConnect && client.isTv
        updateState {
            it.copy(
                busy = false,
                server = server,
                address = server.address,
                step = if (code) HomeServerStep.CODE else HomeServerStep.PASSWORD,
                field = HomeServerField.USERNAME,
            )
        }
        if (code) startCode(server)
    }

    private fun startCode(server: HomeServer) {
        polling?.cancel()
        polling = screenModelScope { _ ->
            updateState { it.copy(step = HomeServerStep.CODE, code = null, error = null) }
            when (val started = servers.startQuickConnect(server)) {
                is RequestResult.Success -> {
                    updateState { it.copy(code = started.data.code) }
                    waitForApproval(server, started.data)
                }
                is RequestResult.Error -> updateState { it.copy(error = CODE_UNAVAILABLE) }
            }
        }
    }

    /** Ждём подтверждения, пока код жив на сервере; ошибка значит, что код истёк или сервер пропал. */
    private suspend fun waitForApproval(server: HomeServer, code: QuickConnectCode) {
        repeat(CODE_CHECKS) { _ ->
            delay(CODE_CHECK_MS)
            when (val check = servers.checkQuickConnect(server, code)) {
                is RequestResult.Success -> if (check.data) return finish()
                is RequestResult.Error -> return updateState { it.copy(error = CODE_EXPIRED) }
            }
        }
        updateState { it.copy(error = CODE_EXPIRED) }
    }

    private fun signIn() {
        val server = state.server ?: return
        screenModelScope { current ->
            if (current.busy) return@screenModelScope
            updateState { it.copy(busy = true, error = null) }
            when (val result = servers.login(server, current.username.trim(), current.password)) {
                is RequestResult.Success -> finish()
                is RequestResult.Error -> updateState { it.copy(busy = false, error = signInError(result)) }
            }
        }
    }

    private suspend fun finish() {
        sources.select(ContentSource.JELLYFIN)
        updateState { it.copy(busy = false) }
        postSideEffect(HomeServerSideEffect.SignedIn)
    }

    private fun usePassword() {
        polling?.cancel()
        update { it.copy(step = HomeServerStep.PASSWORD, field = HomeServerField.USERNAME, code = null, error = null) }
    }

    private fun changeServer() {
        polling?.cancel()
        update {
            it.copy(
                step = HomeServerStep.SERVER,
                server = null,
                code = null,
                field = HomeServerField.ADDRESS,
                error = null,
            )
        }
    }

    private fun update(transform: (HomeServerState) -> HomeServerState) {
        screenModelScope { _ -> updateState(transform) }
    }

    private companion object {
        /** Сервер держит код несколько минут; проверяем раз в три секунды — как веб-клиент Jellyfin. */
        const val CODE_CHECK_MS = 3_000L
        const val CODE_CHECKS = 100

        const val CODE_UNAVAILABLE = "Вход по коду на этом сервере выключен — войдите по паролю"
        const val CODE_EXPIRED = "Код истёк. Запросите новый или войдите по паролю"

        fun connectError(error: RequestResult.Error): String = when (error.toAppError()) {
            AppError.Offline, AppError.Timeout ->
                "Сервер не отвечает. Проверьте адрес и что устройство в той же сети"
            AppError.NotFound -> "По этому адресу не Jellyfin"
            else -> error.message ?: "Не удалось подключиться"
        }

        fun signInError(error: RequestResult.Error): String = when (error.toAppError()) {
            AppError.Auth -> "Неверное имя пользователя или пароль"
            AppError.Offline, AppError.Timeout -> "Сервер не отвечает"
            else -> error.message ?: "Не удалось войти"
        }

        /** Нажатие клавиатуры ТВ — в выбранное поле, без правил текста для поиска (см. typedRaw). */
        fun HomeServerState.typed(input: ServerKey): HomeServerState = when (input) {
            is ServerKey.Layout -> copy(layout = input.layout)
            is ServerKey.Letter -> edit { typedRaw(it, input.key, MAX_INPUT) }
            ServerKey.Space -> edit { typedRaw(it, SPACE_KEY, MAX_INPUT) }
            ServerKey.Erase -> edit(::erasedText)
        }

        fun HomeServerState.edit(change: (String) -> String): HomeServerState = when (field) {
            HomeServerField.ADDRESS -> copy(address = change(address), error = null)
            HomeServerField.USERNAME -> copy(username = change(username), error = null)
            HomeServerField.PASSWORD -> copy(password = change(password), error = null)
        }

        const val MAX_INPUT = 120
    }
}
