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
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
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
import com.filmax.feature.profile.common.ProfileSideEffect
import com.filmax.feature.profile.common.ProfileState
import com.filmax.feature.profile.common.activeUntil
import com.filmax.feature.profile.common.daysLabel
import com.filmax.feature.profile.common.initialsOrFallback
import com.filmax.feature.profile.common.valueOf
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Переходы и действия Профиля одним объектом: порог LongParameterList у detekt — 6. */
private data class ProfileActions(
    val onEvent: (ProfileEvent) -> Unit,
    val onCheckUpdates: () -> Unit,
    val onRate: () -> Unit,
    /** Пульт телевизора в той же сети. */
    val onOpenRemote: () -> Unit,
    val onOpenDesignSystem: (() -> Unit)?,
)

/**
 * [onCheckUpdates] — не навигация, а действие: обновления живут в `:app` (GitHub Releases),
 * и фича до них не дотягивается. [showDesignSystem] включает строку каталога компонентов —
 * граф отдаёт её только debug-сборке.
 */
@Composable
fun ProfileScreen(
    onCheckUpdates: () -> Unit,
    showDesignSystem: Boolean = false,
    modifier: Modifier = Modifier,
    screenModel: ProfileScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            // Вход становится единственным корнем: «Назад» не возвращает в аккаунт, из которого вышли.
            ProfileSideEffect.LoggedOut -> navigator.root(Destination.Onboarding)
        }
    }
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
            ProfileContent(
                state = state,
                actions = ProfileActions(
                    onEvent = screenModel::dispatch,
                    onCheckUpdates = onCheckUpdates,
                    onRate = { navigator.open(Destination.Feedback(FeedbackTrigger.Profile.name)) },
                    onOpenRemote = { navigator.open(Destination.Remote) },
                    onOpenDesignSystem = if (showDesignSystem) {
                        { navigator.open(Destination.DesignSystem) }
                    } else {
                        null
                    },
                ),
            )
        }
    }
    ProfileSheets(state = state, onEvent = screenModel::dispatch)
}

@Composable
private fun ProfileContent(state: ProfileState, actions: ProfileActions) {
    val glass = rememberGlassState()
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        ProfileColumn(state = state, actions = actions, modifier = Modifier.glassSource(glass).verticalScroll(scroll))
        FilmaxStatusBarGlass(glass = glass, visible = scroll.value > 0)
    }
}

@Composable
private fun ProfileColumn(state: ProfileState, actions: ProfileActions, modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = FilmaxMetrics.GridPadding)
            .padding(top = 28.dp, bottom = LocalNavBarPadding.current),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ProfileHeader(state.profile)
        SubscriptionCard(state.profile)
        PlaybackGroup(state = state, onEvent = actions.onEvent)
        // Телефон как пульт телевизора с Filmax в той же Wi-Fi: выбор ТВ — на самом экране пульта.
        FilmaxSettingsGroup(title = "Телевизор") {
            FilmaxSettingsRow(Icons.Filled.Cast, "Пульт телевизора", onClick = actions.onOpenRemote)
        }
        // Блока «Устройство» временно нет: device/info и device/settings отвечают 500,
        // и строка вела на нерабочий экран. Вернуть, когда бэкенд починят.
        AppGroup(actions = actions)
        FilmaxSettingsGroup(title = "Аккаунт") {
            FilmaxSettingsRow(
                Icons.AutoMirrored.Filled.Logout,
                "Выйти",
                onClick = { actions.onEvent(ProfileEvent.AskLogout) },
                destructive = true,
            )
        }
        FilmaxVersionLabel(color = FilmaxColors.TextDim, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile?) {
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
private fun AppGroup(actions: ProfileActions) {
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
