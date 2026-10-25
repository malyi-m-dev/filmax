package com.filmax.data.tvlink

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.tvlink.android.AndroidLinkCrypto
import com.filmax.data.tvlink.platform.LinkService
import com.filmax.data.tvlink.platform.LocalDevice
import com.filmax.data.tvlink.platform.ServiceAdvertiser
import com.filmax.data.tvlink.platform.ServiceBrowser
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Телефон и ТВ на настоящих сокетах через localhost: рукопожатие, команды пульта, снимки плеера,
 * обрывы связи и память о ТВ. Время настоящее — тайм-ауты протокола и паузы повторов идут по часам,
 * а не по виртуальному времени.
 */
class TvLinkLoopbackTest {

    private val crypto = AndroidLinkCrypto()
    private val ownUsers = FakeUserRepository(FakeBackend(latency = FakeLatency.None))

    /** Настройки телефона: переживают «перезапуск» — новый клиент на тех же настройках. */
    private val phoneSettings = MapSettings()

    /** Что телефон видит в mDNS: до ТВ дотягиваются по адресу, пока тест не «объявит» его. */
    private val network = MutableStateFlow<List<LinkService>>(emptyList())

    @Test
    fun `phone of the same account controls the tv`() = linkTest { tv, phone ->
        val received = Channel<RemoteCommand>(Channel.UNLIMITED)
        launch(start = CoroutineStart.UNDISPATCHED) { tv.commands.collect { received.send(it) } }

        assertTrue(phone.connect(tv.device()))
        val play = RemoteCommand.Play(itemId = 2003, videoId = 7, season = 4, startSeconds = 1250)
        phone.send(play)
        phone.send(RemoteCommand.SeekBy(-10_000))

        assertEquals(play, received.receive())
        assertEquals(RemoteCommand.SeekBy(-10_000), received.receive())
        assertEquals(listOf(PHONE_NAME), tv.state.first { it.phones.isNotEmpty() }.phones)
        val connected = assertIs<RemoteSession.Connected>(phone.session.value)
        assertEquals(TV_NAME, connected.device.name, "имя ТВ — из приветствия, а не из введённого адреса")
    }

    @Test
    fun `phone sees what the tv plays`() = linkTest { tv, phone ->
        assertTrue(phone.connect(tv.device()))

        tv.publish(STATUS)
        val playing = phone.session.first { (it as? RemoteSession.Connected)?.playback != null }
        assertEquals(STATUS, (playing as RemoteSession.Connected).playback)

        tv.publish(null)
        phone.session.first { (it as? RemoteSession.Connected)?.playback == null }
    }

    @Test
    fun `phone of another account is turned away`() = linkTest(phoneUsers = AnotherAccount(ownUsers)) { tv, phone ->
        assertFalse(phone.connect(tv.device()))

        assertEquals(LinkFailure.OtherAccount, assertIs<RemoteSession.Lost>(phone.session.value).failure)
        assertTrue(tv.state.value.phones.isEmpty())
        assertNull(phone.paired.value, "ТВ, который не пустил, не запоминается")
    }

    @Test
    fun `nobody at the address means unreachable`() = linkTest { _, phone ->
        val nobody = TvDevice(id = "nobody", name = "nobody", host = "127.0.0.1", port = 1)

        assertFalse(phone.connect(nobody))
        assertEquals(LinkFailure.Unreachable, assertIs<RemoteSession.Lost>(phone.session.value).failure)
    }

    @Test
    fun `closed tv app drops the link`() = linkTest { tv, phone ->
        assertTrue(phone.connect(tv.device()))

        stopTv()

        val lost = phone.session.first { it is RemoteSession.Lost }
        assertEquals(LinkFailure.Dropped, (lost as RemoteSession.Lost).failure)
    }

    @Test
    fun `phone remembers the tv by its own id even when connected by address`() = linkTest { tv, phone ->
        assertTrue(phone.connect(tv.device()))

        val paired = assertNotNull(phone.paired.value)
        assertEquals(TV_NAME, paired.name)
        assertNotEquals(tv.device().id, paired.id, "запоминается постоянный id ТВ, а не введённый адрес")
        assertEquals(paired, TvLinkMemory(phoneSettings, crypto).paired.value, "память переживает перезапуск")
    }

    @Test
    fun `connecting again to the linked tv keeps the link`() = linkTest { tv, phone ->
        assertTrue(phone.connect(tv.device()))
        val seen = mutableListOf<RemoteSession>()
        launch(start = CoroutineStart.UNDISPATCHED) { phone.session.collect { seen += it } }

        assertTrue(phone.connect(assertNotNull(phone.paired.value)))
        assertTrue(phone.connect(tv.device()))

        assertTrue(seen.none { it is RemoteSession.Connecting }, "связь с тем же ТВ не должна рваться: $seen")
    }

    @Test
    fun `remembered tv comes back by itself when filmax on it is reopened`() = linkTest { tv, phone ->
        assertTrue(phone.connect(tv.device()))
        launch { phone.stayConnected() }

        stopTv()
        phone.session.first { it is RemoteSession.Lost }
        startTv()
        // Стандартный порт бывает занят (проброс на эмулятор), и ТВ встаёт на другой — его адрес
        // телефон узнаёт из mDNS, как узнал бы новый адрес от роутера.
        network.value = listOf(tv.advertised(assertNotNull(phone.paired.value).id))

        phone.session.first { it is RemoteSession.Connected }
        assertEquals(listOf(PHONE_NAME), tv.state.first { it.phones.isNotEmpty() }.phones)
    }

