package com.filmax.feature.profile.common

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel

/**
 * Профиль: аккаунт, настройки воспроизведения, палитра ТВ, источник контента и выход. Общий для телефона и ТВ;
 * панель вариантов и диалог выхода — ТВ-сценарий (на телефоне свои листы), но их состояние
 * тоже здесь: экран только рисует и отдаёт события.
 */
// По короткому обработчику на событие: настройки, панель вариантов, выход.
@Suppress("TooManyFunctions")
class ProfileScreenModel(
    private val user: UserRepository,
    private val watching: WatchingRepository,
    private val auth: AuthRepository,
    private val favorites: FavoritesRepository,
    settings: ProfileSettings,
) : BaseScreenModel<ProfileState, ProfileSideEffect, ProfileEvent>(ProfileState()) {

    private val playbackSettings = settings.playback
    private val themeSettings = settings.theme
    private val sources = settings.sources

    init {
        onFetchData()
        observeFavorites()
        observePlaybackSettings()
        observeTheme()
        observeSource()
    }

    private fun observeSource() {
        screenModelScope {
            sources.source.collect { source -> updateState { it.copy(source = source) } }
        }
    }

    private fun observeFavorites() {
        screenModelScope {
            favorites.favorites.collect { items ->
                updateState { it.copy(favoritesCount = items.size) }
            }
        }
    }

    private fun observePlaybackSettings() {
        screenModelScope {
            playbackSettings.settings.collect { settings ->
                updateState { it.copy(playback = settings) }
            }
        }
    }

    /** Оформление — живыми потоками: его меняют здесь же, а значение должно сразу дойти до строк. */
    private fun observeTheme() {
        screenModelScope {
            themeSettings.palette.collect { palette -> updateState { current -> current.copy(palette = palette) } }
        }
        screenModelScope {
            themeSettings.plainFocus.collect { plain -> updateState { current -> current.copy(plainFocus = plain) } }
        }
    }

    override fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Logout -> logout()
            is ProfileEvent.SetQuality -> screenModelScope { playbackSettings.setQuality(event.quality) }
            is ProfileEvent.SetAudioLanguage ->
                screenModelScope { playbackSettings.setAudioLanguage(event.language) }
            is ProfileEvent.SetSubtitleLanguage ->
                screenModelScope { playbackSettings.setSubtitleLanguage(event.language) }
            is ProfileEvent.SetSubtitlesEnabled ->
                screenModelScope { playbackSettings.setSubtitleLanguage(subtitleLanguageFor(event.enabled)) }
            is ProfileEvent.SetPlainFocus -> screenModelScope { themeSettings.setPlainFocus(event.enabled) }
            is ProfileEvent.OpenSetting -> update { it.copy(setting = event.setting, settingOpen = true) }
            ProfileEvent.CloseSetting -> update { it.copy(settingOpen = false) }
            is ProfileEvent.ChooseOption -> chooseOption(event.index)
            ProfileEvent.AskLogout -> update { it.copy(confirmLogout = true) }
            ProfileEvent.DismissLogout -> update { it.copy(confirmLogout = false) }
        }
    }

    override fun onFetchData() {
        screenModelScope {
            // Профиль — основной запрос: от него зависит отрисовка экрана.
            when (val result = user.getProfile()) {
                is RequestResult.Success ->
                    updateState { it.copy(loading = false, profile = result.data) }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    return@screenModelScope
                }
            }

            // Статистика — best-effort: ошибки не блокируют экран, поля остаются по умолчанию.
            (watching.getHistory() as? RequestResult.Success)?.let { history ->
                updateState { it.copy(watchedCount = history.data.size) }
            }
            // device/info не запрашиваем: бэкенд отвечает 500, а блок «Устройство» с экранов
            // временно убран. Вернуть вместе с блоком, когда бэкенд починят.
        }
    }

    /**
     * Применяет вариант открытой настройки и закрывает панель. Настройка читается при выполнении,
     * а не при отправке: «открыть» стоит в той же очереди раньше.
     */
    private fun chooseOption(index: Int) {
        screenModelScope {
            val current = state
            val option = optionsOf(current.setting).getOrNull(index) ?: return@screenModelScope
            updateState { it.copy(settingOpen = false) }
            when (current.setting) {
                ProfileSetting.QUALITY -> playbackSettings.setQuality(option)
                ProfileSetting.AUDIO -> playbackSettings.setAudioLanguage(option)
                ProfileSetting.SUBTITLES -> playbackSettings.setSubtitleLanguage(option)
                ProfileSetting.PALETTE -> themeSettings.setPalette(ThemePalette.entries[index])
                // Граф сам вернётся к корню нового источника (или ко входу, если на него не входили).
                ProfileSetting.SOURCE -> sources.select(ContentSource.entries[index])
            }
        }
    }

    private fun logout() {
        screenModelScope {
            updateState { it.copy(confirmLogout = false) }
            auth.logout()
            postSideEffect(ProfileSideEffect.LoggedOut)
        }
    }

    private fun update(transform: (ProfileState) -> ProfileState) {
        screenModelScope { updateState(transform) }
    }
}
