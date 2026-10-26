package com.filmax.feature.profile.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.serverLocationLabel
import com.filmax.core.domain.user.model.streamingTypeLabel
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvError
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.tvFocusOutline
import com.filmax.feature.profile.common.DeviceSettingsEvent
import com.filmax.feature.profile.common.DeviceSettingsScreenModel
import com.filmax.feature.profile.common.DeviceSettingsState
import org.koin.androidx.compose.koinViewModel

/** Ширина колонки настроек: строку во весь экран с 3 метров читать невозможно. */
private val ContentMaxWidth = 640.dp
private val ContentTop = TvMetrics.ContentTop + 18.dp
private val RowHeight = 60.dp
private val RowGap = 10.dp

/**
 * TV-экран «Настройки устройства». Тумблеры (SSL/HEVC/HDR/4K/смешанный плейлист) переключаются
 * кликом, тип потока циклически меняется по кругу — как настройки воспроизведения в TV-Профиле.
 * Правки локальны, на сервер уходят по кнопке «Сохранить»; после успеха модель закрывает экран.
 *
 * Сервер раздачи — справочная строка без выбора: список локаций API не отдаёт (см. serverLocationLabel).
 */
@Composable
fun TvDeviceSettingsScreen(modifier: Modifier = Modifier, screenModel: DeviceSettingsScreenModel = koinViewModel()) {
    val state by screenModel.collectAsState()

    if (state.loading) {
        Box(modifier.fillMaxSize().background(TvSurface), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TvOnSurface)
        }
        return
    }
    DeviceSettingsContent(state = state, onEvent = screenModel::dispatch, modifier = modifier)
}

// ── Контент ────────────────────────────────────────────────────────────────

@Composable
private fun DeviceSettingsContent(
    state: DeviceSettingsState,
    onEvent: (DeviceSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings ?: return
    val firstRow = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    ScrollToTopOnNavFocus(scrollState)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface)
            .verticalScroll(scrollState)
            .padding(
                start = TvMetrics.SafeHorizontal,
                end = TvMetrics.SafeHorizontal,
                top = ContentTop,
                bottom = TvMetrics.SafeVertical,
            ),
    ) {
        Column(Modifier.widthIn(max = ContentMaxWidth)) {
            DeviceSettingsHeader(title = settings.title)
            Spacer(Modifier.height(28.dp))
            TvOverline("Возможности", accent = false)
            Spacer(Modifier.height(12.dp))
            ToggleRows(settings = settings, onEvent = onEvent, firstRow = firstRow)
            Spacer(Modifier.height(26.dp))
            TvOverline("Поток", accent = false)
            Spacer(Modifier.height(12.dp))
            StreamRows(settings = settings, onEvent = onEvent)
            Spacer(Modifier.height(28.dp))
            SaveRow(saving = state.saving, error = state.error, onSave = { onEvent(DeviceSettingsEvent.Save) })
        }
    }

    // Стартовый фокус на первой строке: экран пушевой, таб-бар focus сюда не заводит.
    LaunchedEffect(Unit) { runCatching { firstRow.requestFocus() } }
}

@Composable
private fun DeviceSettingsHeader(title: String) {
    Column {
        Text(
            "Настройки устройства",
            style = MaterialTheme.typography.headlineMedium,
            color = TvOnSurface,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = TvOnSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun ToggleRows(settings: DeviceSettings, onEvent: (DeviceSettingsEvent) -> Unit, firstRow: FocusRequester) {
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        SettingRow(
            "SSL-соединение",
            onOff(settings.supportSsl),
            { onEvent(DeviceSettingsEvent.SetSsl(!settings.supportSsl)) },
            firstRow,
        )
        SettingRow(
            "HEVC (H.265)",
            onOff(settings.supportHevc),
            { onEvent(DeviceSettingsEvent.SetHevc(!settings.supportHevc)) },
        )
        SettingRow("HDR", onOff(settings.supportHdr), { onEvent(DeviceSettingsEvent.SetHdr(!settings.supportHdr)) })
        SettingRow("4K", onOff(settings.support4k), { onEvent(DeviceSettingsEvent.Set4k(!settings.support4k)) })
        SettingRow(
            "Смешанный плейлист",
            onOff(settings.mixedPlaylist),
            { onEvent(DeviceSettingsEvent.SetMixedPlaylist(!settings.mixedPlaylist)) },
        )
    }
}

@Composable
private fun StreamRows(settings: DeviceSettings, onEvent: (DeviceSettingsEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        SettingRow(
            "Тип потока",
            streamingTypeLabel(settings.streamingType),
            { onEvent(DeviceSettingsEvent.CycleStreamingType) },
        )
        // Сервер раздачи — справочная строка: список локаций API не отдаёт, менять не из чего.
        SettingRow("Сервер раздачи", serverLocationLabel(settings.serverLocation), null)
    }
}

@Composable
private fun SaveRow(saving: Boolean, error: String?, onSave: () -> Unit) {
    Column {
        TvButton(
            text = if (saving) "Сохранение…" else "Сохранить",
            onClick = { if (!saving) onSave() },
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, style = MaterialTheme.typography.bodyLarge, color = TvError)
        }
    }
}

// ── Строка настройки ─────────────────────────────────────────────────────────

/**
 * Строка настройки: слева ярлык, справа значение. Фокус рисуем вручную (рамка + подъём фона),
 * а не через `TvFocusCard`: `verticalScroll` клипает контент по горизонтали, и масштаб 1.08 на
 * строке 640dp вылезал бы за края.
 */
@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = TvMetrics.PanelShape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(
                if (onClick != null) {
                    Modifier
                        .onFocusChanged { focused = it.isFocused }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .background(if (focused) TvSurfaceContainerHigh else TvSurfaceContainer, shape)
            .tvFocusOutline(focused, shape)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TvOnSurface)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = TvOnSurfaceVariant, maxLines = 1)
    }
}

private fun onOff(enabled: Boolean): String = if (enabled) "Вкл" else "Выкл"
