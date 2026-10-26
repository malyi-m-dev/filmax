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
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvSettingRow
import com.filmax.core.tv.designsystem.TvSubscriptionCard
import com.filmax.core.tv.designsystem.TvSwitchRow
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.ui.components.FilmaxVersionLabel
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileSetting
import com.filmax.feature.profile.common.ProfileState
import com.filmax.feature.profile.common.ProfileSubscription

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

/** Ключ строки «Отключить обводку и свечение» в памяти фокуса экрана. */
internal const val PLAIN_FOCUS_KEY = "plainFocus"

/** Колонка настроек слева, справочные карточки справа: подписка и [remoteCard] под ней. */
@Composable
internal fun ProfileBody(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onCheckUpdates: () -> Unit,
    focus: ProfileFocus,
    remoteCard: @Composable (Modifier) -> Unit,
) {
    Row(Modifier.fillMaxHeight()) {
        SettingsColumn(state, onEvent, onCheckUpdates, focus)
        Column(Modifier.padding(start = 30.dp, top = CardTop), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SubscriptionCard(state.subscription, Modifier.fxEnter(CARD_ENTER))
            remoteCard(Modifier.fxEnter(CARD_ENTER + 1))
        }
    }
}

/**
 * Колонка настроек — контейнер фокуса экрана: старт на первой строке, возврат из панели — на
 * строку, которую открывали.
 */
@Composable
private fun SettingsColumn(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onCheckUpdates: () -> Unit,
    focus: ProfileFocus,
) {
    val scroll = rememberScrollState()
    ScrollToTopOnNavFocus(scroll)
    Column(
        Modifier
            .padding(start = TvMetrics.SectionStart - FocusRoom)
            .width(SettingsWidth + FocusRoom * 2)
            .then(focus.screen.containerModifier)
            .verticalScroll(scroll)
            .padding(horizontal = FocusRoom, vertical = HeaderTop),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileHeader(state, Modifier.fxEnter(0))
        SettingsGroup("Просмотр")
        ValueRow(state, ProfileSetting.QUALITY, FIRST_ROW_ENTER, onEvent, focus)
        ValueRow(state, ProfileSetting.AUDIO, FIRST_ROW_ENTER + 1, onEvent, focus)
        SubtitlesRow(state, FIRST_ROW_ENTER + 2, onEvent, focus)
        SettingsGroup("Оформление")
        ValueRow(state, ProfileSetting.PALETTE, FIRST_ROW_ENTER + 3, onEvent, focus)
        UiRows(state, FIRST_ROW_ENTER + 4, onEvent, focus)
        AccountRows(FIRST_ROW_ENTER + 5, onEvent, onCheckUpdates, focus)
    }
}

/** Строка со значением: «ОК» открывает панель вариантов, фокус потом возвращается сюда. */
@Composable
private fun ValueRow(
    state: ProfileState,
    setting: ProfileSetting,
    enter: Int,
    onEvent: (ProfileEvent) -> Unit,
    focus: ProfileFocus,
) {
    TvSettingRow(
        label = setting.label,
        value = state.valueOf(setting),
        onClick = { onEvent(ProfileEvent.OpenSetting(setting)) },
        modifier = Modifier
            .fxEnter(enter)
            .then(focus.screen.item(settingKey(setting)))
            .focusRequester(focus.settings.getValue(setting)),
    )
}

/**
 * Субтитры — переключатель, как в макете: «ОК» включает язык по умолчанию или выключает их.
 * Другой язык выбирают в плеере — там он запоминается для всего сериала.
 */
@Composable
private fun SubtitlesRow(state: ProfileState, enter: Int, onEvent: (ProfileEvent) -> Unit, focus: ProfileFocus) {
    val setting = ProfileSetting.SUBTITLES
    TvSwitchRow(
        label = setting.label,
        checked = state.subtitlesEnabled,
        onCheckedChange = { enabled -> onEvent(ProfileEvent.SetSubtitlesEnabled(enabled)) },
        modifier = Modifier
            .fxEnter(enter)
            .then(focus.screen.item(settingKey(setting)))
            .focusRequester(focus.settings.getValue(setting)),
    )
}

/**
 * «Настройки UI» — как выглядит сам интерфейс. Первый переключатель убирает белую обводку и
 * свечение у элемента в фокусе: кому они мешают, остаются увеличение и цвет подложки.
 */
@Composable
private fun UiRows(state: ProfileState, enter: Int, onEvent: (ProfileEvent) -> Unit, focus: ProfileFocus) {
    SettingsGroup("Настройки UI")
    TvSwitchRow(
        label = "Отключить обводку и свечение",
        checked = state.plainFocus,
        onCheckedChange = { enabled -> onEvent(ProfileEvent.SetPlainFocus(enabled)) },
        modifier = Modifier.fxEnter(enter).then(focus.screen.item(PLAIN_FOCUS_KEY)),
    )
}

/** «Аккаунт» — выход с подтверждением; «Приложение» — ручная проверка обновлений и версия. */
@Composable
private fun AccountRows(enter: Int, onEvent: (ProfileEvent) -> Unit, onCheckUpdates: () -> Unit, focus: ProfileFocus) {
    SettingsGroup("Аккаунт")
    TvSettingRow(
        label = "Выйти из аккаунта",
        value = null,
        onClick = { onEvent(ProfileEvent.AskLogout) },
        modifier = Modifier.fxEnter(enter).then(focus.screen.item("logout")).focusRequester(focus.logout),
        destructive = true,
    )
    // На телевизоре магазина нет — приложение ставится APK, и ручная проверка здесь нужнее,
    // чем на телефоне.
    SettingsGroup("Приложение")
    TvSettingRow(
        label = "Проверить обновления",
        value = null,
        onClick = onCheckUpdates,
        modifier = Modifier.fxEnter(enter + 1).then(focus.screen.item("updates")),
    )
    TvSettingRow(
        label = "Оценить приложение",
        value = null,
        onClick = { onEvent(ProfileEvent.OpenRate) },
        modifier = Modifier.fxEnter(enter + 1).then(focus.screen.item("rate")),
    )
    FilmaxVersionLabel(color = LocalFxColors.current.textDim, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun SettingsGroup(title: String) {
    TvOverline(title, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp), accent = false)
}

/** Аватар с инициалами вторым акцентом, имя и почта. */
@Composable
private fun ProfileHeader(state: ProfileState, modifier: Modifier) {
    val colors = LocalFxColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(AvatarSize).background(colors.accent2, CircleShape), contentAlignment = Alignment.Center) {
            Text(state.initials, style = NameStyle, color = colors.onAccent2)
        }
        Column {
            Text(state.username, style = NameStyle, color = colors.text, maxLines = 1)
            state.email?.let { email ->
                Text(
                    email,
                    style = FxType.metaRow,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
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
private fun SubscriptionCard(subscription: ProfileSubscription, modifier: Modifier) {
    TvSubscriptionCard(
        overline = subscription.overline,
        value = subscription.value,
        caption = subscription.caption,
        modifier = modifier,
    )
}
