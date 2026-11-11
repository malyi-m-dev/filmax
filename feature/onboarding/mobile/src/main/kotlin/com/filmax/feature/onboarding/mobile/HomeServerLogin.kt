// Вход на домашний сервер Jellyfin с телефона: сервер в сети или адрес, затем имя и пароль (или
// код Quick Connect). Логика шагов — в общем HomeServerScreenModel, здесь только вёрстка.
package com.filmax.feature.onboarding.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxSettingsDivider
import com.filmax.core.ui.components.FilmaxSettingsGroup
import com.filmax.core.ui.components.FilmaxSettingsRow
import com.filmax.core.ui.components.FilmaxTextButton
import com.filmax.core.ui.components.FilmaxTextField
import com.filmax.feature.onboarding.common.HomeServerEvent
import com.filmax.feature.onboarding.common.HomeServerScreenModel
import com.filmax.feature.onboarding.common.HomeServerSideEffect
import com.filmax.feature.onboarding.common.HomeServerState
import com.filmax.feature.onboarding.common.HomeServerStep
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Отступ шапки шага от верха: экран прокручивается, клавиатура поднимает поля над собой. */
private val TopPadding = 48.dp

/**
 * Вход на домашний сервер. «Назад» — шаг назад: со входа к выбору сервера, с выбора сервера —
 * к приветствию kino.pub ([onBack]).
 */
@Composable
internal fun HomeServerLogin(
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
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SidePadding)
            .padding(top = TopPadding, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Wordmark()
        StepHeader(state)
        when (state.step) {
            HomeServerStep.SERVER -> ServerChoice(state, dispatch, onBack)
            HomeServerStep.CODE -> CodeLogin(state, dispatch)
            HomeServerStep.PASSWORD -> PasswordLogin(state, dispatch)
        }
        state.error?.let { Text(it, style = FilmaxType.body, color = FilmaxColors.Error) }
    }
}

@Composable
private fun StepHeader(state: HomeServerState) {
    val server = state.server
    val title = if (state.step == HomeServerStep.SERVER) "Домашний сервер" else server?.name ?: "Сервер"
    val hint = when (state.step) {
        HomeServerStep.SERVER -> "Jellyfin в вашей сети: фильмы и сериалы из своей медиатеки, без подписки."
        HomeServerStep.CODE -> "Откройте Jellyfin на другом устройстве: Профиль → Quick Connect — и введите код."
        HomeServerStep.PASSWORD -> "Пользователь Jellyfin на этом сервере."
    }
    Text(title, style = FilmaxType.collectionTitle, color = FilmaxColors.Text, modifier = Modifier.padding(top = 6.dp))
    Text(hint, style = FilmaxType.body, color = FilmaxColors.TextSecondary)
}

@Composable
private fun ServerChoice(state: HomeServerState, dispatch: (HomeServerEvent) -> Unit, onBack: () -> Unit) {
    if (state.found.isNotEmpty()) {
        FilmaxSettingsGroup(title = "В этой сети") {
            state.found.forEachIndexed { index, server ->
                if (index > 0) FilmaxSettingsDivider()
                FilmaxSettingsRow(
                    icon = Icons.Filled.Dns,
                    label = server.name,
                    value = server.address.substringAfter("://"),
                    onClick = { dispatch(HomeServerEvent.Choose(server)) },
                )
            }
        }
    }
    FilmaxTextField(
        value = state.address,
        onValueChange = { dispatch(HomeServerEvent.SetAddress(it)) },
        placeholder = "192.168.1.20:8096",
        onDone = { dispatch(HomeServerEvent.Connect) },
        label = "Адрес сервера",
        keyboardType = KeyboardType.Uri,
    )
    FilmaxButton(
        text = if (state.busy) "Подключаемся…" else "Подключиться",
        onClick = { dispatch(HomeServerEvent.Connect) },
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    )
    FilmaxTextButton(
        text = if (state.searching) "Ищем серверы…" else "Искать в сети снова",
        onClick = { dispatch(HomeServerEvent.Search) },
    )
    FilmaxTextButton(text = "Войти в kino.pub", onClick = onBack)
}

@Composable
private fun CodeLogin(state: HomeServerState, dispatch: (HomeServerEvent) -> Unit) {
    val code = state.code
    if (code == null) {
        Text("Запрашиваем код…", style = FilmaxType.body, color = FilmaxColors.TextSecondary)
    } else {
        CodeBox(userCode = code)
        PollingStatus()
    }
    FilmaxTextButton(text = "Войти по паролю", onClick = { dispatch(HomeServerEvent.UsePassword) })
    FilmaxTextButton(text = "Другой сервер", onClick = { dispatch(HomeServerEvent.ChangeServer) })
}

@Composable
private fun PasswordLogin(state: HomeServerState, dispatch: (HomeServerEvent) -> Unit) {
    FilmaxTextField(
        value = state.username,
        onValueChange = { dispatch(HomeServerEvent.SetUsername(it)) },
        placeholder = "Имя пользователя",
        onDone = {},
        label = "Пользователь",
    )
    FilmaxTextField(
        value = state.password,
        onValueChange = { dispatch(HomeServerEvent.SetPassword(it)) },
        placeholder = "Пароль",
        onDone = { dispatch(HomeServerEvent.SignIn) },
        label = "Пароль",
        secret = true,
    )
    FilmaxButton(
        text = if (state.busy) "Входим…" else "Войти",
        onClick = { dispatch(HomeServerEvent.SignIn) },
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.server?.quickConnect == true) {
        FilmaxTextButton(text = "Войти по коду Quick Connect", onClick = { dispatch(HomeServerEvent.UseCode) })
    }
    FilmaxTextButton(text = "Другой сервер", onClick = { dispatch(HomeServerEvent.ChangeServer) })
}
