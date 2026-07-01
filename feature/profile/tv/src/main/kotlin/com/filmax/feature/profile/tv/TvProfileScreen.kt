package com.filmax.feature.profile.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.domain.user.model.initials
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileScreenModel
import com.filmax.feature.profile.common.ProfileSideEffect
import com.filmax.feature.profile.common.ProfileState
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)
private val HeroGradient = listOf(Color(0xFFB4305A), Color(0xFF6B4B8F))
private val AvatarGradient = listOf(Color(0xFFFFD89A), Color(0xFFF4B792))
private val InitialsColor = Color(0xFF5E1133)

// Акценты плиток-иконок (как в мобильном профиле — для консистентности).
private val AccentQuality = Color(0xFFB4305A)
private val AccentAudio = Color(0xFF6AC2B0)
private val AccentSubtitle = Color(0xFFF4B792)
private val AccentSubscription = Color(0xFFD4A84A)

/**
 * TV-Профиль (экран 05 макета). Слева — hero-карточка аккаунта (аватар/имя/подписка +
 * статистика), справа — сгруппированные настройки «Просмотр» и «Аккаунт». Наполнение
 * совпадает с мобильным профилем (реальные данные [ProfileScreenModel]); меняется только
 * раскладка под 10-foot. Клик по настройке циклически меняет значение.
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

    if (state.loading) {
        Box(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Accent)
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 56.dp, end = 56.dp, top = 110.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        ProfileHero(state = state, modifier = Modifier.width(400.dp))
        ProfileSettings(
            state = state,
            actions = ProfileActions(
                onCycleQuality = {
                    screenModel.dispatch(
                        ProfileEvent.SetQuality(next(PlaybackSettings.qualityOptions, state.playback.quality))
                    )
                },
                onCycleAudio = {
                    screenModel.dispatch(
                        ProfileEvent.SetAudioLanguage(next(PlaybackSettings.audioOptions, state.playback.audioLanguage))
                    )
                },
                onCycleSubtitle = {
                    screenModel.dispatch(
                        ProfileEvent.SetSubtitleLanguage(
                            next(PlaybackSettings.subtitleOptions, state.playback.subtitleLanguage)
                        )
                    )
                },
                onLogout = { screenModel.dispatch(ProfileEvent.Logout) },
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Левая колонка: hero-карточка аккаунта ────────────────────────────────────
@Composable
private fun ProfileHero(state: ProfileState, modifier: Modifier = Modifier) {
    val profile = state.profile
    val username = profile?.username ?: "Гость"
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(36.dp))
            .background(Brush.linearGradient(HeroGradient))
            .padding(32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(ShapeCookie)
                    .background(Brush.linearGradient(AvatarGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    profile.initialsOrFallback(),
                    color = InitialsColor,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(username, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 2)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD89A),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(profile?.subscription.label(), fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroStat(value = state.watchedCount.toString(), label = "Просмотрено", modifier = Modifier.weight(1f))
            HeroStat(value = state.favoritesCount.toString(), label = "В избранном", modifier = Modifier.weight(1f))
            HeroStat(value = state.quality ?: "—", label = "Качество", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f), maxLines = 1)
    }
}

// ── Правая колонка: группы настроек ──────────────────────────────────────────
private data class ProfileActions(
    val onCycleQuality: () -> Unit,
    val onCycleAudio: () -> Unit,
    val onCycleSubtitle: () -> Unit,
    val onLogout: () -> Unit,
)

@Composable
private fun ProfileSettings(
    state: ProfileState,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    ScrollToTopOnNavFocus(scrollState)
    Column(modifier = modifier.verticalScroll(scrollState)) {
        SettingsGroup(title = "Просмотр") { PlaybackSettingsRows(state, actions) }
        Spacer(Modifier.height(24.dp))
        SettingsGroup(title = "Аккаунт") { AccountSettingsRows(state, actions) }
    }
}

@Composable
private fun PlaybackSettingsRows(state: ProfileState, actions: ProfileActions) {
    SettingRow(
        spec = SettingRowSpec(
            icon = Icons.Filled.HighQuality,
            accent = AccentQuality,
            label = "Качество видео",
            value = state.playback.quality,
        ),
        onClick = actions.onCycleQuality,
        showDivider = true,
    )
    SettingRow(
        spec = SettingRowSpec(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            accent = AccentAudio,
            label = "Язык аудио",
            value = state.playback.audioLanguage,
        ),
        onClick = actions.onCycleAudio,
        showDivider = true,
    )
    SettingRow(
        spec = SettingRowSpec(
            icon = Icons.Filled.Subtitles,
            accent = AccentSubtitle,
            label = "Субтитры",
            value = state.playback.subtitleLanguage,
        ),
        onClick = actions.onCycleSubtitle,
        showDivider = false,
    )
}

@Composable
private fun AccountSettingsRows(state: ProfileState, actions: ProfileActions) {
    val active = state.profile?.subscription?.active == true
    SettingRow(
        spec = SettingRowSpec(
            icon = Icons.Filled.Star,
            accent = AccentSubscription,
            label = "Подписка",
            value = if (active) "Premium" else "Неактивна",
            badge = if (active) "PREMIUM" else null,
        ),
        onClick = null,
        showDivider = true,
    )
    SettingRow(
        spec = SettingRowSpec(
            icon = Icons.AutoMirrored.Filled.Logout,
            accent = MaterialTheme.colorScheme.error,
            label = "Выйти из аккаунта",
            labelColor = MaterialTheme.colorScheme.error,
        ),
        onClick = actions.onLogout,
        showDivider = false,
    )
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Text(
        title.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        content()
    }
}

private data class SettingRowSpec(
    val icon: ImageVector,
    val accent: Color,
    val label: String,
    val value: String? = null,
    val badge: String? = null,
    val labelColor: Color? = null,
)

@Composable
private fun SettingRow(
    spec: SettingRowSpec,
    onClick: (() -> Unit)?,
    showDivider: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val rowModifier = Modifier
        .fillMaxWidth()
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
        .clip(shape)
        .background(if (focused) Color.White.copy(alpha = 0.10f) else Color.Transparent)
        .then(if (focused) Modifier.border(3.dp, Accent, shape) else Modifier)
        .padding(horizontal = 22.dp, vertical = 18.dp)

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        SettingRowIcon(icon = spec.icon, accent = spec.accent)
        Spacer(Modifier.width(18.dp))
        SettingRowText(
            spec = spec,
            labelColor = spec.labelColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onClick != null) {
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    if (showDivider) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
private fun SettingRowIcon(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SettingRowText(spec: SettingRowSpec, labelColor: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(spec.label, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = labelColor)
            if (spec.badge != null) {
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(spec.accent)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        spec.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
        }
        if (!spec.value.isNullOrEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(spec.value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Вспомогательное ──────────────────────────────────────────────────────────

/** Следующее значение в списке опций по кругу. */
private fun next(options: List<String>, current: String): String {
    val index = options.indexOf(current)
    return options[(index + 1).mod(options.size)]
}

private fun UserProfile?.initialsOrFallback(): String = this?.initials()?.ifEmpty { "?" } ?: "?"

private fun Subscription?.label(): String = when {
    this?.active == true && daysLeft != null -> "Filmax Premium · ещё $daysLeft дн."
    this?.active == true -> "Filmax Premium"
    else -> "Бесплатный аккаунт"
}
