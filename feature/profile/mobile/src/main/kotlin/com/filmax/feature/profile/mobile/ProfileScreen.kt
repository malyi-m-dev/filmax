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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.ui.components.FilmaxListGroup
import com.filmax.core.ui.components.FilmaxListRow
import com.filmax.feature.profile.common.ProfileEvent
import com.filmax.feature.profile.common.ProfileScreenModel
import com.filmax.feature.profile.common.ProfileSideEffect
import com.filmax.feature.profile.common.ProfileState
import org.koin.androidx.compose.koinViewModel

private val SectionPadding = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)

private val AvatarGradient = listOf(Color(0xFFFFD89A), Color(0xFFF4B792))
private val InitialsColor = Color(0xFF5E1133)

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
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var activeSheet by remember { mutableStateOf<ProfileSheet?>(null) }
    val density = LocalDensity.current
    val header = rememberCollapsingHeaderState(initialHeightPx = with(density) { 340.dp.roundToPx() })

    Box(modifier.fillMaxSize().nestedScroll(header.nestedScrollConnection)) {
        ProfileContent(
            state = state,
            topInset = with(density) { header.spacerHeightPx.toDp() },
            onOpenSheet = { activeSheet = it },
            onOpenDesignSystem = onOpenDesignSystem,
            onLogout = { screenModel.dispatch(ProfileEvent.Logout) },
        )

        // Закреплённая шапка: уезжает вверх и гаснет по мере сворачивания.
        ProfileHero(
            state = state,
            modifier = Modifier
                .onSizeChanged { header.heightPx = it.height }
                .graphicsLayer {
                    translationY = header.offsetPx
                    alpha = (1f - header.progress * 1.3f).coerceIn(0f, 1f)
                },
        )

        // Компактная шапка: проявляется на финальной трети сворачивания.
        val barAlpha = ((header.progress - 0.65f) / 0.35f).coerceIn(0f, 1f)
        if (barAlpha > 0f) {
            ProfileCompactBar(
                username = state.username,
                modifier = Modifier.graphicsLayer { alpha = barAlpha },
            )
        }

        PlaybackSettingsSheets(
            activeSheet = activeSheet,
            playback = state.playback,
            onDismiss = { activeSheet = null },
            onSelectQuality = { screenModel.dispatch(ProfileEvent.SetQuality(it)) },
            onSelectAudio = { screenModel.dispatch(ProfileEvent.SetAudioLanguage(it)) },
            onSelectSubtitle = { screenModel.dispatch(ProfileEvent.SetSubtitleLanguage(it)) },
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileState,
    topInset: Dp,
    onOpenSheet: (ProfileSheet) -> Unit,
    onOpenDesignSystem: (() -> Unit)?,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Резерв под закреплённую шапку — ужимается синхронно с её сворачиванием.
        Spacer(Modifier.height(topInset))

        // TODO: значения строк ниже (кроме «Подписка») — статичные заглушки.
        //  Эти настройки клиентские и в API отсутствуют: «Качество видео (Авто)»,
        //  «Загрузки», «Субтитры и аудио», «Уведомления (вкл/выкл)», «Приватность».
        //  В будущем их нужно хранить локально (DataStore/multiplatform-settings).
        FilmaxListGroup(title = "Просмотр", modifier = SectionPadding) {
            FilmaxListRow(
                icon = Icons.Filled.HighQuality,
                accent = Color(0xFFB4305A),
                label = "Качество видео",
                value = state.playback.quality,
                onClick = { onOpenSheet(ProfileSheet.QUALITY) },
                showDivider = true,
            )
            FilmaxListRow(
                icon = Icons.Filled.Subtitles,
                accent = Color(0xFFF4B792),
                label = "Субтитры и аудио",
                value = state.playback.subtitleSummary(),
                onClick = { onOpenSheet(ProfileSheet.SUBTITLES) },
                showDivider = true,
            )
            FilmaxListRow(
                icon = Icons.Filled.Download,
                accent = Color(0xFF6AC2B0),
                label = "Загрузки",
                value = "Только по Wi-Fi",
            )
        }

        FilmaxListGroup(title = "Аккаунт", modifier = SectionPadding) {
            val active = state.profile?.subscription?.active == true
            FilmaxListRow(
                icon = Icons.Filled.Star,
                accent = Color(0xFFD4A84A),
                label = "Подписка",
                value = if (active) "Premium" else "Неактивна",
                badge = if (active) "PREMIUM" else null,
            )
        }

        if (onOpenDesignSystem != null) {
            FilmaxListGroup(title = "Разработчикам", modifier = SectionPadding) {
                FilmaxListRow(
                    icon = Icons.Filled.Code,
                    accent = MaterialTheme.colorScheme.primaryContainer,
                    label = "Дизайн-система",
                    value = "Каталог компонентов",
                    onClick = onOpenDesignSystem,
                )
            }
        }

        FilmaxListGroup(modifier = SectionPadding) {
            FilmaxListRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                accent = MaterialTheme.colorScheme.error,
                label = "Выйти",
                labelColor = MaterialTheme.colorScheme.error,
                onClick = onLogout,
            )
        }

        Spacer(Modifier.height(120.dp))
    }
}

