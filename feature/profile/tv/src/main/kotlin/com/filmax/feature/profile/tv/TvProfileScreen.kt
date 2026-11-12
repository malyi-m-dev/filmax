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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvConfirmDialog
import com.filmax.core.tv.designsystem.TvConfirmSpec
import com.filmax.core.tv.designsystem.TvOption
import com.filmax.core.tv.designsystem.TvOptionSheet
import com.filmax.core.tv.designsystem.TvOptionSheetState
import com.filmax.core.tv.designsystem.TvSheetHost
import com.filmax.core.tv.designsystem.preview.TvPreviews
import com.filmax.core.tv.designsystem.tvSectionBackground
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileSamples
import com.filmax.feature.profile.common.ProfileScreenModel
import com.filmax.feature.profile.common.ProfileSetting
import com.filmax.feature.profile.common.ProfileSideEffect
import com.filmax.feature.profile.common.ProfileState
import com.filmax.feature.profile.common.optionsOf
import com.filmax.feature.profile.common.valueOf
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * ТВ-Профиль (экран 09): шапка аккаунта, настройки строками, карточка подписки справа. «ОК» на
 * строке со значением открывает боковую панель с вариантами, «Выйти из аккаунта» спрашивает
 * подтверждение с фокусом на «Отмена». Палитра применяется ко всему ТВ-UI сразу. Логика —
 * в общем с телефоном [ProfileScreenModel], экран рисует и отдаёт события.
 *
 * Блока «Настройки устройства» здесь нет намеренно: device/info и device/settings отвечают 500.
 *
 * [remoteCard] — карточка «Пульт с телефона» под подпиской. Её даёт граф (фича связи с телефоном),
 * а не этот экран: фичи друг от друга не зависят.
 */
@Composable
fun TvProfileScreen(
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier,
    remoteCard: @Composable (Modifier) -> Unit = {},
    screenModel: ProfileScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            ProfileSideEffect.LoggedOut -> navigator.root(Destination.Onboarding)
        }
    }
    val actions = remember(screenModel, navigator, onCheckUpdates) {
        ProfileActions(screenModel::dispatch, onCheckUpdates, onRate = {
            navigator.open(Destination.Feedback(FeedbackTrigger.Profile.name))
        })
    }
    TvProfileScreen(state = state, actions = actions, modifier = modifier, remoteCard = remoteCard)
}

/** Действия экрана: события модели, ручная проверка обновлений (её модель живёт в :app) и оценка. */
internal data class ProfileActions(
    val onEvent: (ProfileEvent) -> Unit,
    val onCheckUpdates: () -> Unit,
    /** Окно оценки приложения. */
    val onRate: () -> Unit = {},
) {
    companion object {
        /** Ничего не делает — для превью. */
        val None = ProfileActions(onEvent = {}, onCheckUpdates = {})
    }
}

/**
 * Куда вернуть фокус: на строку настройки, чья панель закрылась, и на «Выйти», когда закрылся
 * диалог.
 */
internal class ProfileFocus {
    val settings: Map<ProfileSetting, FocusRequester> = ProfileSetting.entries.associateWith { FocusRequester() }
    val logout = FocusRequester()
}

/** Профиль из состояния — без Koin и навигатора: его рисуют превью и UI-тесты фокуса. */
@Composable
internal fun TvProfileScreen(
    state: ProfileState,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
    remoteCard: @Composable (Modifier) -> Unit = {},
) {
    val focus = remember { ProfileFocus() }
    Box(modifier.fillMaxSize().tvSectionBackground()) {
        if (state.loading) {
            CircularProgressIndicator(color = LocalFxColors.current.accent, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        TvSheetHost(
            open = state.settingOpen,
            onClose = { actions.onEvent(ProfileEvent.CloseSetting) },
            returnFocus = focus.settings.getValue(state.setting),
            sheet = {
                TvOptionSheet(optionSheet(state)) { index -> actions.onEvent(ProfileEvent.ChooseOption(index)) }
            },
        ) {
            ProfileBody(state, actions, focus, remoteCard)
        }
        TvConfirmDialog(
            spec = LogoutSpec.takeIf { state.confirmLogout },
            onConfirm = { actions.onEvent(ProfileEvent.Logout) },
            onDismiss = { actions.onEvent(ProfileEvent.DismissLogout) },
            returnFocus = focus.logout,
        )
    }
}

private val LogoutSpec = TvConfirmSpec(
    title = "Выйти из аккаунта?",
    message = "Чтобы вернуться, телевизор придётся снова привязать кодом на сайте kino.pub.",
    confirmLabel = "Выйти",
)

/** Панель вариантов открытой настройки; у палитры — кружки её цветов. */
private fun optionSheet(state: ProfileState): TvOptionSheetState {
    val setting = state.setting
    val labels = optionsOf(setting)
    val swatches = if (setting == ProfileSetting.PALETTE) ThemePalette.entries.map { it.toFxColors().accent } else null
    return TvOptionSheetState(
        overline = when (setting) {
            ProfileSetting.PALETTE -> "Оформление"
            ProfileSetting.SOURCE -> "Аккаунт"
            else -> "Просмотр"
        },
        title = setting.label,
        options = labels.mapIndexed { index, label -> TvOption(label, swatches?.getOrNull(index)) },
        selectedIndex = labels.indexOf(state.valueOf(setting)),
    )
}

private class ProfileStatePreviews : PreviewParameterProvider<ProfileState> {
    override val values = ProfileSamples.all.asSequence()
}

@TvPreviews
@Composable
private fun TvProfileScreenPreview(@PreviewParameter(ProfileStatePreviews::class) state: ProfileState) {
    FilmaxTvTheme { TvProfileScreen(state = state, actions = ProfileActions.None) }
}
