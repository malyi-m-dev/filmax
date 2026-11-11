// Вход на домашний сервер Jellyfin с пульта: слева шаг (сервер, код или пароль), справа
// экранная клавиатура — как в отзывах. Логика шагов — в общем HomeServerScreenModel.
package com.filmax.feature.onboarding.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.input.KeyboardLayout
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvButtonStyle
import com.filmax.core.tv.designsystem.TvKeyboard
import com.filmax.core.tv.designsystem.TvKeyboardActions
import com.filmax.core.tv.designsystem.TvKeyboardLayouts
import com.filmax.core.tv.designsystem.TvKeyboardStyle
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSettingRow
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.feature.onboarding.common.HomeServerEvent
import com.filmax.feature.onboarding.common.HomeServerField
import com.filmax.feature.onboarding.common.HomeServerScreenModel
import com.filmax.feature.onboarding.common.HomeServerSideEffect
import com.filmax.feature.onboarding.common.HomeServerState
import com.filmax.feature.onboarding.common.HomeServerStep
import com.filmax.feature.onboarding.common.ServerKey
import com.filmax.feature.onboarding.common.serverKeyboardRows
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Колонка шага: строка длиннее ~520dp заставляет глаз искать начало следующей. */
private val StepWidth = 520.dp

/** Клавиатура ЙЦУКЕН в одиннадцать колонок — по ширине её клавиш. */
private val KeyboardWidth = 440.dp
private val ColumnGap = 64.dp

/** Ключи стартового фокуса шагов. */
private const val SERVER_KEY = "server:"
private const val ADDRESS_KEY = "address"
private const val USERNAME_KEY = "username"
private const val PASSWORD_INSTEAD_KEY = "usePassword"

/**
 * Вход на домашний сервер. «Назад» — шаг назад: со входа к выбору сервера, с выбора сервера —
 * к приветствию kino.pub ([onBack]).
 */
