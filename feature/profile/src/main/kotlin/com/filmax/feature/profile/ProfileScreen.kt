package com.filmax.feature.profile

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.ui.components.FilmaxListGroup
import com.filmax.core.ui.components.FilmaxListRow
import org.koin.androidx.compose.koinViewModel

private val SectionPadding = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)

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

    val subscription = state.profile?.subscription

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        ProfileHero(
            username = state.profile?.username ?: "",
            watchedCount = state.watchedCount,
            favoritesCount = state.favoritesCount,
            quality = state.quality,
            subscription = subscription,
        )

        // TODO: значения строк ниже (кроме «Подписка») — статичные заглушки.
        //  Эти настройки клиентские и в API отсутствуют: «Качество видео (Авто)»,
        //  «Загрузки», «Субтитры и аудио», «Уведомления (вкл/выкл)», «Приватность»,
        //  число подключённых устройств. В будущем их нужно хранить локально
        //  (DataStore/multiplatform-settings) и завести соответствующие экраны.
        // ── Просмотр ────────────────────────────────────────────────────────────
        FilmaxListGroup(
            title = "Просмотр",
            modifier = SectionPadding,
        ) {
            FilmaxListRow(
                icon = Icons.Filled.HighQuality,
                accent = Color(0xFFB4305A),
                label = "Качество видео",
                value = "Авто (до 4K)",
                onClick = {},
                showDivider = true,
            )
            FilmaxListRow(
                icon = Icons.Filled.Download,
                accent = Color(0xFF6AC2B0),
                label = "Загрузки",
                value = "Только по Wi-Fi",
                onClick = {},
                showDivider = true,
            )
            FilmaxListRow(
                icon = Icons.Filled.Subtitles,
                accent = Color(0xFFF4B792),
                label = "Субтитры и аудио",
                value = "Русский",
                onClick = {},
                showDivider = true,
            )
            FilmaxListRow(
                icon = Icons.Filled.Cast,
                accent = Color(0xFFE86D9E),
                label = "Устройства",
                value = "Трансляция",
                onClick = {},
            )
        }

        // ── Аккаунт ─────────────────────────────────────────────────────────────
        FilmaxListGroup(
            title = "Аккаунт",
            modifier = SectionPadding,
        ) {
            FilmaxListRow(
                icon = Icons.Filled.Star,
                accent = Color(0xFFD4A84A),
                label = "Подписка",
                value = if (subscription?.active == true) "Premium" else "Неактивна",
                badge = if (subscription?.active == true) "PREMIUM" else null,
                onClick = {},
                showDivider = true,
            )
            FilmaxListRow(
                icon = Icons.Filled.Notifications,
                accent = Color(0xFF4A7C9E),
                label = "Уведомления",
                value = "Включены",
                onClick = {},
                showDivider = true,
            )
            FilmaxListRow(
                icon = Icons.Filled.Shield,
                accent = Color(0xFF8B2C2C),
                label = "Приватность",
                onClick = {},
            )
        }

        // ── Разработчикам ───────────────────────────────────────────────────────
        if (onOpenDesignSystem != null) {
            FilmaxListGroup(
                title = "Разработчикам",
                modifier = SectionPadding,
            ) {
                FilmaxListRow(
                    icon = Icons.Filled.Code,
                    accent = MaterialTheme.colorScheme.primaryContainer,
                    label = "Дизайн-система",
                    value = "Каталог компонентов",
                    onClick = onOpenDesignSystem,
                )
            }
        }

        // ── Выход ───────────────────────────────────────────────────────────────
        FilmaxListGroup(modifier = SectionPadding) {
            FilmaxListRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                accent = MaterialTheme.colorScheme.error,
                label = "Выйти",
                labelColor = MaterialTheme.colorScheme.error,
                onClick = { screenModel.dispatch(ProfileEvent.Logout) },
            )
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun ProfileHero(
    username: String,
    watchedCount: Int,
    favoritesCount: Int,
    quality: String?,
    subscription: Subscription?,
) {
    Box(
        modifier = Modifier
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
            // Top row: title + settings button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

            Spacer(Modifier.height(20.dp))

            // Avatar + name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(ShapeCookie)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFFD89A), Color(0xFFF4B792)))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val initials = username.split(' ', '.')
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercaseChar() }
                        .joinToString("")
                        .ifEmpty { "?" }
                    Text(
                        initials,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF5E1133),
                    )
                }
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
                            subscriptionLabel(subscription),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    value = watchedCount.toString(),
                    label = "Просмотрено",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = favoritesCount.toString(),
                    label = "В избранном",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = quality ?: "—",
                    label = "Качество",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun subscriptionLabel(subscription: Subscription?): String = when {
    subscription?.active == true && subscription.daysLeft != null ->
        "Filmax Premium · ещё ${subscription.daysLeft} дн."
    subscription?.active == true -> "Filmax Premium"
    else -> "Бесплатный аккаунт"
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
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.75f),
            letterSpacing = 0.3.sp,
        )
    }
}

