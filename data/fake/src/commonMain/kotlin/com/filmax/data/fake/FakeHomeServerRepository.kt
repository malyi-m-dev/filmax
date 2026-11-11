package com.filmax.data.fake

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.source.HomeServer
import com.filmax.core.domain.source.HomeServerRepository
import com.filmax.core.domain.source.QuickConnectCode

/**
 * Домашний сервер для юнит-тестов входа: находится в сети, пускает с [password] и подтверждает
 * код Quick Connect на [approveOnCheck]-й проверке.
 */
class FakeHomeServerRepository(
    var found: List<HomeServer> = listOf(HOME),
    private val password: String = "secret",
    private val approveOnCheck: Int = 2,
) : HomeServerRepository {

    var signedIn = false
        private set

    private var checks = 0

    override suspend fun discover(): List<HomeServer> = found

    override suspend fun connect(address: String): RequestResult<HomeServer> =
        found.firstOrNull { it.address == address }?.let { RequestResult.Success(it) }
            ?: RequestResult.Error("Failed to connect to $address")

    override suspend fun login(server: HomeServer, username: String, password: String): RequestResult<Unit> {
        if (password != this.password) return RequestResult.Error("Client request invalid: 401 Unauthorized")
        signedIn = true
        return RequestResult.Success(Unit)
    }

    override suspend fun startQuickConnect(server: HomeServer): RequestResult<QuickConnectCode> =
        if (server.quickConnect) {
            RequestResult.Success(QuickConnectCode(code = "482913", secret = "secret-qc"))
        } else {
            RequestResult.Error("Quick Connect выключен")
        }

    override suspend fun checkQuickConnect(server: HomeServer, code: QuickConnectCode): RequestResult<Boolean> {
        checks++
        signedIn = checks >= approveOnCheck
        return RequestResult.Success(signedIn)
    }

    companion object {
        val HOME = HomeServer(
            address = "http://192.168.1.20:8096",
            name = "Дом",
            version = "10.10.7",
            quickConnect = true,
        )
    }
}
