package com.filmax.data.source

import com.filmax.core.domain.source.ContentSource
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Переключатель источника: запрос — в выбранный источник, поток — следом за выбором. */
class SourceSwitchTest {

    private val settings = MapSettings()
    private val sources = ContentSourceRepositoryImpl(settings)

    private var homeServerCreated = false
    private val kinopubAuth = MutableStateFlow(true)
    private val homeServerAuth = MutableStateFlow(false)

    private val switch = SourceSwitch(
        sources = sources,
        kinopub = lazy { Backend("kino.pub", kinopubAuth) },
        homeServer = lazy {
            homeServerCreated = true
            Backend("server", homeServerAuth)
        },
    )

    @Test
    fun `kinopub is the source until another is chosen`() {
        assertEquals("kino.pub", switch.current.name)
        assertFalse(homeServerCreated, "Невыбранный источник не должен создаваться")
    }

    @Test
    fun `choice is remembered on the device`() = runTest {
        sources.select(ContentSource.JELLYFIN)

        assertEquals(ContentSource.JELLYFIN, ContentSourceRepositoryImpl(settings).source.value)
        assertEquals("server", switch.current.name)
    }

    @Test
    fun `flow follows the chosen source`() = runTest {
        val seen = mutableListOf<Boolean>()
        val collecting = launch { switch.flow { it.signedIn }.take(2).toList(seen) }
        // Подписчик успевает получить значение kino.pub до переключения.
        testScheduler.runCurrent()

        sources.select(ContentSource.JELLYFIN)
        collecting.join()

        assertEquals(listOf(true, false), seen)
        assertEquals(false, switch.flow { it.signedIn }.first())
    }

    private class Backend(val name: String, val signedIn: MutableStateFlow<Boolean>)
}
