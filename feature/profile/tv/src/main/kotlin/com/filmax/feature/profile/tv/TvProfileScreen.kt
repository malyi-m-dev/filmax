package com.filmax.feature.profile.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvConfirmDialog
import com.filmax.core.tv.designsystem.TvConfirmSpec
import com.filmax.core.tv.designsystem.TvOption
import com.filmax.core.tv.designsystem.TvOptionSheet
import com.filmax.core.tv.designsystem.TvOptionSheetState
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSheetHost
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.tv.designsystem.tvSectionBackground
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileScreenModel
import com.filmax.feature.profile.common.ProfileSetting
import com.filmax.feature.profile.common.ProfileState
import org.koin.androidx.compose.koinViewModel

/**
 * ТВ-Профиль (экран 09): шапка аккаунта, настройки строками, карточка подписки справа. «ОК» на
 * строке со значением открывает боковую панель с вариантами, «Выйти из аккаунта» спрашивает
 * подтверждение с фокусом на «Отмена». Палитра применяется ко всему ТВ-UI сразу. Логика —
 * в общем с телефоном [ProfileScreenModel], экран рисует и отдаёт события.
 *
 * Блока «Настройки устройства» здесь нет намеренно: device/info и device/settings отвечают 500.
 *
 * [onCheckUpdates] — действие `:app` (обновления из GitHub Releases), а [remoteCard] — карточка
 * «Пульт с телефона» под подпиской. Их даёт граф: фичи друг от друга не зависят.
 */
@Composable
fun TvProfileScreen(
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier,
    remoteCard: @Composable (Modifier) -> Unit = {},
    screenModel: ProfileScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    ProfileContent(state, screenModel::dispatch, onCheckUpdates, modifier, remoteCard)
}

/**
 * Куда вернуть фокус: на строку настройки, чья панель закрылась, и на «Выйти», когда закрылся
 * диалог. [screen] — вход на экран и возврат из других экранов.
 */
internal class ProfileFocus(val screen: TvScreenFocus) {
    val settings: Map<ProfileSetting, FocusRequester> = ProfileSetting.entries.associateWith { FocusRequester() }
    val logout = FocusRequester()
}

/** Экран из состояния — без Koin и навигатора: так его собирают UI-тесты фокуса. */
@Composable
internal fun ProfileContent(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier,
    remoteCard: @Composable (Modifier) -> Unit = {},
) {
    val screenFocus = rememberTvScreenFocus(startAt = settingKey(ProfileSetting.QUALITY))
    val focus = remember(screenFocus) { ProfileFocus(screenFocus) }
    Box(modifier.fillMaxSize().tvSectionBackground()) {
        if (state.loading) {
            CircularProgressIndicator(color = LocalFxColors.current.accent, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        TvSheetHost(
            open = state.settingOpen,
            onClose = { onEvent(ProfileEvent.CloseSetting) },
            returnFocus = focus.settings.getValue(state.setting),
            sheet = { TvOptionSheet(optionSheet(state)) { index -> onEvent(ProfileEvent.ChooseOption(index)) } },
        ) {
            ProfileBody(state, onEvent, onCheckUpdates, focus, remoteCard)
        }
        TvConfirmDialog(
            spec = LogoutSpec.takeIf { state.confirmLogout },
            onConfirm = { onEvent(ProfileEvent.Logout) },
            onDismiss = { onEvent(ProfileEvent.DismissLogout) },
            returnFocus = focus.logout,
        )
    }
}

private val LogoutSpec = TvConfirmSpec(
    title = "Выйти из аккаунта?",
    message = "Чтобы вернуться, телевизор придётся снова привязать кодом на сайте kino.pub.",
    confirmLabel = "Выйти",
)

/** Панель вариантов открытой настройки в терминах дизайн-системы; у палитры — кружки её цветов. */
private fun optionSheet(state: ProfileState): TvOptionSheetState {
    val palette = state.setting == ProfileSetting.PALETTE
    val swatches = if (palette) ThemePalette.entries.map { it.toFxColors().accent } else null
    return TvOptionSheetState(
        overline = state.setting.group,
        title = state.setting.label,
        options = state.options.mapIndexed { index, label -> TvOption(label, swatches?.getOrNull(index)) },
        selectedIndex = state.selectedOption,
    )
}
