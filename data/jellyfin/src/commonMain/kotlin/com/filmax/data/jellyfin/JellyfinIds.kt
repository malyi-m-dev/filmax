package com.filmax.data.jellyfin

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Числовые id для GUID сервера. Домен, навигация и локальные хранилища адресуют тайтлы числом
 * (`Destination.Details(itemId)`, «Буду смотреть», память озвучки), а у Jellyfin id — GUID.
 *
 * Число выводится из GUID, а не выдаётся по порядку: телефон и ТВ, глядящие в один сервер, дают
 * тайтлу одинаковый id, и «Открыть на ТВ» передаёт число, которое ТВ поймёт. Диапазон — от 2³⁰:
 * id kino.pub на порядки меньше, и записи двух источников в общих хранилищах не пересекутся. При
 * совпадении чисел у разных GUID берётся следующее свободное — такие редкие тайтлы на двух
 * устройствах могут получить разные id, но не смешаться.
 *
 * Таблица хранится на устройстве: после перезапуска число из сохранённого экрана или из истории
 * должно снова находить свой GUID. Пишется пачкой ([flush]) после разбора ответа, а не на каждый id.
 */
internal class JellyfinIds(private val settings: Settings) {

    private val table = MutableStateFlow(load())
    private val dirty = MutableStateFlow(false)

    fun intFor(guid: String): Int {
        table.value.byGuid[guid]?.let { return it }
        var assigned = 0
        table.update { current ->
            val known = current.byGuid[guid]
            if (known != null) {
                assigned = known
                current
            } else {
                assigned = current.freeIdFrom(preferredId(guid))
                current.with(assigned, guid)
            }
        }
        dirty.value = true
        return assigned
    }

    fun guidFor(id: Int): String? = table.value.byId[id]

    /** GUID по id; неизвестный id — ошибка с 404: экран покажет «не найдено». */
    fun requireGuid(id: Int): String = guidFor(id) ?: throw NoSuchElementException("Тайтл $id не найден (404)")

    fun flush() {
        if (!dirty.compareAndSet(expect = true, update = false)) return
        settings.putString(KEY, json.encodeToString(serializer, table.value.byId.mapKeys { it.key.toString() }))
    }

    private fun load(): IdTable {
        val stored = settings.getStringOrNull(KEY)
            ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
            .orEmpty()
        val byId = stored.mapNotNull { (id, guid) -> id.toIntOrNull()?.let { it to guid } }.toMap()
        return IdTable(byId = byId, byGuid = byId.entries.associate { (id, guid) -> guid to id })
    }

    private data class IdTable(val byId: Map<Int, String>, val byGuid: Map<String, Int>) {

        fun with(id: Int, guid: String) = IdTable(byId + (id to guid), byGuid + (guid to id))

        fun freeIdFrom(start: Int): Int =
            generateSequence(start) { ID_BASE + ((it - ID_BASE + 1) and ID_MASK) }.first { it !in byId }
    }

    companion object {
        /** Начало диапазона id домашнего сервера: 2³⁰. */
        const val ID_BASE = 1 shl 30
        private const val ID_MASK = ID_BASE - 1
        private const val KEY = "jellyfin_ids"
        private const val HASH_HEX_DIGITS = 8
        private const val HEX = 16
        private val json = Json { ignoreUnknownKeys = true }
        private val serializer = MapSerializer(String.serializer(), String.serializer())

        /** Число из GUID без регистрации — для ключей списков серий, которых не открывают по id. */
        fun preferredId(guid: String): Int {
            val bits = guid.filter { it.isLetterOrDigit() }.take(HASH_HEX_DIGITS).toLongOrNull(HEX)
                ?: guid.hashCode().toLong()
            return ID_BASE + (bits and ID_MASK.toLong()).toInt()
        }
    }
}
