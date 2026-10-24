package com.filmax.data.tvlink.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.filmax.data.tvlink.platform.LinkAdvert
import com.filmax.data.tvlink.platform.LinkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.assertEquals

/**
 * Настоящий mDNS устройства: объявленный ТВ находится поиском — с отпечатком аккаунта и портом из
 * TXT-записи. Устройство поиска и объявления одно, а сеть нужна с multicast (Wi-Fi): на ТВ-эмуляторе
 * с одним eth0 системный mDNS сервис не объявляет.
 */
@RunWith(AndroidJUnit4::class)
class NsdRoundTripTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun advertisedTvIsFoundWithFingerprintAndPort() = runBlocking {
        val advert = LinkAdvert(name = "Filmax test ${Random.nextInt(BOUND)}", port = PORT, fingerprint = FINGERPRINT)
        val advertising = launch(Dispatchers.Default) { NsdAdvertiser(context).advertise(advert) }
        try {
            val found = discover(advert.name)

            assertEquals(FINGERPRINT, found.fingerprint)
            assertEquals(PORT, found.port)
        } finally {
            advertising.cancelAndJoin()
        }
    }

    /** Свернули Filmax на ТВ и тут же вернулись: объявление снято и поставлено снова — ТВ виден. */
    @Test
    fun tvBackFromBackgroundIsFoundAgain() = runBlocking {
        val advert = LinkAdvert(name = "Filmax test ${Random.nextInt(BOUND)}", port = PORT, fingerprint = FINGERPRINT)
        launch(Dispatchers.Default) { NsdAdvertiser(context).advertise(advert) }.cancelAndJoin()
        val advertising = launch(Dispatchers.Default) { NsdAdvertiser(context).advertise(advert) }
        try {
            assertEquals(FINGERPRINT, discover(advert.name).fingerprint)
        } finally {
            advertising.cancelAndJoin()
        }
    }

    private suspend fun discover(name: String): LinkService = withTimeout(DISCOVERY_TIMEOUT_MS) {
        NsdBrowser(context).browse()
            .map { services -> services.firstOrNull { it.name == name } }
            .filterNotNull()
            .first()
    }

    private companion object {
        const val PORT = 47199
        const val FINGERPRINT = "0123456789abcdef"
        const val BOUND = 100_000
        const val DISCOVERY_TIMEOUT_MS = 20_000L
    }
}