// ── Шапка ───────────────────────────────────────────────────────────────────
@Composable
private fun ProfileHero(state: ProfileState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.linearGradient(
                    0.0f to Color(0xFFB4305A),
                    0.6f to Color(0xFF6B4B8F),
                    1.0f to MaterialTheme.colorScheme.surface,
                    start = Offset(Float.POSITIVE_INFINITY, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY),
                )
            )
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            HeroTitleRow()
            Spacer(Modifier.height(20.dp))
            HeroIdentity(username = state.username, subscription = state.profile?.subscription)
            Spacer(Modifier.height(20.dp))
            HeroStats(
                watchedCount = state.watchedCount,
                favoritesCount = state.favoritesCount,
                quality = state.quality,
            )
        }
    }
}

@Composable
private fun HeroTitleRow() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Профиль",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Настройки",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun HeroIdentity(username: String, subscription: Subscription?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(username = username, size = 80.dp, fontSize = 28.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                username.ifEmpty { "Гость" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD89A),
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    subscription.label(),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun HeroStats(watchedCount: Int, favoritesCount: Int, quality: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(value = watchedCount.toString(), label = "Просмотрено", modifier = Modifier.weight(1f))
        StatCard(value = favoritesCount.toString(), label = "В избранном", modifier = Modifier.weight(1f))
        StatCard(value = quality ?: "—", label = "Качество", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.75f),
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
private fun ProfileCompactBar(username: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Профиль",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Avatar(username = username, size = 32.dp, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Avatar(
    username: String,
    size: Dp,
    fontSize: TextUnit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(ShapeCookie)
            .background(Brush.linearGradient(AvatarGradient)),
        contentAlignment = Alignment.Center,
    ) {
        Text(username.initials(), fontSize = fontSize, fontWeight = FontWeight.ExtraBold, color = InitialsColor)
    }
}

// ── Bottom sheets настроек воспроизведения ───────────────────────────────────
private enum class ProfileSheet { QUALITY, SUBTITLES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSettingsSheets(
    activeSheet: ProfileSheet?,
    playback: PlaybackSettings,
    onDismiss: () -> Unit,
    onSelectQuality: (String) -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String) -> Unit,
) {
    if (activeSheet == null) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            when (activeSheet) {
                ProfileSheet.QUALITY -> OptionList(
                    title = "Качество видео",
                    options = PlaybackSettings.qualityOptions,
                    selected = playback.quality,
                    onSelect = onSelectQuality,
                )

                ProfileSheet.SUBTITLES -> {
                    OptionList(
                        title = "Аудио",
                        options = PlaybackSettings.audioOptions,
                        selected = playback.audioLanguage,
                        onSelect = onSelectAudio,
                    )
                    Spacer(Modifier.height(12.dp))
                    OptionList(
                        title = "Субтитры",
                        options = PlaybackSettings.subtitleOptions,
                        selected = playback.subtitleLanguage,
                        onSelect = onSelectSubtitle,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionList(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 12.dp),
    )
    options.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelect(option) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = option == selected, onClick = { onSelect(option) })
            Spacer(Modifier.width(8.dp))
            Text(option, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ── Вспомогательное ──────────────────────────────────────────────────────────
private val ProfileState.username: String get() = profile?.username ?: ""

private fun String.initials(): String = split(' ', '.')
    .filter { it.isNotBlank() }
    .take(2)
    .map { it.first().uppercaseChar() }
    .joinToString("")
    .ifEmpty { "?" }

private fun PlaybackSettings.subtitleSummary(): String {
    val subs = if (subtitleLanguage == PlaybackSettings.SubtitleOff) "субтитры выкл" else "суб: $subtitleLanguage"
    return "$audioLanguage · $subs"
}

private fun Subscription?.label(): String = when {
    this?.active == true && daysLeft != null -> "Filmax Premium · ещё $daysLeft дн."
    this?.active == true -> "Filmax Premium"
    else -> "Бесплатный аккаунт"
}

// ── Состояние коллапсирующей шапки ───────────────────────────────────────────
@Composable
private fun rememberCollapsingHeaderState(initialHeightPx: Int): CollapsingHeaderState =
    remember { CollapsingHeaderState(initialHeightPx) }

/**
 * Управляет сворачиванием закреплённой шапки: потребляет вертикальный скролл
 * (через [nestedScrollConnection]) и отдаёт прогресс/смещение для анимаций.
 * Шапка сворачивается при скролле вверх и разворачивается, когда список уже у верха.
 */
@Stable
private class CollapsingHeaderState(initialHeightPx: Int) {
    /** Полная высота шапки — измеряется UI через `onSizeChanged`. */
    var heightPx by mutableIntStateOf(initialHeightPx)

    /** Текущее смещение шапки вверх: 0 (раскрыта) … -[heightPx] (свёрнута). */
    var offsetPx by mutableFloatStateOf(0f)
        private set

    /** Прогресс сворачивания 0..1. */
    val progress: Float
        get() = if (heightPx > 0) (-offsetPx / heightPx).coerceIn(0f, 1f) else 0f

    /** Высота резерва-спейсера под шапку (ужимается вместе с ней). */
    val spacerHeightPx: Float
        get() = (heightPx + offsetPx).coerceAtLeast(0f)

    val nestedScrollConnection = object : NestedScrollConnection {
        // Сворачиваем первыми, пока есть движение вверх.
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f) consume(available.y) else Offset.Zero

        // Разворачиваем только то, что список не использовал (т.е. он уже у верха).
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f) consume(available.y) else Offset.Zero
    }

    private fun consume(delta: Float): Offset {
        val newOffset = (offsetPx + delta).coerceIn(-heightPx.toFloat(), 0f)
        val used = newOffset - offsetPx
        offsetPx = newOffset
        return Offset(0f, used)
    }
}