    @Test
    fun `remembered tv connects by itself after the phone app restarts`() = linkTest { tv, phone ->
        assertTrue(phone.connect(tv.device()))
        val paired = assertNotNull(phone.paired.value)
        // Телефон «закрыли»: связь порвалась, а в настройках ТВ остался.
        phone.forget()
        TvLinkMemory(phoneSettings, crypto).remember(paired)
        val restarted = client(ownUsers)

        launch { restarted.stayConnected() }

        restarted.session.first { it is RemoteSession.Connected }
        tv.state.first { it.phones.isNotEmpty() }
        restarted.forget()
    }

    @Test
    fun `forgotten tv does not come back`() = linkTest { tv, phone ->
        assertTrue(phone.connect(tv.device()))
        launch { phone.stayConnected() }

        phone.forget()
        tv.state.first { it.phones.isEmpty() }
        delay(QUIET_CHECK_MS)

        assertEquals(RemoteSession.Idle, phone.session.value)
        assertNull(phone.paired.value)
        assertTrue(tv.state.value.phones.isEmpty())
    }

    /** ТВ с поднятым приёмником и телефон; [phoneUsers] — аккаунт, в который вошёл телефон. */
    private fun linkTest(
        phoneUsers: UserRepository = ownUsers,
        block: suspend LinkScope.(tv: TvLinkServer, phone: TvLinkClient) -> Unit,
    ) = runBlocking {
        val tv = TvLinkServer(
            account = TvLinkAccount(ownUsers, crypto),
            advertiser = ServiceAdvertiser { awaitCancellation() },
            device = TestDevice(TV_NAME),
            memory = TvLinkMemory(MapSettings(), crypto),
            dispatcher = Dispatchers.IO,
        )
        val phone = client(phoneUsers)
        val receiver = TvReceiver(tv, this)
        try {
            withTimeout(TEST_TIMEOUT_MS) {
                receiver.start()
                coroutineScope {
                    LinkScope(this, receiver).block(tv, phone)
                    // Подписки теста (сбор команд, повторы подключения) живут до конца сценария.
                    coroutineContext.cancelChildren()
                }
            }
        } finally {
            phone.forget()
            receiver.stop()
        }
    }

    /** Телефон на общих [phoneSettings] и с общей сетью [network]. */
    private fun client(users: UserRepository) = TvLinkClient(
        account = TvLinkAccount(users, crypto),
        device = TestDevice(PHONE_NAME),
        memory = TvLinkMemory(phoneSettings, crypto),
        browser = ServiceBrowser { network },
        dispatcher = Dispatchers.IO,
    )

    /** Приёмник, который можно закрыть и открыть снова, как Filmax на ТВ. Живёт в скоупе всего теста. */
    private class TvReceiver(private val tv: TvLinkServer, private val scope: CoroutineScope) {

        private var serving: Job? = null

        suspend fun start() {
            serving = scope.launch(Dispatchers.IO) { tv.serve() }
            tv.state.first { it.listening }
        }

        suspend fun stop() {
            serving?.cancelAndJoin()
        }
    }

    private class LinkScope(scope: CoroutineScope, private val receiver: TvReceiver) :
        CoroutineScope by scope {
        suspend fun stopTv() = receiver.stop()
        suspend fun startTv() = receiver.start()
    }

    /** Адрес поднятого приёмника — так его ввёл бы человек с экрана ТВ. */
    private fun TvLinkServer.device(): TvDevice = assertNotNull(state.value.address?.let(TvAddress::parse))

    /** Запись mDNS поднятого приёмника с постоянным id [tvId]. */
    private fun TvLinkServer.advertised(tvId: String): LinkService =
        device().let { LinkService(id = tvId, name = TV_NAME, host = it.host, port = it.port, fingerprint = null) }

    private class TestDevice(override val name: String) : LocalDevice {
        override fun localAddress() = "127.0.0.1"
    }

    /** Тот же «сервер», но другой человек: другое имя и дата регистрации. */
    private class AnotherAccount(private val base: UserRepository) : UserRepository by base {
        override suspend fun getProfile(): RequestResult<UserProfile> = when (val profile = base.getProfile()) {
            is RequestResult.Success -> RequestResult.Success(
                profile.data.copy(username = "Сосед", registeredAt = 1_600_000_000_000L),
            )
            is RequestResult.Error -> profile
        }
    }

    private companion object {
        const val TV_NAME = "Гостиная"
        const val PHONE_NAME = "Galaxy S25"
        const val TEST_TIMEOUT_MS = 20_000L

        /** Дольше первой паузы повтора: забытый ТВ за это время успел бы подключиться снова. */
        const val QUIET_CHECK_MS = 3_000L

        val STATUS = PlaybackStatus(
            itemId = 2003,
            title = "Хроники Пограничья",
            posterUrl = null,
            season = 4,
            episode = 7,
            episodeTitle = null,
            playing = true,
            buffering = false,
            positionMs = 60_000,
            durationMs = 2_700_000,
            hasNext = true,
            hasPrevious = true,
        )
    }
}
