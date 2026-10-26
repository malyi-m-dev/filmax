// Профиль: аватар и подписка сверху, ниже — группы настроек. Варианты настройки выбирают в шторке;
// какая открыта и что в ней, решает общий с ТВ ProfileScreenModel. Шторки — в ProfileSheets.

package com.filmax.feature.profile.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.radialWash
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.ui.components.FilmaxAvatar
import com.filmax.core.ui.components.FilmaxSettingsDivider
import com.filmax.core.ui.components.FilmaxSettingsGroup
import com.filmax.core.ui.components.FilmaxSettingsRow
import com.filmax.core.ui.components.FilmaxStatusBarGlass
import com.filmax.core.ui.components.FilmaxSubscriptionCard
import com.filmax.core.ui.components.FilmaxVersionLabel
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileScreenModel
import com.filmax.feature.profile.common.ProfileSetting
import com.filmax.feature.profile.common.ProfileState
import org.koin.androidx.compose.koinViewModel

/**
 * [onCheckUpdates] — не навигация, а действие: обновления живут в `:app` (GitHub Releases),
 * и фича до них не дотягивается. [showDesignSystem] включает строку каталога компонентов —
 * граф отдаёт её только debug-сборке. Остальное — события модели.
 */
@Composable
fun ProfileScreen(
    onCheckUpdates: () -> Unit,
    showDesignSystem: Boolean = false,
    modifier: Modifier = Modifier,
    screenModel: ProfileScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val onEvent: (ProfileEvent) -> Unit = screenModel::dispatch
    val palette = FilmaxTheme.palette
    Box(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .radialWash(palette.wash, centerX = 0.5f, centerY = 0f, radiusX = 0.8f, radiusY = 0.35f),
    ) {
        if (state.loading) {
            CircularProgressIndicator(color = palette.accent, modifier = Modifier.align(Alignment.Center))
        } else {
            ProfileContent(state, onEvent, onCheckUpdates, showDesignSystem)
        }
    }
    ProfileSheets(state = state, onEvent = onEvent)
}

@Composable
private fun ProfileContent(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onCheckUpdates: () -> Unit,
    showDesignSystem: Boolean,
) {
    val glass = rememberGlassState()
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .glassSource(glass)
                .verticalScroll(scroll)
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = FilmaxMetrics.GridPadding)
                .padding(top = 28.dp, bottom = LocalNavBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ProfileHeader(state)
            // Подписка — справочная карточка: менять её из приложения kino.pub не даёт.
            FilmaxSubscriptionCard(
                title = state.subscription.title,
                subtitle = state.subscription.subtitle,
                active = state.subscription.active,
            )
            PlaybackGroup(state = state, onEvent = onEvent)
            // Телефон как пульт телевизора с Filmax в той же Wi-Fi: выбор ТВ — на самом экране пульта.
            FilmaxSettingsGroup(title = "Телевизор") {
                FilmaxSettingsRow(Icons.Filled.Cast, "Пульт телевизора", onClick = { onEvent(ProfileEvent.OpenRemote) })
            }
            // Блока «Устройство» временно нет: device/info и device/settings отвечают 500,
            // и строка вела на нерабочий экран. Вернуть, когда бэкенд починят.
            AppGroup(onEvent = onEvent, onCheckUpdates = onCheckUpdates, showDesignSystem = showDesignSystem)
            FilmaxSettingsGroup(title = "Аккаунт") {
                FilmaxSettingsRow(
                    Icons.AutoMirrored.Filled.Logout,
                    "Выйти",
                    onClick = { onEvent(ProfileEvent.AskLogout) },
                    destructive = true,
                )
            }
            FilmaxVersionLabel(color = FilmaxColors.TextDim, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        FilmaxStatusBarGlass(glass = glass, visible = scroll.value > 0)
    }
}

@Composable
private fun ProfileHeader(state: ProfileState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        FilmaxAvatar(initials = state.initials)
        Text(
            state.username,
            style = FilmaxType.profileName,
            color = FilmaxColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 16.dp),
        )
        state.email?.let {
            Text(
                it,
                style = FilmaxType.bodySmall,
                color = FilmaxColors.TextDim,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * Настройки воспроизведения и оформления реальны: качество, аудио и субтитры хранятся локально и
 * применяются в плеере, палитра перекрашивает приложение сразу. Заглушек на экране нет.
 */
@Composable
private fun PlaybackGroup(state: ProfileState, onEvent: (ProfileEvent) -> Unit) {
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
private fun SettingRow(
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
private fun AppGroup(onEvent: (ProfileEvent) -> Unit, onCheckUpdates: () -> Unit, showDesignSystem: Boolean) {
    FilmaxSettingsGroup(title = "Приложение") {
        FilmaxSettingsRow(Icons.Filled.SystemUpdate, "Проверить обновления", onClick = onCheckUpdates)
        FilmaxSettingsDivider()
        FilmaxSettingsRow(Icons.Filled.StarRate, "Оценить приложение", onClick = { onEvent(ProfileEvent.OpenRate) })
        if (showDesignSystem) {
            FilmaxSettingsDivider()
            FilmaxSettingsRow(
                Icons.Filled.Widgets,
                "Дизайн-система",
                onClick = { onEvent(ProfileEvent.OpenDesignSystem) },
            )
        }
    }
}
