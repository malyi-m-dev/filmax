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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.ui.components.FilmaxVersionLabel
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileScreenModel
import com.filmax.feature.profile.common.ProfileSideEffect
import com.filmax.feature.profile.common.ProfileState
import com.filmax.feature.profile.common.initialsOrFallback
import com.filmax.feature.profile.common.label
import org.koin.androidx.compose.koinViewModel

/**
 * Профиль (экран 09 макета): шапка аккаунта и две группы строк — «Просмотр» и «Аккаунт».
 * Состав совпадает с TV-Профилем, меняется только раскладка; данные и события — общие
 * ([ProfileScreenModel]). Клик по строке настройки открывает лист выбора значения.
 *
 * Коллапсирующей шапки и карточек статистики здесь нет: экран из семи строк не даёт скролла,
 * ради которого шапка сворачивалась, а «просмотрено/в избранном» ни на что не влияли —
 * это украшение, которое оттягивало внимание от единственной задачи экрана.
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenDesignSystem: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    screenModel: ProfileScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    screenModel.collectSideEffect { effect ->
        when (effect) {
            ProfileSideEffect.LoggedOut -> onLogout()
        }
    }

    if (state.loading) {
        Box(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var activeSheet by remember { mutableStateOf<ProfileSheet?>(null) }

    ProfileContent(
        state = state,
        actions = ProfileActions(
            onOpenSheet = { sheet -> activeSheet = sheet },
            onOpenDesignSystem = onOpenDesignSystem,
            onLogout = { screenModel.dispatch(ProfileEvent.Logout) },
        ),
        modifier = modifier,
    )

    PlaybackSettingsSheets(
        activeSheet = activeSheet,
        playback = state.playback,
        onDismiss = { activeSheet = null },
        selection = PlaybackSelection(
            onSelectQuality = { screenModel.dispatch(ProfileEvent.SetQuality(it)) },
            onSelectAudio = { screenModel.dispatch(ProfileEvent.SetAudioLanguage(it)) },
            onSelectSubtitle = { screenModel.dispatch(ProfileEvent.SetSubtitleLanguage(it)) },
        ),
    )
}

/** Колбэки профиля одним объектом — иначе список параметров ProfileContent за порогом detekt. */
private data class ProfileActions(
    val onOpenSheet: (ProfileSheet) -> Unit,
    val onOpenDesignSystem: (() -> Unit)?,
    val onLogout: () -> Unit,
)

@Composable
private fun ProfileContent(
    state: ProfileState,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FilmaxMetrics.ScreenPadding)
            .padding(top = 14.dp, bottom = 24.dp),
    ) {
        ProfileHeader(profile = state.profile)

        SectionOverline("ПРОСМОТР")
        // Настройки воспроизведения реальны: качество/аудио/субтитры хранятся локально
        // (PlaybackSettingsRepository) и применяются в плеере. Заглушек на экране нет.
        PlaybackRows(playback = state.playback, onOpenSheet = actions.onOpenSheet)

        // Блока «УСТРОЙСТВО» временно нет: device/info и device/settings отвечают 500,
        // и строка вела на нерабочий экран. Вернуть, когда бэкенд починят.
        SectionOverline("АККАУНТ")
        AccountRows(state = state, onLogout = actions.onLogout)

        val onOpenDesignSystem = actions.onOpenDesignSystem
        if (onOpenDesignSystem != null) {
            SectionOverline("РАЗРАБОТЧИКАМ")
            SettingRow(
                spec = SettingRowSpec(label = "Дизайн-система", value = "Каталог компонентов"),
                onClick = onOpenDesignSystem,
            )
        }

        FilmaxVersionLabel(
            color = FilmaxOnSurfaceDim,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 32.dp),
        )
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profile.initialsOrFallback(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                profile?.username ?: "Гость",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                profile?.subscription.label(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
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
private fun PlaybackRows(playback: PlaybackSettings, onOpenSheet: (ProfileSheet) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        SettingRow(
            spec = SettingRowSpec(label = "Качество видео", value = playback.quality),
            onClick = { onOpenSheet(ProfileSheet.QUALITY) },
        )
        SettingRow(
            spec = SettingRowSpec(label = "Язык аудио", value = playback.audioLanguage),
            onClick = { onOpenSheet(ProfileSheet.AUDIO) },
        )
        SettingRow(
            spec = SettingRowSpec(label = "Субтитры", value = playback.subtitleLanguage),
            onClick = { onOpenSheet(ProfileSheet.SUBTITLES) },
        )
    }
}

@Composable
private fun AccountRows(state: ProfileState, onLogout: () -> Unit) {
    val active = state.profile?.subscription?.active == true
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        // Подписка — справочная строка: менять её из приложения нельзя, поэтому без шеврона.
        SettingRow(
            spec = SettingRowSpec(label = "Подписка", value = if (active) "Premium" else "Неактивна"),
            onClick = null,
        )
        SettingRow(
            spec = SettingRowSpec(label = "Выйти", labelColor = MaterialTheme.colorScheme.error),
            onClick = onLogout,
        )
    }
}

// ── Строка настройки ──────────────────────────────────────────────────────

/** [labelColor] = null — обычный текст: цвет темы в дефолте data-класса недоступен. */
private data class SettingRowSpec(
    val label: String,
    val value: String? = null,
    val labelColor: Color? = null,
)

@Composable
private fun SettingRow(spec: SettingRowSpec, onClick: (() -> Unit)?) {
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
            spec.label,
            style = MaterialTheme.typography.titleMedium,
            color = spec.labelColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!spec.value.isNullOrBlank()) {
            Text(
                spec.value,
                style = MaterialTheme.typography.bodyMedium,
                color = FilmaxOnSurfaceDim,
                maxLines = 1,
            )
        }
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

// ── Листы выбора настроек воспроизведения ─────────────────────────────────

private enum class ProfileSheet(val title: String) {
    QUALITY("Качество видео"),
    AUDIO("Язык аудио"),
    SUBTITLES("Субтитры"),
}

/** Колбэки выбора настроек воспроизведения: качество, аудио и субтитры. */
private data class PlaybackSelection(
    val onSelectQuality: (String) -> Unit,
    val onSelectAudio: (String) -> Unit,
    val onSelectSubtitle: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSettingsSheets(
    activeSheet: ProfileSheet?,
    playback: PlaybackSettings,
    onDismiss: () -> Unit,
    selection: PlaybackSelection,
) {
    if (activeSheet == null) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Text(
                activeSheet.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            when (activeSheet) {
                ProfileSheet.QUALITY -> OptionList(
                    options = PlaybackSettings.qualityOptions,
                    selected = playback.quality,
                    onSelect = selection.onSelectQuality,
                )

                ProfileSheet.AUDIO -> OptionList(
                    options = PlaybackSettings.audioOptions,
                    selected = playback.audioLanguage,
                    onSelect = selection.onSelectAudio,
                )

                ProfileSheet.SUBTITLES -> OptionList(
                    options = PlaybackSettings.subtitleOptions,
                    selected = playback.subtitleLanguage,
                    onSelect = selection.onSelectSubtitle,
                )
            }
        }
    }
}

/** Выбранную опцию отмечает галочка, а не радиокнопка: в монохроме отметка — это знак, не цвет. */
@Composable
private fun OptionList(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FilmaxMetrics.SettingsRowHeight)
                    .clip(ShapeButton)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (option == selected) {
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

// ── Размеры и вспомогательное ─────────────────────────────────────────────

private val AvatarSize = 64.dp

private val RowGap = 9.dp
