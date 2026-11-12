// Профиль: модель и экран из состояния — аватар и подписка сверху, ниже группы настроек (секции — в
// ProfileSections). Какая шторка открыта и что в ней, решает общий с ТВ ProfileScreenModel; шторки —
// в ProfileSheets.

package com.filmax.feature.profile.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.radialWash
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxSettingsDivider
import com.filmax.core.ui.components.FilmaxSettingsGroup
import com.filmax.core.ui.components.FilmaxSettingsRow
import com.filmax.core.ui.components.FilmaxStatusBarGlass
import com.filmax.core.ui.components.FilmaxVersionLabel
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.core.ui.preview.PhonePreviews
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileSamples
import com.filmax.feature.profile.common.ProfileScreenModel
import com.filmax.feature.profile.common.ProfileSetting
import com.filmax.feature.profile.common.ProfileSideEffect
import com.filmax.feature.profile.common.ProfileState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Переходы и действия Профиля одним объектом: порог LongParameterList у detekt — 6. */
internal data class ProfileActions(
    val onEvent: (ProfileEvent) -> Unit,
    val onCheckUpdates: () -> Unit,
    val onRate: () -> Unit,
    /** Пульт телевизора в той же сети. */
    val onOpenRemote: () -> Unit,
    /** Каталог компонентов — только у debug-сборки; null — строки нет. */
    val onOpenDesignSystem: (() -> Unit)?,
) {
    companion object {
        /** Ничего не делает — для превью. */
        val None = ProfileActions(
            onEvent = {},
            onCheckUpdates = {},
            onRate = {},
            onOpenRemote = {},
            onOpenDesignSystem = null,
        )
    }
}

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
    val actions = remember(screenModel, navigator, onCheckUpdates, showDesignSystem) {
        ProfileActions(
            onEvent = screenModel::dispatch,
            onCheckUpdates = onCheckUpdates,
            onRate = { navigator.open(Destination.Feedback(FeedbackTrigger.Profile.name)) },
            onOpenRemote = { navigator.open(Destination.Remote) },
            onOpenDesignSystem = { navigator.open(Destination.DesignSystem) }.takeIf { showDesignSystem },
        )
    }
    ProfileScreen(state = state, actions = actions, modifier = modifier)
}

/** Профиль из состояния — без Koin и навигатора: его рисуют превью. */
@Composable
internal fun ProfileScreen(state: ProfileState, actions: ProfileActions, modifier: Modifier = Modifier) {
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
            ProfileContent(state = state, actions = actions)
        }
    }
    ProfileSheets(state = state, onEvent = actions.onEvent)
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
        AccessCard(state)
        PlaybackGroup(state = state, onEvent = actions.onEvent)
        // Телефон как пульт телевизора с Filmax в той же Wi-Fi: выбор ТВ — на самом экране пульта.
        FilmaxSettingsGroup(title = "Телевизор") {
            FilmaxSettingsRow(Icons.Filled.Cast, "Пульт телевизора", onClick = actions.onOpenRemote)
        }
        // Блока «Устройство» временно нет: device/info и device/settings отвечают 500,
        // и строка вела на нерабочий экран. Вернуть, когда бэкенд починят.
        AppGroup(actions = actions)
        FilmaxSettingsGroup(title = "Аккаунт") {
            // Выход — из текущего источника: второй остаётся с входом.
            SettingRow(state, ProfileSetting.SOURCE, Icons.Filled.Dns, actions.onEvent)
            FilmaxSettingsDivider()
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

private class ProfileStatePreviews : PreviewParameterProvider<ProfileState> {
    override val values = ProfileSamples.all.asSequence()
}

@PhonePreviews
@Composable
private fun ProfileScreenPreview(@PreviewParameter(ProfileStatePreviews::class) state: ProfileState) {
    FilmaxTheme { ProfileScreen(state = state, actions = ProfileActions.None) }
}
