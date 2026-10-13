// Общие для mobile и tv подписи Профиля: инициалы аватара, строка подписки, варианты настроек.
// Чистые функции без UI-зависимостей.

package com.filmax.feature.profile.common

import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.domain.user.model.initials
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Russian = Locale.forLanguageTag("ru")

/** «29 ноября» — месяц в родительном падеже: так его отдаёт русская локаль в формате даты. */
private val DayFormat = DateTimeFormatter.ofPattern("d MMMM", Russian)

/** Инициалы для аватара; профиль ещё не загружен или имя пустое — «?». */
fun UserProfile?.initialsOrFallback(): String = this?.initials()?.ifEmpty { "?" } ?: "?"

/** Строка подписки под именем: Premium с остатком дней, просто Premium или бесплатный аккаунт. */
fun Subscription?.label(): String = when {
    this?.active == true && daysLeft != null -> "Filmax Premium · ещё $daysLeft дн."
    this?.active == true -> "Filmax Premium"
    else -> "Бесплатный аккаунт"
}

/** Название палитры в Профиле. */
val ThemePalette.label: String
    get() = when (this) {
        ThemePalette.VIOLET -> "Фиолет"
        ThemePalette.CORAL -> "Коралл"
        ThemePalette.MINT -> "Мята"
    }

/** Варианты настройки по порядку — их показывает панель, и по номеру из неё выбирают. */
fun optionsOf(setting: ProfileSetting): List<String> = when (setting) {
    ProfileSetting.QUALITY -> PlaybackSettings.qualityOptions
    ProfileSetting.AUDIO -> PlaybackSettings.audioOptions
    ProfileSetting.SUBTITLES -> PlaybackSettings.subtitleOptions
    ProfileSetting.PALETTE -> ThemePalette.entries.map { it.label }
}

/** Текущее значение настройки — подпись справа в строке. */
fun ProfileState.valueOf(setting: ProfileSetting): String = when (setting) {
    ProfileSetting.QUALITY -> playback.quality
    ProfileSetting.AUDIO -> playback.audioLanguage
    ProfileSetting.SUBTITLES -> playback.subtitleLanguage
    ProfileSetting.PALETTE -> palette.label
}

/** «47 дней», «1 день», «3 дня» — крупная цифра на карточке подписки. */
fun daysLabel(days: Int): String {
    val lastTwo = days % HUNDRED
    val last = days % TEN
    val word = when {
        lastTwo in TEENS -> "дней"
        last == 1 -> "день"
        last in FEW -> "дня"
        else -> "дней"
    }
    return "$days $word"
}

/** «активна до 29 ноября»; дата окончания неизвестна — null. */
fun Subscription.activeUntil(zone: ZoneId = ZoneId.systemDefault()): String? =
    endsAt?.let { millis -> "активна до ${Instant.ofEpochMilli(millis).atZone(zone).format(DayFormat)}" }

private const val HUNDRED = 100
private const val TEN = 10
private val TEENS = 11..14
private val FEW = 2..4
