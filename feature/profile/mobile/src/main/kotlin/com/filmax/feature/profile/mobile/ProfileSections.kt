// Секции Профиля телефона: аватар с именем, карточка доступа (подписка kino.pub или домашний
// сервер), настройки воспроизведения и группа «Приложение».
package com.filmax.feature.profile.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.domain.source.capabilities
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.ui.components.FilmaxAvatar
import com.filmax.core.ui.components.FilmaxSettingsDivider
import com.filmax.core.ui.components.FilmaxSettingsGroup
import com.filmax.core.ui.components.FilmaxSettingsRow
import com.filmax.core.ui.components.FilmaxSubscriptionCard
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileSetting
import com.filmax.feature.profile.common.ProfileState
import com.filmax.feature.profile.common.activeUntil
import com.filmax.feature.profile.common.daysLabel
import com.filmax.feature.profile.common.initialsOrFallback
import com.filmax.feature.profile.common.label
import com.filmax.feature.profile.common.valueOf

@Composable
internal fun ProfileHeader(profile: UserProfile?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        FilmaxAvatar(initials = profile.initialsOrFallback())
        Text(
            profile?.username ?: "Гость",
            style = FilmaxType.profileName,
            color = FilmaxColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 16.dp),
        )
        profile?.email?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = FilmaxType.bodySmall,
                color = FilmaxColors.TextDim,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Доступ к видео: подписка kino.pub или домашний сервер, которому подписка не нужна. */
@Composable
internal fun AccessCard(state: ProfileState) {
    if (!state.source.capabilities.subscription) {
        FilmaxSubscriptionCard(
            title = state.source.label,
            subtitle = "Своя медиатека, без подписки",
            active = true,
        )
    } else {
        SubscriptionCard(state.profile)
    }
}

/** Подписка — справочная карточка: менять её из приложения kino.pub не даёт. */
@Composable
private fun SubscriptionCard(profile: UserProfile?) {
    val subscription = profile?.subscription
    val active = subscription?.active == true
    val subtitle = if (active) {
        listOfNotNull(subscription?.activeUntil(), subscription?.daysLeft?.let { "ещё ${daysLabel(it)}" })
            .joinToString(" · ")
            .ifBlank { null }
    } else {
        "Без подписки kino.pub видео не играет"
    }
    FilmaxSubscriptionCard(
        title = if (active) "Filmax Премиум" else "Нет подписки",
        subtitle = subtitle,
        active = active
    )
}

/**
 * Настройки воспроизведения и оформления реальны: качество, аудио и субтитры хранятся локально и
 * применяются в плеере, палитра перекрашивает приложение сразу. Заглушек на экране нет.
 */
@Composable
internal fun PlaybackGroup(state: ProfileState, onEvent: (ProfileEvent) -> Unit) {
    FilmaxSettingsGroup(title = "Просмотр") {
        SettingRow(state, ProfileSetting.QUALITY, Icons.Filled.HighQuality, onEvent)
        FilmaxSettingsDivider()
        SettingRow(state, ProfileSetting.AUDIO, Icons.Filled.GraphicEq, onEvent)
        FilmaxSettingsDivider()
        SettingRow(state, ProfileSetting.SUBTITLES, Icons.Filled.Subtitles, onEvent)
        FilmaxSettingsDivider()
        SettingRow(state, ProfileSetting.PALETTE, Icons.Filled.Palette, onEvent)
    }
}

@Composable
internal fun SettingRow(
    state: ProfileState,
    setting: ProfileSetting,
    icon: ImageVector,
    onEvent: (ProfileEvent) -> Unit,
) {
    FilmaxSettingsRow(
        icon = icon,
        label = setting.label,
        value = state.valueOf(setting),
        onClick = { onEvent(ProfileEvent.OpenSetting(setting)) },
    )
}

/**
 * Приложение ставится APK из GitHub Releases, магазина с автообновлением нет — значит проверка
 * должна быть доступна руками, а не только один раз при старте.
 */
@Composable
internal fun AppGroup(actions: ProfileActions) {
    FilmaxSettingsGroup(title = "Приложение") {
        FilmaxSettingsRow(Icons.Filled.SystemUpdate, "Проверить обновления", onClick = actions.onCheckUpdates)
        FilmaxSettingsDivider()
        FilmaxSettingsRow(Icons.Filled.StarRate, "Оценить приложение", onClick = actions.onRate)
        actions.onOpenDesignSystem?.let {
            FilmaxSettingsDivider()
            FilmaxSettingsRow(Icons.Filled.Widgets, "Дизайн-система", onClick = it)
        }
    }
}
