package com.filmax.core.ui.components

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Режим окна держится, пока его держит хотя бы один экран: следующая серия — новый плеер входит
 * в композицию раньше, чем уходит старый, и уход старого альбомную не снимает.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WindowModeHoldTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `next episode keeps landscape while the previous player fades out`() {
        var players by mutableStateOf(listOf(FIRST_EPISODE))
        compose.setContent {
            players.forEach { episode -> key(episode) { LockLandscape() } }
        }
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, orientation())

        // Кроссфейд перехода: оба плеера в композиции, затем старый уходит.
        players = listOf(FIRST_EPISODE, NEXT_EPISODE)
        compose.waitForIdle()
        players = listOf(NEXT_EPISODE)
        compose.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, orientation())

        players = emptyList()
        compose.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, orientation())
    }

    @Test
    fun `modes are counted apart`() {
        val holds = WindowModeHolds()

        assertTrue(holds.take(WindowMode.Landscape))
        assertTrue(holds.take(WindowMode.HiddenSystemBars))
        assertFalse(holds.take(WindowMode.Landscape))

        assertFalse(holds.release(WindowMode.Landscape))
        assertTrue(holds.release(WindowMode.HiddenSystemBars))
        assertTrue(holds.release(WindowMode.Landscape))
    }

    private fun orientation(): Int = compose.activity.requestedOrientation

    private companion object {
        const val FIRST_EPISODE = 1
        const val NEXT_EPISODE = 2
    }
}
