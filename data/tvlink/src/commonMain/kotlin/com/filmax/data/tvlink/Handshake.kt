package com.filmax.data.tvlink

import com.filmax.data.tvlink.protocol.TvLinkMessage
import com.filmax.data.tvlink.protocol.TvLinkProtocol
import com.filmax.data.tvlink.security.LinkRole
import com.filmax.data.tvlink.security.TvLinkKey

/*
 * Рукопожатие — взаимная проверка аккаунта, три сообщения:
 *   ТВ → телефон   hello(nonceТВ)
 *   телефон → ТВ   auth(подпись nonceТВ, nonceТелефона)
 *   ТВ → телефон   welcome(подпись nonceТелефона)  |  denied
 * Подписывает ключ аккаунта (см. TvLinkKey). ТВ пускает только телефон своего аккаунта, а телефон
 * не шлёт команды ТВ, который лишь повторил чужой отпечаток из mDNS.
 */

/** Чем кончилось рукопожатие для телефона. */
internal sealed interface PhoneHandshake {

    /** [tvName] — имя из приветствия ТВ: у ТВ, введённого адресом, другого имени нет. */
    data class Accepted(val tvName: String) : PhoneHandshake

    /** ТВ из другого аккаунта: он отказал или не смог подписать число телефона. */
    data object OtherAccount : PhoneHandshake

    /** На том конце не Filmax или он не ответил вовремя. */
    data object NoAnswer : PhoneHandshake
}

internal suspend fun LinkConnection.handshakeAsPhone(key: TvLinkKey, phoneName: String): PhoneHandshake {
    val hello = receive(TvLinkProtocol.HANDSHAKE_TIMEOUT_MS) as? TvLinkMessage.Hello ?: return PhoneHandshake.NoAnswer
    val phoneNonce = key.nonce()
    send(TvLinkMessage.Auth(name = phoneName, proof = key.proof(LinkRole.Phone, hello.nonce), nonce = phoneNonce))
    return when (val reply = receive(TvLinkProtocol.HANDSHAKE_TIMEOUT_MS)) {
        is TvLinkMessage.Welcome -> if (key.accepts(LinkRole.Tv, phoneNonce, reply.proof)) {
            PhoneHandshake.Accepted(tvName = hello.name)
        } else {
            PhoneHandshake.OtherAccount
        }
        TvLinkMessage.Denied -> PhoneHandshake.OtherAccount
        else -> PhoneHandshake.NoAnswer
    }
}

/** Имя телефона, если он из того же аккаунта; null — не пустили (чужой аккаунт или не ответил). */
internal suspend fun LinkConnection.handshakeAsTv(key: TvLinkKey, tvName: String): String? {
    val tvNonce = key.nonce()
    send(TvLinkMessage.Hello(version = TvLinkProtocol.VERSION, name = tvName, nonce = tvNonce))
    val auth = receive(TvLinkProtocol.HANDSHAKE_TIMEOUT_MS) as? TvLinkMessage.Auth ?: return null
    val accepted = key.accepts(LinkRole.Phone, tvNonce, auth.proof)
    send(if (accepted) TvLinkMessage.Welcome(proof = key.proof(LinkRole.Tv, auth.nonce)) else TvLinkMessage.Denied)
    return auth.name.takeIf { accepted }
}