@Composable
internal fun TvHomeServerStep(
    onBack: () -> Unit,
    screenModel: HomeServerScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val dispatch: (HomeServerEvent) -> Unit = screenModel::dispatch
    screenModel.collectSideEffect { effect ->
        when (effect) {
            HomeServerSideEffect.SignedIn -> navigator.root(Destination.Home)
        }
    }
    BackHandler {
        if (state.step == HomeServerStep.SERVER) onBack() else dispatch(HomeServerEvent.ChangeServer)
    }
    val focus = rememberTvScreenFocus(startAt = startKey(state))
    // Шаг сменился целиком — фокус на его первый элемент, иначе он повис бы на исчезнувшей кнопке и
    // пульт перестал бы отвечать. Адрес фокуса — здесь, в композиции, а не в LaunchedEffect: эффект
    // запускается уже после того, как новый шаг разместился, и элемент свою очередь пропустил бы.
    remember(state.step) { focus.focusOn(startKey(state)) }
    val keyboard = remember { FocusRequester() }

    Row(
        Modifier.fillMaxSize().padding(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(ColumnGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.width(StepWidth).then(focus.containerModifier),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Wordmark()
            StepHeader(state)
            val step = StepContext(state, dispatch, focus, keyboard)
            when (state.step) {
                HomeServerStep.SERVER -> ServerChoice(step, onBack)
                HomeServerStep.CODE -> CodeLogin(step)
                HomeServerStep.PASSWORD -> PasswordLogin(step)
            }
            state.error?.let { Text(it, style = FxType.body, color = LocalFxColors.current.error) }
        }
        // Коду клавиатура не нужна: вход подтверждают на другом устройстве.
        if (state.step != HomeServerStep.CODE) ServerKeyboard(state, dispatch, keyboard, Modifier.width(KeyboardWidth))
    }
}

/** Всё, что нужно шагу: состояние, события, фокус экрана и первая клавиша клавиатуры. */
private class StepContext(
    val state: HomeServerState,
    val dispatch: (HomeServerEvent) -> Unit,
    val focus: TvScreenFocus,
    val keyboard: FocusRequester,
)

private fun startKey(state: HomeServerState): String = when (state.step) {
    HomeServerStep.SERVER -> if (state.found.isEmpty()) ADDRESS_KEY else "${SERVER_KEY}0"
    HomeServerStep.CODE -> PASSWORD_INSTEAD_KEY
    HomeServerStep.PASSWORD -> USERNAME_KEY
}

@Composable
private fun StepHeader(state: HomeServerState) {
    val colors = LocalFxColors.current
    val server = state.server
    val title = if (state.step == HomeServerStep.SERVER) "Домашний сервер" else server?.name ?: "Сервер"
    val hint = when (state.step) {
        HomeServerStep.SERVER -> "Jellyfin в этой сети: фильмы и сериалы из своей медиатеки, без подписки."
        HomeServerStep.CODE -> "Откройте Jellyfin на телефоне или в браузере: Профиль → Quick Connect — и введите код."
        HomeServerStep.PASSWORD -> "Пользователь Jellyfin на этом сервере."
    }
    Text(title, style = FxType.screenTitle, color = colors.text, modifier = Modifier.padding(top = 14.dp))
    Text(hint, style = FxType.body, color = colors.textSecondary)
}

/** Найденные серверы — одним «ОК»; не нашёлся — адрес набирают клавиатурой справа. */
@Composable
private fun ServerChoice(step: StepContext, onBack: () -> Unit) {
    val state = step.state
    Column(Modifier.tvFocusGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.found.forEachIndexed { index, server ->
            TvSettingRow(
                label = server.name,
                value = server.address.substringAfter("://"),
                onClick = { step.dispatch(HomeServerEvent.Choose(server)) },
                modifier = step.focus.item("$SERVER_KEY$index"),
            )
        }
        InputRow(step, HomeServerField.ADDRESS, "Адрес сервера", state.address, ADDRESS_KEY)
    }
    Row(Modifier.tvFocusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvButton(
            text = if (state.busy) "Подключаемся…" else "Подключиться",
            onClick = { step.dispatch(HomeServerEvent.Connect) },
        )
        TvButton(
            text = if (state.searching) "Ищем…" else "Искать снова",
            onClick = { step.dispatch(HomeServerEvent.Search) },
            style = TvButtonStyle.Secondary,
        )
        TvButton(text = "kino.pub", onClick = onBack, style = TvButtonStyle.Secondary)
    }
}

@Composable
private fun CodeLogin(step: StepContext) {
    val code = step.state.code
    if (code == null) {
        Text("Запрашиваем код…", style = FxType.body, color = LocalFxColors.current.textSecondary)
    } else {
        ActivationCode(code)
        PollingStatus()
    }
    Row(Modifier.tvFocusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvButton(
            text = "Войти по паролю",
            onClick = { step.dispatch(HomeServerEvent.UsePassword) },
            modifier = step.focus.item(PASSWORD_INSTEAD_KEY),
        )
        TvButton(
            text = "Другой сервер",
            onClick = { step.dispatch(HomeServerEvent.ChangeServer) },
            style = TvButtonStyle.Secondary,
        )
    }
}

@Composable
private fun PasswordLogin(step: StepContext) {
    val state = step.state
    Column(Modifier.tvFocusGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InputRow(step, HomeServerField.USERNAME, "Пользователь", state.username, USERNAME_KEY)
        // Пароль — точками: экран видят все, кто в комнате.
        InputRow(step, HomeServerField.PASSWORD, "Пароль", "•".repeat(state.password.length), "password")
    }
    Row(Modifier.tvFocusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvButton(text = if (state.busy) "Входим…" else "Войти", onClick = { step.dispatch(HomeServerEvent.SignIn) })
        if (state.server?.quickConnect == true) {
            TvButton(
                text = "Войти по коду",
                onClick = { step.dispatch(HomeServerEvent.UseCode) },
                style = TvButtonStyle.Secondary,
            )
        }
        TvButton(
            text = "Другой сервер",
            onClick = { step.dispatch(HomeServerEvent.ChangeServer) },
            style = TvButtonStyle.Secondary,
        )
    }
}

/**
 * Поле ввода — строка настройки: значение набирают клавиатурой справа. Фокус на строке и
 * выбирает поле, в которое та печатает, — переходить на клавиатуру можно сразу «вправо». Куда
 * «вправо» — задано явно: по геометрии ближе всех к нижним полям переключатель раскладок под
 * клавиатурой, и «ОК» там менял бы раскладку вместо набора. Выбранное поле отмечено кареткой.
 */
@Composable
private fun InputRow(step: StepContext, field: HomeServerField, label: String, value: String, key: String) {
    val active = step.state.field == field
    TvSettingRow(
        label = label,
        value = if (active) "$value▏" else value.ifEmpty { "—" },
        onClick = { step.dispatch(HomeServerEvent.Focus(field)) },
        modifier = step.focus.item(key)
            .focusProperties { right = step.keyboard }
            .onFocusChanged { state -> if (state.isFocused) step.dispatch(HomeServerEvent.Focus(field)) },
    )
}

@Composable
private fun ServerKeyboard(
    state: HomeServerState,
    dispatch: (HomeServerEvent) -> Unit,
    firstKey: FocusRequester,
    modifier: Modifier,
) {
    val key = { input: ServerKey -> dispatch(HomeServerEvent.Key(input)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TvKeyboard(
            rows = serverKeyboardRows(state.layout),
            actions = TvKeyboardActions(
                onKey = { key(ServerKey.Letter(it)) },
                onSpace = { key(ServerKey.Space) },
                onDelete = { key(ServerKey.Erase) },
            ),
            style = TvKeyboardStyle.Panel,
            modifier = Modifier.fillMaxWidth(),
            firstKey = firstKey,
        )
        TvKeyboardLayouts(
            labels = KeyboardLayout.entries.map { it.label },
            selected = state.layout.ordinal,
            onSelect = { key(ServerKey.Layout(KeyboardLayout.entries[it])) },
        )
    }
}
