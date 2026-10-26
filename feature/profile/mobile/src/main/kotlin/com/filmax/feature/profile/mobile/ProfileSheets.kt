// Шторки Профиля: варианты настройки (качество, аудио, субтитры, палитра) и «Выйти из аккаунта?».
// Какая настройка открыта, её варианты и выбранный — в ProfileState, как и на ТВ.

package com.filmax.feature.profile.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.ui.components.FilmaxConfirmSheet
import com.filmax.core.ui.components.FilmaxRadioRow
import com.filmax.core.ui.components.FilmaxSheet
import com.filmax.core.ui.components.toFilmaxPalette
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileSetting
import com.filmax.feature.profile.common.ProfileState

@Composable
internal fun ProfileSheets(state: ProfileState, onEvent: (ProfileEvent) -> Unit) {
    if (state.settingOpen) SettingSheet(state = state, onEvent = onEvent)
    if (state.confirmLogout) {
        FilmaxConfirmSheet(
            title = "Выйти из аккаунта?",
            message = "Устройство отвяжется от kino.pub — чтобы смотреть снова, войдите кодом с сайта.",
            confirmLabel = "Выйти",
            onConfirm = { onEvent(ProfileEvent.Logout) },
            onDismiss = { onEvent(ProfileEvent.DismissLogout) },
        )
    }
}

/** Варианты настройки радиокнопками; выбор применяется и закрывает шторку (так решает модель). */
@Composable
private fun SettingSheet(state: ProfileState, onEvent: (ProfileEvent) -> Unit) {
    FilmaxSheet(onDismiss = { onEvent(ProfileEvent.CloseSetting) }, title = state.setting.label) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = FilmaxMetrics.GridPadding)
                .padding(bottom = 20.dp),
        ) {
            state.options.forEachIndexed { index, option ->
                FilmaxRadioRow(
                    label = option,
                    selected = index == state.selectedOption,
                    onClick = { onEvent(ProfileEvent.ChooseOption(index)) },
                    // У палитры рядом с названием — её акцент: выбирают цвет, а не слово.
                    swatch = paletteSwatch(state.setting, index),
                )
            }
        }
    }
}

/** Кружок акцента у варианта палитры; у остальных настроек кружка нет. */
private fun paletteSwatch(setting: ProfileSetting, index: Int): Color? =
    if (setting == ProfileSetting.PALETTE) ThemePalette.entries[index].toFilmaxPalette().accent else null
