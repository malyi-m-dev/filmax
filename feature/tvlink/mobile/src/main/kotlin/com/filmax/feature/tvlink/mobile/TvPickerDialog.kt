package com.filmax.feature.tvlink.mobile

import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeSidePanel
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonStyle
import com.filmax.core.ui.components.FilmaxListRow
import com.filmax.core.ui.components.FilmaxTextField
import com.filmax.feature.tvlink.common.TvPickerEvent
import com.filmax.feature.tvlink.common.TvPickerScreenModel
import com.filmax.feature.tvlink.common.TvPickerState
import org.koin.androidx.compose.koinViewModel

/**
 * Окно «Смотреть на телевизоре» поверх деталей или плеера: свои ТВ в сети и ввод адреса вручную.
 * Выбрали ТВ — модель уступает место пульту; «Назад» с пульта ведёт туда, откуда пришли, а не сюда.
 */
@Composable
fun TvPickerDialog(screenModel: TvPickerScreenModel = koinViewModel()) {
    val state by screenModel.collectAsState()
    // Окно диалога по умолчанию сдвигается за клавиатурой целиком, и всё, что ниже поля адреса
    // (ошибка, «Подключиться»), оставалось под ней. Ужатое окно прокручивается к нужному блоку.
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) }
    Surface(
        shape = ShapeSidePanel,
        color = FilmaxTheme.palette.background,
        contentColor = FilmaxColors.Text,
        border = BorderStroke(1.dp, FilmaxColors.PanelBorder),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(state.title, style = FilmaxType.sheetTitle)
            Text(
                "Телевизоры с Filmax вашего аккаунта kino.pub в этой Wi-Fi сети",
                style = FilmaxType.meta,
                color = FilmaxColors.TextSecondary,
            )
            Devices(state, screenModel::dispatch)
            state.failure?.let { failure -> ErrorText(failure.message) }
            ManualAddress(state, screenModel::dispatch)
            FilmaxButton(
                text = "Отмена",
                onClick = { screenModel.dispatch(TvPickerEvent.Dismiss) },
                style = FilmaxButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Devices(state: TvPickerState, onEvent: (TvPickerEvent) -> Unit) {
    if (state.devices.isEmpty()) {
        Searching(explain = state.searchTakesLong)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.devices.forEach { device ->
            FilmaxListRow(
                title = device.name,
                subtitle = if (device.id == state.connectedTo) "Подключён" else device.host,
                icon = Icons.Filled.Tv,
                busy = device.id == state.connectingTo,
                onClick = { onEvent(TvPickerEvent.Choose(device.id)) },
            )
        }
    }
}

@Composable
private fun Searching(explain: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            color = FilmaxTheme.palette.accent,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp),
        )
        Text("Ищем телевизор…", style = FilmaxType.bodySmall, color = FilmaxColors.Text)
    }
    if (explain) {
        Text(
            "Не находится? Откройте Filmax на телевизоре: он должен быть включён, в той же Wi-Fi сети " +
                "и в том же аккаунте kino.pub. Если роутер прячет устройства друг от друга, " +
                "подключитесь по адресу.",
            style = FilmaxType.meta,
            color = FilmaxColors.TextSecondary,
        )
    }
}

/** Адрес вручную — для сетей, где роутер режет поиск (гостевой Wi-Fi, «изоляция клиентов»). */
@Composable
private fun ManualAddress(state: TvPickerState, onEvent: (TvPickerEvent) -> Unit) {
    if (!state.manualOpen) {
        FilmaxButton(
            text = "Ввести адрес вручную",
            onClick = { onEvent(TvPickerEvent.OpenManual) },
            style = FilmaxButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    Text(
        "Адрес — на телевизоре: Профиль → «Пульт с телефона»",
        style = FilmaxType.meta,
        color = FilmaxColors.TextSecondary,
    )
    AddressInput(state, onEvent)
}

/**
 * Поле, ошибка и «Подключиться» — один блок, и над клавиатурой он поднимается целиком: окно само
 * прокручивает только до поля, и ошибка с кнопкой оставались под клавиатурой.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddressInput(state: TvPickerState, onEvent: (TvPickerEvent) -> Unit) {
    val block = remember { BringIntoViewRequester() }
    val keyboardShown = WindowInsets.isImeVisible
    LaunchedEffect(keyboardShown, state.addressInvalid) {
        if (keyboardShown || state.addressInvalid) block.bringIntoView()
    }
    Column(Modifier.bringIntoViewRequester(block), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FilmaxTextField(
            value = state.address,
            onValueChange = { onEvent(TvPickerEvent.EditAddress(it)) },
            placeholder = "Например, 192.168.1.50",
            onDone = { onEvent(TvPickerEvent.ConnectByAddress) },
            isError = state.addressInvalid,
            keyboardType = KeyboardType.Uri,
        )
        if (state.addressInvalid) ErrorText("Это не похоже на адрес — он выглядит как 192.168.1.50")
        FilmaxButton(
            text = if (state.connectingTo != null) "Подключаемся…" else "Подключиться",
            onClick = { onEvent(TvPickerEvent.ConnectByAddress) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = FilmaxType.meta, color = FilmaxColors.Error)
}
