package com.filmax.feature.profile.common

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.domain.settings.ThemeSettingsRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.BaseScreenModel

/**
 * Профиль: аккаунт, настройки воспроизведения, палитра и выход. Общий для телефона и ТВ;
 * шторка вариантов и диалог выхода на обеих платформах — состояние здесь: экран только рисует
 * и отдаёт события. Переходы (оценка, пульт, дизайн-система, онбординг после выхода) — тоже здесь.
 */
class ProfileScreenModel(
    private val user: UserRepository,
    private val watching: WatchingRepository,
    private val auth: AuthRepository,
    private val favorites: FavoritesRepository,
    private val playbackSettings: PlaybackSettingsRepository,
    private val themeSettings: ThemeSettingsRepository,
    private val navigator: Navigator,
) : BaseScreenModel<ProfileState, ProfileSideEffect, ProfileEvent>(ProfileState()) {

    init {
        onFetchData()
        observeFavorites()
        observePlaybackSettings()
        observeTheme()
    }

    override fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Logout -> onLogout()
            is ProfileEvent.SetQuality -> onSetQuality(event.quality)
            is ProfileEvent.SetAudioLanguage -> onSetAudioLanguage(event.language)
            is ProfileEvent.SetSubtitleLanguage -> onSetSubtitleLanguage(event.language)
            is ProfileEvent.SetSubtitlesEnabled -> onSetSubtitleLanguage(subtitleLanguageFor(event.enabled))
            is ProfileEvent.SetPlainFocus -> onSetPlainFocus(event.enabled)
            is ProfileEvent.OpenSetting -> onSettingSheet(event.setting, open = true)
            ProfileEvent.CloseSetting -> onCloseSetting()
            is ProfileEvent.ChooseOption -> onChooseOption(event.index)
            ProfileEvent.AskLogout -> onLogoutDialog(open = true)
            ProfileEvent.DismissLogout -> onLogoutDialog(open = false)
            ProfileEvent.OpenRate -> navigator.open(Destination.Feedback(FeedbackTrigger.Profile.name))
            ProfileEvent.OpenRemote -> navigator.open(Destination.Remote)
            ProfileEvent.OpenDesignSystem -> navigator.open(Destination.DesignSystem)
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            // Профиль — основной запрос: от него зависит отрисовка экрана.
            when (val result = user.getProfile()) {
                is RequestResult.Success -> updateState { it.withProfile(result.data) }
                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    return@screenModelScope
                }
            }
            // Статистика — best-effort: ошибки не блокируют экран, поля остаются по умолчанию.
            val history = watching.getHistory() as? RequestResult.Success ?: return@screenModelScope
            val watchedCount = history.data.size
            updateState { it.copy(watchedCount = watchedCount) }
            // device/info не запрашиваем: бэкенд отвечает 500, а блок «Устройство» с экранов
            // временно убран. Вернуть вместе с блоком, когда бэкенд починят.
        }
    }

    private fun observeFavorites() {
        screenModelScope { _ ->
            favorites.favorites.collect { items -> updateState { it.copy(favoritesCount = items.size) } }
        }
    }

    private fun observePlaybackSettings() {
        screenModelScope { _ ->
            playbackSettings.settings.collect { settings -> updateState { it.withPlayback(settings) } }
        }
    }

    /** Оформление — живыми потоками: его меняют здесь же, а значение должно сразу дойти до строк. */
    private fun observeTheme() {
        screenModelScope { _ ->
            themeSettings.palette.collect { palette -> updateState { it.withPalette(palette) } }
        }
        screenModelScope { _ ->
            themeSettings.plainFocus.collect { plain -> updateState { it.copy(plainFocus = plain) } }
        }
    }

    private fun onSetQuality(quality: String) {
        screenModelScope { _ -> playbackSettings.setQuality(quality) }
    }

    private fun onSetAudioLanguage(language: String) {
        screenModelScope { _ -> playbackSettings.setAudioLanguage(language) }
    }

    private fun onSetSubtitleLanguage(language: String) {
        screenModelScope { _ -> playbackSettings.setSubtitleLanguage(language) }
    }

    private fun onSetPlainFocus(enabled: Boolean) {
        screenModelScope { _ -> themeSettings.setPlainFocus(enabled) }
    }

    private fun onSettingSheet(setting: ProfileSetting, open: Boolean) {
        screenModelScope { _ -> updateState { it.withSetting(setting, open) } }
    }

    private fun onCloseSetting() {
        screenModelScope { current -> updateState { it.withSetting(current.setting, open = false) } }
    }

    /**
     * Применяет вариант открытой настройки и закрывает шторку. Настройка читается при выполнении,
     * а не при отправке: «открыть» стоит в той же очереди раньше.
     */
    private fun onChooseOption(index: Int) {
        screenModelScope { current ->
            val option = current.options.getOrNull(index) ?: return@screenModelScope
            updateState { it.withSetting(current.setting, open = false) }
            when (current.setting) {
                ProfileSetting.QUALITY -> playbackSettings.setQuality(option)
                ProfileSetting.AUDIO -> playbackSettings.setAudioLanguage(option)
                ProfileSetting.SUBTITLES -> playbackSettings.setSubtitleLanguage(option)
                ProfileSetting.PALETTE -> themeSettings.setPalette(ThemePalette.entries[index])
            }
        }
    }

    private fun onLogoutDialog(open: Boolean) {
        screenModelScope { _ -> updateState { it.copy(confirmLogout = open) } }
    }

    /** Вход становится единственным корнем: «Назад» не возвращает в аккаунт, из которого вышли. */
    private fun onLogout() {
        screenModelScope { _ ->
            updateState { it.copy(confirmLogout = false) }
            auth.logout()
            navigator.root(Destination.Onboarding)
        }
    }
}
