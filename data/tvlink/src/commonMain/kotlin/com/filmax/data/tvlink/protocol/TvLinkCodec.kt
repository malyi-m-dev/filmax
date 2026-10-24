package com.filmax.data.tvlink.protocol

import com.filmax.data.tvlink.linkTrail
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Сообщение ↔ строка протокола. */
internal object TvLinkCodec {

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(message: TvLinkMessage): String = json.encodeToString(TvLinkMessage.serializer(), message)

    /**
     * null — строка не разбирается: мусор или тип сообщения, которого эта версия ещё не знает
     * (телефон обновился раньше ТВ). Такую строку пропускают, а не рвут связь.
     */
    fun decode(line: String): TvLinkMessage? = try {
        json.decodeFromString(TvLinkMessage.serializer(), line)
    } catch (error: SerializationException) {
        linkTrail("пропущено сообщение: ${error.message}")
        null
    }
}
