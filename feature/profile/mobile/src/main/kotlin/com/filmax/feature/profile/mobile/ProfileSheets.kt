// Листы выбора настроек воспроизведения: качество видео, язык аудио и субтитры. Какой лист
// открыт, хранит ProfileScreen.

package com.filmax.feature.profile.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.domain.playback.PlaybackSettings

internal enum class ProfileSheet(val title: String) {
    QUALITY("Качество видео"),
    AUDIO("Язык аудио"),
    SUBTITLES("Субтитры"),
}

/** Колбэки выбора настроек воспроизведения: качество, аудио и субтитры. */
internal data class PlaybackSelection(
    val onSelectQuality: (String) -> Unit,
    val onSelectAudio: (String) -> Unit,
    val onSelectSubtitle: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackSettingsSheets(
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
