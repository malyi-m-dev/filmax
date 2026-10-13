// Секции ТВ-Профиля: шапка аккаунта, группы настроек и карточка подписки.
package com.filmax.feature.profile.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSettingRow
import com.filmax.core.tv.designsystem.TvSubscriptionCard
import com.filmax.core.tv.designsystem.TvSwitchRow
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.FilmaxVersionLabel
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileSetting
import com.filmax.feature.profile.common.ProfileState
import com.filmax.feature.profile.common.activeUntil
import com.filmax.feature.profile.common.daysLabel
import com.filmax.feature.profile.common.initialsOrFallback
import com.filmax.feature.profile.common.subtitlesEnabled
import com.filmax.feature.profile.common.valueOf

/** Ширина колонки настроек: строку во весь экран с трёх метров не прочитать. */
private val SettingsWidth = 520.dp

/** Запас по бокам колонки под увеличение строки в фокусе: прокрутка клипает края. */
private val FocusRoom = 8.dp
private val HeaderTop = 36.dp
private val AvatarSize = 72.dp
private val CardTop = 162.dp
private val NameStyle = FxType.screenTitle.copy(fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em)

/**
 * Шаги появления, как в макете: шапка, строки настроек сверху вниз, карточка подписки — вместе
 * с третьей строкой.
 */
private const val FIRST_ROW_ENTER = 1
private const val CARD_ENTER = 3

/** Ключ строки настройки для памяти фокуса; стартовый фокус — на «Качестве видео». */
internal fun settingKey(setting: ProfileSetting) = "setting:${setting.name}"

@Composable
internal fun ProfileBody(state: ProfileState, actions: ProfileActions, focus: ProfileFocus) {
    Row(Modifier.fillMaxHeight()) {
        SettingsColumn(state, actions, focus)
        SubscriptionCard(
            state.profile?.subscription,
            Modifier.padding(start = 30.dp, top = CardTop).fxEnter(CARD_ENTER)
        )
    }
}

/**
 * Колонка настроек — контейнер фокуса экрана: старт на первой строке, возврат из панели — на
 * строку, которую открывали.
 */
@Composable
private fun SettingsColumn(state: ProfileState, actions: ProfileActions, focus: ProfileFocus) {
    val scroll = rememberScrollState()
    ScrollToTopOnNavFocus(scroll)
    val screenFocus = rememberTvScreenFocus(startAt = settingKey(ProfileSetting.QUALITY))
    Column(
        Modifier
            .padding(start = TvMetrics.SectionStart - FocusRoom)
            .width(SettingsWidth + FocusRoom * 2)
            .then(screenFocus.containerModifier)
            .verticalScroll(scroll)
            .padding(horizontal = FocusRoom, vertical = HeaderTop),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileHeader(state.profile, Modifier.fxEnter(0))
        SettingsGroup("Просмотр")
        val rows = SettingRows(state, actions, focus, screenFocus)
        rows.Value(ProfileSetting.QUALITY, enter = FIRST_ROW_ENTER)
        rows.Value(ProfileSetting.AUDIO, enter = FIRST_ROW_ENTER + 1)
        SubtitlesRow(rows, enter = FIRST_ROW_ENTER + 2)
        SettingsGroup("Оформление")
        rows.Value(ProfileSetting.PALETTE, enter = FIRST_ROW_ENTER + 3)
        AccountRows(rows, enter = FIRST_ROW_ENTER + 4)
    }
}

/** Всё, что нужно строкам настроек: состояние, события и фокус. */
private class SettingRows(
    val state: ProfileState,
    val actions: ProfileActions,
    val focus: ProfileFocus,
    val screenFocus: TvScreenFocus,
) {
    /** Строка со значением: «ОК» открывает панель вариантов, фокус потом возвращается сюда. */
    @Composable
    fun Value(setting: ProfileSetting, enter: Int) {
        TvSettingRow(
            label = setting.label,
            value = state.valueOf(setting),
            onClick = { actions.onEvent(ProfileEvent.OpenSetting(setting)) },
            modifier = Modifier
                .fxEnter(enter)
                .then(screenFocus.item(settingKey(setting)))
                .focusRequester(focus.settings.getValue(setting)),
        )
    }
}

/**
 * Субтитры — переключатель, как в макете: «ОК» включает язык по умолчанию или выключает их.
 * Другой язык выбирают в плеере — там он запоминается для всего сериала.
 */
@Composable
private fun SubtitlesRow(rows: SettingRows, enter: Int) {
    val setting = ProfileSetting.SUBTITLES
    TvSwitchRow(
        label = setting.label,
        checked = rows.state.subtitlesEnabled,
        onCheckedChange = { enabled -> rows.actions.onEvent(ProfileEvent.SetSubtitlesEnabled(enabled)) },
        modifier = Modifier
            .fxEnter(enter)
            .then(rows.screenFocus.item(settingKey(setting)))
            .focusRequester(rows.focus.settings.getValue(setting)),
    )
}

/** «Аккаунт» — выход с подтверждением; «Приложение» — ручная проверка обновлений и версия. */
@Composable
private fun AccountRows(rows: SettingRows, enter: Int) {
    val actions = rows.actions
    SettingsGroup("Аккаунт")
    TvSettingRow(
        label = "Выйти из аккаунта",
        value = null,
        onClick = { actions.onEvent(ProfileEvent.AskLogout) },
        modifier = Modifier.fxEnter(enter).then(rows.screenFocus.item("logout")).focusRequester(rows.focus.logout),
        destructive = true,
    )
    // На телевизоре магазина нет — приложение ставится APK, и ручная проверка здесь нужнее,
    // чем на телефоне.
    SettingsGroup("Приложение")
    TvSettingRow(
        label = "Проверить обновления",
        value = null,
        onClick = actions.onCheckUpdates,
        modifier = Modifier.fxEnter(enter + 1).then(rows.screenFocus.item("updates")),
    )
    FilmaxVersionLabel(color = LocalFxColors.current.textDim, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun SettingsGroup(title: String) {
    TvOverline(title, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp), accent = false)
}

/** Аватар с инициалами вторым акцентом, имя и почта. */
@Composable
private fun ProfileHeader(profile: UserProfile?, modifier: Modifier) {
    val colors = LocalFxColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(AvatarSize).background(colors.accent2, CircleShape), contentAlignment = Alignment.Center) {
            Text(profile.initialsOrFallback(), style = NameStyle, color = colors.onAccent2)
        }
        Column {
            Text(profile?.username ?: "Гость", style = NameStyle, color = colors.text, maxLines = 1)
            profile?.email?.let { email ->
                Text(
                    email,
                    style = FxType.metaRow,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Карточка подписки: сколько дней осталось и до какого числа. Полосы остатка и «Продлить» из
 * макета нет: длительность оплаченного периода API не отдаёт, а продление — только на сайте.
 */
@Composable
private fun SubscriptionCard(subscription: Subscription?, modifier: Modifier) {
    val active = subscription?.active == true
    val days = subscription?.daysLeft?.takeIf { active }
    TvSubscriptionCard(
        overline = if (active) "Filmax Premium" else "Подписка",
        value = when {
            days != null -> daysLabel(days)
            active -> "Активна"
            else -> "Нет"
        },
        caption = if (active) subscription?.activeUntil() else "Оформить можно на сайте kino.pub",
        modifier = modifier,
    )
}
