package com.filmax.feature.profile.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.domain.user.model.DeviceOption
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.serverLocationLabel
import com.filmax.core.domain.user.model.streamingTypeLabel
import com.filmax.core.domain.user.model.streamingTypeOptions
import com.filmax.feature.profile.common.DeviceSettingsEvent
import com.filmax.feature.profile.common.DeviceSettingsScreenModel
import com.filmax.feature.profile.common.DeviceSettingsSideEffect
import com.filmax.feature.profile.common.DeviceSettingsState
import org.koin.androidx.compose.koinViewModel

/**
 * «Настройки устройства» — тумблеры возможностей (SSL/HEVC/HDR/4K/смешанный плейлист) и выбор
 * типа потока. Правки локальны, на сервер уходят по «Сохранить» ([DeviceSettingsScreenModel]).
 * После успеха экран закрывается (side-effect [DeviceSettingsSideEffect.Saved] → [onBack]).
 *
 * Сервер раздачи — справочная строка без выбора: API отдаёт только текущий id локации, без списка
 * доступных, поэтому предлагать выбор не из чего (см. `serverLocationLabel`).
 */
@Composable
fun DeviceSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: DeviceSettingsScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    screenModel.collectSideEffect { effect ->
        when (effect) {
            DeviceSettingsSideEffect.Saved -> onBack()
        }
    }

    if (state.loading) {
        LoadingBox(modifier)
        return
    }

    var streamingSheet by remember { mutableStateOf(false) }

    DeviceSettingsContent(
        state = state,
        actions = DeviceSettingsActions(
            toggles = deviceToggles(screenModel),
            onOpenStreamingSheet = { streamingSheet = true },
            onSave = { screenModel.dispatch(DeviceSettingsEvent.Save) },
            onBack = onBack,
        ),
        modifier = modifier,
    )

    if (streamingSheet) {
        StreamingTypeSheet(
            selected = state.settings?.streamingType ?: 0,
            onSelect = {
                screenModel.dispatch(DeviceSettingsEvent.SetStreamingType(it))
                streamingSheet = false
            },
            onDismiss = { streamingSheet = false },
        )
    }
}

@Composable
private fun LoadingBox(modifier: Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

// ── Контент ────────────────────────────────────────────────────────────────

/** Тумблеры возможностей устройства. Отдельная модель — чтобы не раздувать список параметров. */
private data class DeviceToggles(
    val onSsl: (Boolean) -> Unit,
    val onHevc: (Boolean) -> Unit,
    val onHdr: (Boolean) -> Unit,
    val on4k: (Boolean) -> Unit,
    val onMixedPlaylist: (Boolean) -> Unit,
)

private data class DeviceSettingsActions(
    val toggles: DeviceToggles,
    val onOpenStreamingSheet: () -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit,
)

private fun deviceToggles(screenModel: DeviceSettingsScreenModel) = DeviceToggles(
    onSsl = { screenModel.dispatch(DeviceSettingsEvent.SetSsl(it)) },
    onHevc = { screenModel.dispatch(DeviceSettingsEvent.SetHevc(it)) },
    onHdr = { screenModel.dispatch(DeviceSettingsEvent.SetHdr(it)) },
    on4k = { screenModel.dispatch(DeviceSettingsEvent.Set4k(it)) },
    onMixedPlaylist = { screenModel.dispatch(DeviceSettingsEvent.SetMixedPlaylist(it)) },
)

@Composable
private fun DeviceSettingsContent(
    state: DeviceSettingsState,
    actions: DeviceSettingsActions,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FilmaxMetrics.ScreenPadding)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        DeviceSettingsHeader(title = settings.title, onBack = actions.onBack)

        SectionOverline("ВОЗМОЖНОСТИ")
        ToggleGroup(settings = settings, toggles = actions.toggles)

        SectionOverline("ПОТОК")
        StreamGroup(settings = settings, onOpenStreamingSheet = actions.onOpenStreamingSheet)

        Spacer(Modifier.height(24.dp))
        SaveButton(saving = state.saving, onSave = actions.onSave)
        // Локальная копия: smart-cast nullable-поля из другого модуля тут невозможен.
        val error = state.error
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun DeviceSettingsHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .size(FilmaxMetrics.BackButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                "Настройки устройства",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Надзаголовок группы: в монохроме роль секции несёт кегль и трекинг, а не цветная плашка. */
@Composable
private fun SectionOverline(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = FilmaxOnSurfaceDim,
        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
    )
}

@Composable
private fun ToggleGroup(settings: DeviceSettings, toggles: DeviceToggles) {
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        ToggleRow("SSL-соединение", settings.supportSsl, toggles.onSsl)
        ToggleRow("HEVC (H.265)", settings.supportHevc, toggles.onHevc)
        ToggleRow("HDR", settings.supportHdr, toggles.onHdr)
        ToggleRow("4K", settings.support4k, toggles.on4k)
        ToggleRow("Смешанный плейлист", settings.mixedPlaylist, toggles.onMixedPlaylist)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.SettingsRowHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onCheckedChange(!checked) }
            .padding(start = 18.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StreamGroup(settings: DeviceSettings, onOpenStreamingSheet: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        NavigationRow(
            label = "Тип потока",
            value = streamingTypeLabel(settings.streamingType),
            onClick = onOpenStreamingSheet,
        )
        // Сервер раздачи — справочная строка без шеврона: список локаций API не отдаёт.
        NavigationRow(
            label = "Сервер раздачи",
            value = serverLocationLabel(settings.serverLocation),
            onClick = null,
        )
    }
}

@Composable
private fun NavigationRow(label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.SettingsRowHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = FilmaxOnSurfaceDim, maxLines = 1)
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SaveButton(saving: Boolean, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.PrimaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(enabled = !saving, onClick = onSave),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (saving) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                "Сохранить",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

// ── Лист выбора типа потока ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamingTypeSheet(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Text(
                "Тип потока",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            DeviceOptionList(options = streamingTypeOptions, selected = selected, onSelect = onSelect)
        }
    }
}

/** Выбранную опцию отмечает галочка, а не радиокнопка: в монохроме отметка — это знак, не цвет. */
@Composable
private fun DeviceOptionList(options: List<DeviceOption>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FilmaxMetrics.SettingsRowHeight)
                    .clip(ShapeButton)
                    .clickable { onSelect(option.id) }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (option.id == selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Выбрано",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private val RowGap = 9.dp
