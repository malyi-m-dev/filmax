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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.FilmaxVersionLabel
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileSetting
import com.filmax.feature.profile.common.ProfileState
import com.filmax.feature.profile.common.activeUntil
import com.filmax.feature.profile.common.daysLabel
import com.filmax.feature.profile.common.initialsOrFallback
import com.filmax.feature.profile.common.valueOf

/** Ширина колонки настроек: строку во весь экран с трёх метров не прочитать. */
private val SettingsWidth = 520.dp

/** Запас по бокам колонки под увеличение строки в фокусе: прокрутка клипает края. */
private val FocusRoom = 8.dp
private val HeaderTop = 36.dp
private val AvatarSize = 72.dp
private val CardWidth = 290.dp
private val CardTop = 162.dp
private val CardShape = RoundedCornerShape(18.dp)
private val NameStyle = FxType.screenTitle.copy(fontSize = 24.sp, lineHeight = 28.sp)
private val DaysStyle = FxType.hero.copy(fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 46.sp)
private const val CARD_TEXT_ALPHA = 0.8f

/** Ключ строки настройки для памяти фокуса; стартовый фокус — на «Качестве видео». */
internal fun settingKey(setting: ProfileSetting) = "setting:${setting.name}"

@Composable
internal fun ProfileBody(state: ProfileState, actions: ProfileActions, focus: ProfileFocus) {
    Row(Modifier.fillMaxHeight()) {
        SettingsColumn(state, actions, focus)
        Box(Modifier.padding(start = 30.dp, top = CardTop)) { SubscriptionCard(state.profile?.subscription) }
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
        ProfileHeader(state.profile)
        SettingsGroup("Просмотр")
        listOf(ProfileSetting.QUALITY, ProfileSetting.AUDIO, ProfileSetting.SUBTITLES).forEach { setting ->
            SettingRow(state, setting, actions, focus, screenFocus)
        }
        SettingsGroup("Оформление")
        SettingRow(state, ProfileSetting.PALETTE, actions, focus, screenFocus)
        AccountRows(actions, focus, screenFocus)
    }
}

@Composable
private fun SettingRow(
    state: ProfileState,
    setting: ProfileSetting,
    actions: ProfileActions,
    focus: ProfileFocus,
    screenFocus: TvScreenFocus,
) {
    TvSettingRow(
        label = setting.label,
        value = state.valueOf(setting),
        onClick = { actions.onEvent(ProfileEvent.OpenSetting(setting)) },
        modifier = screenFocus.item(settingKey(setting)).focusRequester(focus.settings.getValue(setting)),
    )
}

/** «Аккаунт» — выход с подтверждением; «Приложение» — ручная проверка обновлений и версия. */
@Composable
private fun AccountRows(actions: ProfileActions, focus: ProfileFocus, screenFocus: TvScreenFocus) {
    SettingsGroup("Аккаунт")
    TvSettingRow(
        label = "Выйти из аккаунта",
        value = null,
        onClick = { actions.onEvent(ProfileEvent.AskLogout) },
        modifier = screenFocus.item("logout").focusRequester(focus.logout),
        destructive = true,
    )
    // На телевизоре магазина нет — приложение ставится APK, и ручная проверка здесь нужнее,
    // чем на телефоне.
    SettingsGroup("Приложение")
    TvSettingRow(
        label = "Проверить обновления",
        value = null,
        onClick = actions.onCheckUpdates,
        modifier = screenFocus.item("updates"),
    )
    FilmaxVersionLabel(color = LocalFxColors.current.textDim, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun SettingsGroup(title: String) {
    TvOverline(title, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp), accent = false)
}

/** Аватар с инициалами вторым акцентом, имя и почта. */
@Composable
private fun ProfileHeader(profile: UserProfile?) {
    val colors = LocalFxColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
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
private fun SubscriptionCard(subscription: Subscription?) {
    val colors = LocalFxColors.current
    val active = subscription?.active == true
    Column(
        Modifier.width(CardWidth).background(colors.accent, CardShape).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            if (active) "FILMAX PREMIUM" else "ПОДПИСКА",
            style = FxType.overline,
            color = colors.onAccent.copy(alpha = CARD_TEXT_ALPHA),
        )
        val days = subscription?.daysLeft?.takeIf { active }
        Text(
            when {
                days != null -> daysLabel(days)
                active -> "Активна"
                else -> "Нет"
            },
            style = DaysStyle,
            color = colors.onAccent,
        )
        val caption = if (active) subscription?.activeUntil() else "Оформить можно на сайте kino.pub"
        caption?.let { Text(it, style = FxType.meta, color = colors.onAccent.copy(alpha = CARD_TEXT_ALPHA)) }
    }
}
