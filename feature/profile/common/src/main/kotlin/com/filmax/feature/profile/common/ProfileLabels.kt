// Подписи Профиля, общие для телефона и ТВ: инициалы аватара, подписка словами, названия палитр,
// варианты настроек, дни. Чистые функции без UI-зависимостей; результат ложится в ProfileState.

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

/** Инициалы для аватара; имя пустое — «?». */
fun initialsOf(profile: UserProfile): String = profile.initials().ifEmpty { "?" }

/** Строка подписки одной фразой (шапка приложения): Premium с остатком дней, Premium или бесплатный. */
fun subscriptionLabel(subscription: Subscription?): String = when {
    subscription?.active == true && subscription.daysLeft != null -> "Filmax Premium · ещё ${subscription.daysLeft} дн."
    subscription?.active == true -> "Filmax Premium"
    else -> "Бесплатный аккаунт"
}

/** Карточка подписки для обеих платформ: остаток дней и дата окончания, если kino.pub их прислал. */
fun profileSubscription(subscription: Subscription?, zone: ZoneId = ZoneId.systemDefault()): ProfileSubscription {
    val active = subscription?.active == true
    val days = subscription?.daysLeft?.takeIf { active }?.let(::daysLabel)
    val until = subscription?.takeIf { active }?.let { activeUntil(it, zone) }
    return ProfileSubscription(
        active = active,
        title = if (active) "Filmax Премиум" else "Нет подписки",
        subtitle = if (active) {
            listOfNotNull(until, days?.let { "ещё $it" }).joinToString(" · ").ifBlank { null }
        } else {
            "Без подписки kino.pub видео не играет"
        },
        overline = if (active) "Filmax Premium" else "Подписка",
        value = days ?: if (active) "Активна" else "Нет",
        caption = if (active) until else "Оформить можно на сайте kino.pub",
    )
}

/** Название палитры в Профиле. */
fun paletteLabel(palette: ThemePalette): String = when (palette) {
    ThemePalette.VIOLET -> "Фиолет"
    ThemePalette.CORAL -> "Коралл"
    ThemePalette.MINT -> "Мята"
}

/** Варианты настройки по порядку — их показывает панель, и по номеру из неё выбирают. */
fun optionsOf(setting: ProfileSetting): List<String> = when (setting) {
    ProfileSetting.QUALITY -> PlaybackSettings.qualityOptions
    ProfileSetting.AUDIO -> PlaybackSettings.audioOptions
    ProfileSetting.SUBTITLES -> PlaybackSettings.subtitleOptions
    ProfileSetting.PALETTE -> ThemePalette.entries.map(::paletteLabel)
}

/**
 * Язык субтитров для переключателя: включённые — первый язык из вариантов (русский), выключенные —
 * «Выкл». Другой язык выбирают в плеере, и он запоминается для всего сериала.
 */
fun subtitleLanguageFor(enabled: Boolean): String = if (enabled) {
    PlaybackSettings.subtitleOptions.first { it != PlaybackSettings.SubtitleOff }
} else {
    PlaybackSettings.SubtitleOff
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
fun activeUntil(subscription: Subscription, zone: ZoneId = ZoneId.systemDefault()): String? =
    subscription.endsAt?.let { millis -> "активна до ${Instant.ofEpochMilli(millis).atZone(zone).format(DayFormat)}" }

private const val HUNDRED = 100
private const val TEN = 10
private val TEENS = 11..14
private val FEW = 2..4
