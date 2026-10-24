package com.filmax.data.tvlink.security

import com.filmax.core.domain.user.model.UserProfile
import com.filmax.data.tvlink.android.AndroidLinkCrypto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Ключ аккаунта на настоящей криптографии Android (javax.crypto): тест живёт в androidUnitTest, потому
 * что общий код своей реализации хэшей не имеет.
 */
@OptIn(ExperimentalStdlibApi::class)
class TvLinkKeyTest {

    private val crypto = AndroidLinkCrypto()
    private val owner = UserProfile(
        id = 0,
        username = "zritel",
        email = "zritel@example.com",
        avatarUrl = null,
        subscription = null,
        registeredAt = 1_577_836_800_000L,
    )

    @Test
    fun `hmac matches RFC 4231 test case 2`() {
        val mac = crypto.hmacSha256("Jefe".encodeToByteArray(), "what do ya want for nothing?".encodeToByteArray())

        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", mac.toHexString())
    }

    @Test
    fun `phone and tv of one account derive the same key`() {
        val phone = TvLinkKey.of(owner, crypto)
        val tv = TvLinkKey.of(owner.copy(avatarUrl = "https://example.com/a.png"), crypto)

        assertEquals(phone.fingerprint, tv.fingerprint)
        assertTrue(tv.accepts(LinkRole.Phone, "nonce", phone.proof(LinkRole.Phone, "nonce")))
    }

    @Test
    fun `same name with another registration date is another account`() {
        val impostor = TvLinkKey.of(owner.copy(registeredAt = 1_600_000_000_000L), crypto)
        val tv = TvLinkKey.of(owner, crypto)

        assertNotEquals(tv.fingerprint, impostor.fingerprint)
        assertFalse(tv.accepts(LinkRole.Phone, "nonce", impostor.proof(LinkRole.Phone, "nonce")))
    }

    @Test
    fun `phone proof is not accepted as tv proof and nonces differ`() {
        val key = TvLinkKey.of(owner, crypto)

        assertFalse(key.accepts(LinkRole.Tv, "nonce", key.proof(LinkRole.Phone, "nonce")))
        assertNotEquals(key.nonce(), key.nonce())
    }

    @Test
    fun `fingerprint does not reveal the key`() {
        val key = TvLinkKey.of(owner, crypto)

        assertEquals(16, key.fingerprint.length)
        assertFalse(key.accepts(LinkRole.Phone, "nonce", key.fingerprint))
    }
}
