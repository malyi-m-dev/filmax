package com.filmax.data.tvlink.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Сообщение протокола — одна строка JSON с полем `type`. У полей есть значения по умолчанию: телефон
 * и ТВ обновляются порознь, и сообщение, где новая версия добавила поле, старая должна разобрать.
 */
@Serializable
internal sealed interface TvLinkMessage {

    // ── Рукопожатие ─────────────────────────────────────────────────────────

    /**
     * ТВ → телефон сразу после подключения: кто он и число, которое телефон должен подписать. [id] —
     * постоянный id ТВ: по нему телефон узнаёт запомненный ТВ, даже если подключался по адресу.
     * Пустой — ТВ на Filmax до 2.6.
     */
    @Serializable
    @SerialName("hello")
    data class Hello(
        val version: Int = 0,
        val name: String = "",
        val nonce: String = "",
        val id: String = "",
    ) : TvLinkMessage

    /** Телефон → ТВ: подпись числа ТВ и своё число — ТВ тоже докажет, что он из того же аккаунта. */
    @Serializable
    @SerialName("auth")
    data class Auth(val name: String = "", val proof: String = "", val nonce: String = "") : TvLinkMessage

    /** ТВ → телефон: подпись принята, вот подпись числа телефона. */
    @Serializable
    @SerialName("welcome")
    data class Welcome(val proof: String = "") : TvLinkMessage

    /** ТВ → телефон: подпись не сошлась — телефон из другого аккаунта. */
    @Serializable
    @SerialName("denied")
    data object Denied : TvLinkMessage

    // ── Пульт → ТВ ──────────────────────────────────────────────────────────

    @Serializable
    @SerialName("play")
    data class Play(
        val itemId: Int = 0,
        val videoId: Int = -1,
        val season: Int = -1,
        val startSeconds: Int = -1,
    ) : TvLinkMessage

    @Serializable
    @SerialName("pause")
    data object Pause : TvLinkMessage

    @Serializable
    @SerialName("resume")
    data object Resume : TvLinkMessage

    @Serializable
    @SerialName("seek_by")
    data class SeekBy(val deltaMs: Long = 0) : TvLinkMessage

    @Serializable
    @SerialName("seek_to")
    data class SeekTo(val positionMs: Long = 0) : TvLinkMessage

    @Serializable
    @SerialName("next")
    data object Next : TvLinkMessage

    @Serializable
    @SerialName("previous")
    data object Previous : TvLinkMessage

    @Serializable
    @SerialName("stop")
    data object Stop : TvLinkMessage

    /** «Я на связи» — когда команд нет. По тишине дольше тайм-аута связь считается потерянной. */
    @Serializable
    @SerialName("ping")
    data object Ping : TvLinkMessage

    // ── ТВ → пульт ──────────────────────────────────────────────────────────

    /** Снимок плеера: на каждое изменение и повтором вместо ping, пока ничего не меняется. */
    @Serializable
    @SerialName("status")
    data class Status(
        val itemId: Int = 0,
        val title: String = "",
        val posterUrl: String? = null,
        val season: Int = -1,
        val episode: Int = -1,
        val episodeTitle: String? = null,
        val playing: Boolean = false,
        val buffering: Boolean = false,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
    ) : TvLinkMessage

    /** Плеер на ТВ закрыт. */
    @Serializable
    @SerialName("idle")
    data object Idle : TvLinkMessage
}
