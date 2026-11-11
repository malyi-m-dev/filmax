package com.filmax.data.jellyfin

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.common.safeRequest
import com.filmax.core.domain.source.HomeServer
import com.filmax.core.domain.source.HomeServerRepository
import com.filmax.core.domain.source.QuickConnectCode
import com.filmax.data.jellyfin.remote.JellyfinApi
import com.filmax.data.jellyfin.remote.dto.AuthenticationResultDto

/** Вход на Jellyfin: поиск в сети, проверка адреса, пароль или код Quick Connect. */
internal class JellyfinServerRepository(
    private val api: JellyfinApi,
    private val session: JellyfinSession,
    private val discovery: JellyfinDiscovery,
) : HomeServerRepository {

    override suspend fun discover(): List<HomeServer> = discovery.find().mapNotNull { found ->
        val address = found.address?.let(::normalizeAddress) ?: return@mapNotNull null
        HomeServer(address = address, name = found.name ?: address)
    }

    override suspend fun connect(address: String): RequestResult<HomeServer> = safeRequest {
        val normalized = normalizeAddress(address)
        val info = api.publicInfo(normalized)
        // Quick Connect бывает выключен в настройках сервера — тогда остаётся вход по паролю.
        val quickConnect = safeRequest { api.quickConnectEnabled(normalized) }.getOrNull() == true
        HomeServer(
            address = normalized,
            name = info.serverName ?: normalized,
            version = info.version,
            quickConnect = quickConnect,
        )
    }

    override suspend fun login(server: HomeServer, username: String, password: String): RequestResult<Unit> =
        safeRequest { save(server, api.authenticateByName(server.address, username, password)) }

    override suspend fun startQuickConnect(server: HomeServer): RequestResult<QuickConnectCode> = safeRequest {
        val started = api.initiateQuickConnect(server.address)
        QuickConnectCode(
            code = started.code ?: error("Сервер не выдал код"),
            secret = started.secret ?: error("Сервер не выдал код"),
        )
    }

    override suspend fun checkQuickConnect(server: HomeServer, code: QuickConnectCode): RequestResult<Boolean> =
        safeRequest {
            val approved = api.quickConnectState(server.address, code.secret).authenticated == true
            if (approved) save(server, api.authenticateWithQuickConnect(server.address, code.secret))
            approved
        }

    private fun save(server: HomeServer, result: AuthenticationResultDto) {
        val user = result.user ?: error("Сервер не прислал пользователя")
        session.save(
            StoredSession(
                address = server.address,
                serverName = server.name,
                userId = user.id ?: error("Сервер не прислал пользователя"),
                userName = user.name.orEmpty(),
                token = result.accessToken ?: error("Сервер не выдал токен"),
            ),
        )
    }
}

/**
 * Адрес, как его вводят люди («192.168.1.20», «nas.local:8096», «https://media.example.ru»), —
 * в базовый адрес сервера. Без схемы — http и, если порта нет, стандартный 8096: дома сервер
 * обычно так и стоит. Со схемой адрес берётся как есть: за обратным прокси порт и путь свои.
 */
internal fun normalizeAddress(raw: String): String {
    val trimmed = raw.trim().trimEnd('/')
    require(trimmed.isNotEmpty()) { "Введите адрес сервера" }
    if (trimmed.contains("://")) return trimmed
    val host = trimmed.substringBefore('/')
    val path = trimmed.removePrefix(host)
    val hostWithPort = if (host.contains(':')) host else "$host:$DEFAULT_PORT"
    return "http://$hostWithPort$path"
}

private const val DEFAULT_PORT = 8096
