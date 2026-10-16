package com.filmax.feature.profile.common

import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.data.fake.FakeAuthRepository
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakePlaybackSettingsRepository
import com.filmax.data.fake.FakeThemeSettingsRepository
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Профиль на фейковом бэкенде: панель вариантов открывается и закрывается, выбранный вариант
 * сохраняется, палитра сохраняется в настройках оформления, выход — только после подтверждения.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend(latency = FakeLatency.None)
    private val playback = FakePlaybackSettingsRepository()
    private val theme = FakeThemeSettingsRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `profile loads with subscription and current palette`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        val state = model.states.value
        assertFalse(state.loading)
        assertEquals(true, state.profile?.subscription?.active)
        assertEquals(ThemePalette.VIOLET, state.palette)
        assertEquals("Фиолет", state.valueOf(ProfileSetting.PALETTE))
    }

    @Test
    fun `option panel opens for a setting and the choice is saved`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        model.dispatch(ProfileEvent.OpenSetting(ProfileSetting.QUALITY))
        advanceUntilIdle()
        assertTrue(model.states.value.settingOpen)
        assertEquals(ProfileSetting.QUALITY, model.states.value.setting)

        model.dispatch(ProfileEvent.ChooseOption(optionsOf(ProfileSetting.QUALITY).indexOf("1080p")))
        advanceUntilIdle()

        assertFalse(model.states.value.settingOpen, "панель не закрылась после выбора")
        assertEquals("1080p", model.states.value.playback.quality)
        assertEquals("1080p", playback.settings.first().quality)
    }

    @Test
    fun `plain focus switch is kept in theme settings and shown in the state`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        assertFalse(model.states.value.plainFocus)

        model.dispatch(ProfileEvent.SetPlainFocus(true))
        advanceUntilIdle()

        assertTrue(theme.plainFocus.value)
        assertTrue(model.states.value.plainFocus)
    }

    @Test
    fun `palette choice is kept in theme settings`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        model.dispatch(ProfileEvent.OpenSetting(ProfileSetting.PALETTE))
        model.dispatch(ProfileEvent.ChooseOption(ThemePalette.CORAL.ordinal))
        advanceUntilIdle()

        assertEquals(ThemePalette.CORAL, theme.palette.value)
        assertEquals(ThemePalette.CORAL, model.states.value.palette)
        assertEquals("Коралл", model.states.value.valueOf(ProfileSetting.PALETTE))
    }

    @Test
    fun `subtitles switch turns on the default language and off again`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        assertFalse(model.states.value.subtitlesEnabled)

        model.dispatch(ProfileEvent.SetSubtitlesEnabled(true))
        advanceUntilIdle()
        assertTrue(model.states.value.subtitlesEnabled)
        assertEquals("Русский", playback.settings.first().subtitleLanguage)

        model.dispatch(ProfileEvent.SetSubtitlesEnabled(false))
        advanceUntilIdle()
        assertFalse(model.states.value.subtitlesEnabled)
        assertEquals(PlaybackSettings.SubtitleOff, playback.settings.first().subtitleLanguage)
    }

    @Test
    fun `closing the panel keeps the setting it was about`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()

        model.dispatch(ProfileEvent.OpenSetting(ProfileSetting.AUDIO))
        model.dispatch(ProfileEvent.CloseSetting)
        advanceUntilIdle()

        // Настройка остаётся: по ней панель доезжает своим содержимым и фокус возвращается на строку.
        assertFalse(model.states.value.settingOpen)
        assertEquals(ProfileSetting.AUDIO, model.states.value.setting)
        assertEquals("Оригинал", playback.settings.first().audioLanguage)
    }

    @Test
    fun `logout asks first and dismiss keeps the session`() = runTest(dispatcher) {
        val auth = FakeAuthRepository(backend)
        val model = model(auth)
        advanceUntilIdle()

        model.dispatch(ProfileEvent.AskLogout)
        advanceUntilIdle()
        assertTrue(model.states.value.confirmLogout)

        model.dispatch(ProfileEvent.DismissLogout)
        advanceUntilIdle()
        assertFalse(model.states.value.confirmLogout)
        assertTrue(auth.isAuthenticated.first(), "сессия закрылась без подтверждения")

        model.dispatch(ProfileEvent.AskLogout)
        model.dispatch(ProfileEvent.Logout)
        advanceUntilIdle()
        assertFalse(model.states.value.confirmLogout)
        assertFalse(auth.isAuthenticated.first())
    }

    private fun model(auth: FakeAuthRepository = FakeAuthRepository(backend)) = ProfileScreenModel(
        user = FakeUserRepository(backend),
        watching = FakeWatchingRepository(backend),
        auth = auth,
        favorites = FakeFavoritesRepository(),
        playbackSettings = playback,
        themeSettings = theme,
    )
}
