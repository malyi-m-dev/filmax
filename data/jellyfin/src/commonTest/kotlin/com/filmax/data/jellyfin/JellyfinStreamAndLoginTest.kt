package com.filmax.data.jellyfin

import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.source.HomeServer
import com.filmax.data.jellyfin.JellyfinFixtures.MOVIE
import com.filmax.data.jellyfin.JellyfinFixtures.MOVIE_ID
import com.filmax.data.jellyfin.JellyfinFixtures.playbackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Потоки под запуск и вход на сервер. */
class JellyfinStreamAndLoginTest {

    private val address = FakeJellyfinServer.ADDRESS

    @Test
    fun `playable file goes first with the server transcode as its backup`() = runTest {
        val track = playable(directPlay = true)

        val original = track?.files?.first()
        assertEquals("1080p", original?.quality)
        val direct = "$address/Videos/$MOVIE_ID/stream?static=true&mediaSourceId=ms1&ApiKey=secret-token"
        assertEquals(direct, original?.http)
        assertEquals("$address/videos/$MOVIE_ID/master.m3u8?MediaSourceId=ms1&ApiKey=secret-token", original?.hls)
        assertEquals(listOf("1080p", "720p", "480p"), track?.files?.map { it.quality })
        assertEquals(listOf("rus"), track?.subtitles?.map { it.lang })
        assertEquals("$address/Videos/$MOVIE_ID/ms1/Subtitles/3/0/Stream.vtt", track?.subtitles?.single()?.url)
        assertEquals("Дубляж", track?.audios?.single()?.voiceAuthor)
    }

    @Test
    fun `file the player cannot handle is never offered directly`() = runTest {
        val original = playable(directPlay = false)?.files?.first()

        assertNull(original?.http)
        assertTrue(original?.hls.orEmpty().contains("master.m3u8"))
    }

    @Test
    fun `password login saves the session and signs further requests`() = runTest {
        val server = FakeJellyfinServer(
            mapOf(
                "/Users/AuthenticateByName" to """{"User":{"Id":"u1","Name":"anna"},"AccessToken":"fresh"}""",
                "/Users/Me" to """{"Id":"u1","Name":"anna"}""",
            ),
        )
        val login = JellyfinServerRepository(server.api, server.session, JellyfinDiscovery(Dispatchers.Default))

        login.login(HomeServer(address = address, name = "Дом"), "anna", "pw")
        JellyfinUserRepository(server.context).getProfile()

        val authorization = server.requestsTo("/Users/AuthenticateByName").single().authorization.orEmpty()
        assertTrue(authorization.startsWith("MediaBrowser Client=\"Filmax\""), authorization)
        assertTrue(authorization.contains("DeviceId=\"${server.session.deviceId}\""))
        assertTrue("Token=" !in authorization)
        assertTrue(server.requestsTo("/Users/Me").single().authorization.orEmpty().contains("Token=\"fresh\""))
        assertEquals("Дом", server.session.current?.serverName)
    }

    @Test
    fun `quick connect waits for approval and then signs in`() = runTest {
        val server = FakeJellyfinServer(
            mapOf(
                "/QuickConnect/Initiate" to """{"Secret":"s3","Code":"123456","Authenticated":false}""",
                "/QuickConnect/Connect" to """{"Secret":"s3","Authenticated":true}""",
                "/Users/AuthenticateWithQuickConnect" to """{"User":{"Id":"u1","Name":"anna"},"AccessToken":"qc"}""",
            ),
        )
        val login = JellyfinServerRepository(server.api, server.session, JellyfinDiscovery(Dispatchers.Default))
        val home = HomeServer(address = address, name = "Дом", quickConnect = true)

        val code = login.startQuickConnect(home).getOrNull()
        val approved = code?.let { login.checkQuickConnect(home, it).getOrNull() }

        assertEquals("123456", code?.code)
        assertEquals(true, approved)
        assertEquals("qc", server.session.current?.token)
    }

    private suspend fun playable(directPlay: Boolean) = FakeJellyfinServer(
        mapOf("/Items/$MOVIE_ID" to MOVIE, "/Items/$MOVIE_ID/PlaybackInfo" to playbackInfo(directPlay)),
    ).run {
        signIn()
        val videos = JellyfinVideos(api, ids)
        val movieId = ids.intFor(MOVIE_ID)
        val track = JellyfinCatalogRepository(context, videos).getItemDetails(movieId).getOrNull()?.tracklist?.single()
        track?.let { JellyfinStreamRepository(context, videos, tv = true).playable(movieId, it).getOrNull() }
    }
}
