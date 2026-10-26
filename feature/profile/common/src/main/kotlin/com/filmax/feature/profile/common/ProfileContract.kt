package com.filmax.feature.profile.common

import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.domain.user.model.UserProfile

/**
 * Состояние Профиля — одно на телефон и ТВ. Сырые данные (профиль, настройки, палитра) правит
 * модель, подписи для экранов ([initials], [username], [subscription], [options]…) она же
 * пересчитывает методами `with*`: экраны ничего не выводят из домена сами.
 */
data class ProfileState(
    val profile: UserProfile? = null,
    /** Кол-во просмотренного контента — из истории просмотров (`watching/history`). */
    val watchedCount: Int = 0,
    /** Кол-во элементов в избранном — из локального кэша favorites. */
    val favoritesCount: Int = 0,
    /** Максимальное качество устройства — из `device/info` (4K/HDR/HEVC/HD). */
    val quality: String? = null,
    /** Пользовательские настройки воспроизведения (качество/аудио/субтитры). */
    val playback: PlaybackSettings = PlaybackSettings(),
    /** Палитра интерфейса — общая для телефона и ТВ. */
    val palette: ThemePalette = ThemePalette.VIOLET,
    /** Фокус на ТВ без белой обводки и свечения (см. ThemeSettingsRepository.plainFocus). */
    val plainFocus: Boolean = false,
    /**
     * Настройка, чьи варианты показывает шторка или боковая панель. Остаётся и после закрытия:
     * панель ТВ уезжает со своим содержимым, а фокус возвращается на строку этой настройки.
     */
    val setting: ProfileSetting = ProfileSetting.QUALITY,
    /** Шторка (телефон) или панель (ТВ) вариантов открыта. */
    val settingOpen: Boolean = false,
    /** Открыт диалог «Выйти из аккаунта?». */
    val confirmLogout: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    /** Инициалы для аватара; профиль ещё не загружен или имя пустое — «?». */
    val initials: String = NO_INITIALS,
    val username: String = GUEST,
    /** Почта под именем; пустую kino.pub тоже присылает — тогда строки нет. */
    val email: String? = null,
    val subscription: ProfileSubscription = profileSubscription(subscription = null),
    /** Субтитры включены — выбран любой язык, кроме «Выкл». */
    val subtitlesEnabled: Boolean = false,
    /** Варианты открытой настройки по порядку и индекс текущего — для шторки и панели. */
    val options: List<String> = optionsOf(ProfileSetting.QUALITY),
    val selectedOption: Int = 0,
) {
    /** Текущее значение настройки — подпись справа в строке. */
    fun valueOf(setting: ProfileSetting): String = when (setting) {
        ProfileSetting.QUALITY -> playback.quality
        ProfileSetting.AUDIO -> playback.audioLanguage
        ProfileSetting.SUBTITLES -> playback.subtitleLanguage
        ProfileSetting.PALETTE -> paletteLabel(palette)
    }

    fun withProfile(profile: UserProfile): ProfileState = copy(
        loading = false,
        profile = profile,
        initials = initialsOf(profile),
        username = profile.username,
        email = profile.email?.takeIf { it.isNotBlank() },
        subscription = profileSubscription(profile.subscription),
    )

    fun withPlayback(settings: PlaybackSettings): ProfileState = copy(playback = settings).rendered()

    fun withPalette(palette: ThemePalette): ProfileState = copy(palette = palette).rendered()

    fun withSetting(setting: ProfileSetting, open: Boolean): ProfileState =
        copy(setting = setting, settingOpen = open).rendered()

    /** Поля шторки и переключателя — из настроек; зовётся после каждой их смены. */
    private fun rendered(): ProfileState {
        val options = optionsOf(setting)
        return copy(
            subtitlesEnabled = playback.subtitleLanguage != PlaybackSettings.SubtitleOff,
            options = options,
            selectedOption = options.indexOf(valueOf(setting)),
        )
    }
}

private const val NO_INITIALS = "?"
private const val GUEST = "Гость"

/**
 * Подписка словами для обеих карточек: телефон показывает [title] и [subtitle], ТВ — [overline],
 * крупное [value] и [caption]. Менять подписку из приложения kino.pub не даёт — карточка справочная.
 */
data class ProfileSubscription(
    val active: Boolean,
    val title: String,
    val subtitle: String?,
    val overline: String,
    val value: String,
    val caption: String?,
)

/** Настройка со списком вариантов: её значение выбирают в шторке или панели. [group] — раздел панели ТВ. */
enum class ProfileSetting(val label: String, val group: String) {
    QUALITY("Качество видео", "Просмотр"),
    AUDIO("Язык аудио", "Просмотр"),
    SUBTITLES("Субтитры", "Просмотр"),
    PALETTE("Палитра", "Оформление"),
}

sealed interface ProfileEvent {
    data object Logout : ProfileEvent
    data class SetQuality(val quality: String) : ProfileEvent
    data class SetAudioLanguage(val language: String) : ProfileEvent
    data class SetSubtitleLanguage(val language: String) : ProfileEvent

    /** Переключатель «Субтитры» на ТВ: включить — язык по умолчанию, выключить — «Выкл». */
    data class SetSubtitlesEnabled(val enabled: Boolean) : ProfileEvent

    /** Переключатель в «Настройках UI» на ТВ: фокус без белой обводки и свечения. */
    data class SetPlainFocus(val enabled: Boolean) : ProfileEvent

    /** «ОК» на строке со значением — шторка или панель с вариантами этой настройки. */
    data class OpenSetting(val setting: ProfileSetting) : ProfileEvent
    data object CloseSetting : ProfileEvent

    /** Вариант [index] из [ProfileState.options]: применить и закрыть шторку. */
    data class ChooseOption(val index: Int) : ProfileEvent

    data object AskLogout : ProfileEvent
    data object DismissLogout : ProfileEvent

    /** Окно оценки приложения. */
    data object OpenRate : ProfileEvent

    /** Пульт телевизора (телефон). */
    data object OpenRemote : ProfileEvent

    /** Каталог компонентов дизайн-системы — строка есть только в debug-сборке. */
    data object OpenDesignSystem : ProfileEvent
}

/** Одноразовых эффектов у Профиля нет: после выхода модель сама делает онбординг корнем. */
sealed interface ProfileSideEffect
