package com.filmax.data.tvlink.android

import com.filmax.data.tvlink.security.LinkCrypto
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class AndroidLinkCrypto : LinkCrypto {

    private val random = SecureRandom()

    override fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance(SHA_256).digest(data)

    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance(HMAC_SHA_256).run {
            init(SecretKeySpec(key, HMAC_SHA_256))
            doFinal(data)
        }

    override fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    private companion object {
        const val SHA_256 = "SHA-256"
        const val HMAC_SHA_256 = "HmacSHA256"
    }
}
