package com.filmax.feature.profile.tv

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.feature.profile.ProfileEvent
import com.filmax.feature.profile.ProfileScreenModel
import com.filmax.feature.profile.ProfileSideEffect
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)

/**
 * TV-Профиль (экран 05 макета). Поскольку мульти-профилей в приложении нет, показываем
 * реальный аккаунт: аватар/имя/подписка, статистику и настройки воспроизведения поверх
 * общего [ProfileScreenModel]. Клик по настройке циклически меняет значение.
 */
@Composable
fun TvProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: ProfileScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    screenModel.collectSideEffect { effect ->
        when (effect) {
            ProfileSideEffect.LoggedOut -> onLogout()
        }
    }

    val username = state.profile?.username ?: "Гость"
    val subscription = if (state.profile?.subscription?.active == true) "Premium" else "Free"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 72.dp, end = 72.dp, top = 120.dp, bottom = 56.dp),
    ) {
        // ── Шапка ───────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Accent, Color(0xFFF4B792)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(username.take(1).uppercase(), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(28.dp))
            Column {
                Text(username, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subscription.uppercase(), color = Accent, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(40.dp))

        // ── Статистика ───────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            StatCard("Просмотрено", "${state.watchedCount}")
            StatCard("В избранном", "${state.favoritesCount}")
            StatCard("Качество", state.quality ?: "—")
        }
        Spacer(Modifier.height(40.dp))

        // ── Настройки воспроизведения ─────────────────────────────────────
        Text("Воспроизведение", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        SettingRow(
            label = "Качество",
            value = state.playback.quality,
            onClick = { screenModel.dispatch(ProfileEvent.SetQuality(next(PlaybackSettings.qualityOptions, state.playback.quality))) },
        )
        Spacer(Modifier.height(12.dp))
        SettingRow(
            label = "Язык аудио",
            value = state.playback.audioLanguage,
            onClick = { screenModel.dispatch(ProfileEvent.SetAudioLanguage(next(PlaybackSettings.audioOptions, state.playback.audioLanguage))) },
        )
        Spacer(Modifier.height(12.dp))
        SettingRow(
            label = "Субтитры",
            value = state.playback.subtitleLanguage,
            onClick = { screenModel.dispatch(ProfileEvent.SetSubtitleLanguage(next(PlaybackSettings.subtitleOptions, state.playback.subtitleLanguage))) },
        )
        Spacer(Modifier.height(40.dp))

        TvButton("Выйти из аккаунта", onClick = { screenModel.dispatch(ProfileEvent.Logout) }, primary = false, leadingIcon = Icons.AutoMirrored.Filled.Logout)
    }
}

/** Следующее значение в списке опций по кругу. */
private fun next(options: List<String>, current: String): String {
    val i = options.indexOf(current)
    return options[(i + 1).mod(options.size)]
}

@Composable
private fun StatCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(24.dp),
    ) {
        Text(value, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.width(640.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Accent)
        }
    }
}
