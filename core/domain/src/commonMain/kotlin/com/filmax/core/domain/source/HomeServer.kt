package com.filmax.core.domain.source

import com.filmax.core.domain.common.RequestResult

/**
 * Домашний медиасервер, к которому можно войти. [address] — нормализованный базовый адрес со
 * схемой и портом («http://192.168.1.20:8096»): его и сохраняют, и показывают.
 */
data class HomeServer(
    val address: String,
    val name: String,
    val version: String? = null,
    /** Сервер принимает вход по коду Quick Connect — на ТВ это вход без набора пароля пультом. */
    val quickConnect: Boolean = false,
)

/** Код Quick Connect: [code] показывают человеку, по [secret] устройство ждёт подтверждения. */
data class QuickConnectCode(val code: String, val secret: String)

/**
 * Вход на домашний сервер. Отдельно от [com.filmax.core.domain.auth.AuthRepository]: у kino.pub
 * вход — код устройства, у сервера — адрес в сети и учётная запись на нём, и общий контракт
 * пришлось бы растягивать под оба. Выход и признак входа — общие, через AuthRepository.
 */
interface HomeServerRepository {

    /** Серверы, ответившие в локальной сети. Пусто — не нашлось (или роутер режет широковещание). */
    suspend fun discover(): List<HomeServer>

    /** Проверяет адрес: отвечает ли по нему сервер и как он называется. */
    suspend fun connect(address: String): RequestResult<HomeServer>

    suspend fun login(server: HomeServer, username: String, password: String): RequestResult<Unit>

    suspend fun startQuickConnect(server: HomeServer): RequestResult<QuickConnectCode>

    /**
     * Одна проверка кода: true — человек подтвердил вход, и сессия уже сохранена; false — ещё
     * ждём. Ошибка — код истёк или сервер недоступен.
     */
    suspend fun checkQuickConnect(server: HomeServer, code: QuickConnectCode): RequestResult<Boolean>
}
