package com.filmax.data.tvlink.security

import com.filmax.core.domain.user.model.UserProfile

/** Чья подпись: роли различаются, чтобы подпись телефона нельзя было вернуть ему же как подпись ТВ. */
internal enum class LinkRole(val label: String) {
    Phone("phone"),
    Tv("tv"),
}

/**
 * Ключ аккаунта для связи телефона с ТВ. Выводится из того, что знает только вошедший в аккаунт: имени,
 * почты и даты регистрации из `api/v1/user`. Сам ключ по сети не ходит: ТВ объявляет в mDNS лишь
 * [fingerprint] (по нему телефон отбирает свои ТВ), а в рукопожатии стороны подписывают случайные
 * числа друг друга — подслушанный обмен не повторить, а отпечаток не даёт подделать подпись.
 */
@OptIn(ExperimentalStdlibApi::class)
internal class TvLinkKey private constructor(
    private val secret: ByteArray,
    private val crypto: LinkCrypto,
) {

    val fingerprint: String = crypto.sha256(secret + FINGERPRINT_LABEL.encodeToByteArray())
        .copyOf(FINGERPRINT_BYTES)
        .toHexString()

    fun proof(role: LinkRole, nonce: String): String =
        crypto.hmacSha256(secret, "${role.label}|$nonce".encodeToByteArray()).toHexString()

    fun accepts(role: LinkRole, nonce: String, proof: String): Boolean = proof(role, nonce).sameAs(proof)

    /** Число для рукопожатия. */
    fun nonce(): String = crypto.randomBytes(NONCE_BYTES).toHexString()

    companion object {

        fun of(profile: UserProfile, crypto: LinkCrypto): TvLinkKey {
            val material = listOf(
                KEY_LABEL,
                profile.username,
                profile.email.orEmpty(),
                profile.registeredAt?.toString().orEmpty(),
            ).joinToString(separator = "\n")
            return TvLinkKey(crypto.sha256(material.encodeToByteArray()), crypto)
        }

        /** Метки домена: ключ и отпечаток из одного аккаунта не совпадают ни с чем, кроме себя. */
        private const val KEY_LABEL = "filmax-tvlink-v1"
        private const val FINGERPRINT_LABEL = "fingerprint"

        /** 8 байт отпечатка хватает, чтобы два аккаунта в одной квартире не спутались. */
        private const val FINGERPRINT_BYTES = 8
        private const val NONCE_BYTES = 16
    }
}

/** Сравнение за одно время при любом расхождении: по скорости отказа подпись не подобрать. */
private fun String.sameAs(other: String): Boolean {
    if (length != other.length) return false
    val difference = indices.fold(0) { acc, index -> acc or (this[index].code xor other[index].code) }
    return difference == 0
}
