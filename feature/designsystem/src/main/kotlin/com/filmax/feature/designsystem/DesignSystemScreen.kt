package com.filmax.feature.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.filmax.core.designsystem.FilmaxError
import com.filmax.core.designsystem.FilmaxOnSurface
import com.filmax.core.designsystem.FilmaxOnSurfaceVariant
import com.filmax.core.designsystem.FilmaxOutline
import com.filmax.core.designsystem.FilmaxOutlineVariant
import com.filmax.core.designsystem.FilmaxPrimary
import com.filmax.core.designsystem.FilmaxPrimaryContainer
import com.filmax.core.designsystem.FilmaxSecondary
import com.filmax.core.designsystem.FilmaxSecondaryContainer
import com.filmax.core.designsystem.FilmaxSurface
import com.filmax.core.designsystem.FilmaxSurfaceBright
import com.filmax.core.designsystem.FilmaxSurfaceContainer
import com.filmax.core.designsystem.FilmaxSurfaceContainerHigh
import com.filmax.core.designsystem.FilmaxSurfaceContainerHighest
import com.filmax.core.designsystem.FilmaxSurfaceContainerLow
import com.filmax.core.designsystem.FilmaxSurfaceContainerLowest
import com.filmax.core.designsystem.FilmaxTertiary
import com.filmax.core.designsystem.FilmaxTertiaryContainer
import com.filmax.core.designsystem.ShapeAsymA
import com.filmax.core.designsystem.ShapeAsymB
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.ui.components.ContinueCard
import com.filmax.core.ui.components.FilmaxBadge
import com.filmax.core.ui.components.FilmaxBadgeStyle
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonVariant
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxCollectionCard
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxListGroup
import com.filmax.core.ui.components.FilmaxListRow
import com.filmax.core.ui.components.FilmaxPreviewSamples
import com.filmax.core.ui.components.FilmaxSearchField
import com.filmax.core.ui.components.FilmaxStatCard
import com.filmax.core.ui.components.FilmaxTab
import com.filmax.core.ui.components.FilmaxTabBar
import com.filmax.core.ui.components.GradientPosterPlaceholder
import com.filmax.core.ui.components.PosterCard
import com.filmax.core.ui.components.RatingPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Дизайн-система") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Material 3 Expressive · Cinema Dark",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Токены и компоненты Filmax — один источник правды для цвета, " +
                    "типографики, формы и UI-китов.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            ColorsSection()
            TypographySection()
            ShapeMotionSection()
            ButtonsSection()
            ChipsSection()
            InputsSection()
            PostersSection()
            CardsSection()
            ListsSection()
            IconsSection()
            NavigationSection()

            Spacer(Modifier.height(48.dp))
            Text(
                "Filmax Design System · собрано из живых компонентов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

/* ───────────────────────── doc primitives ───────────────────────── */

@Composable
private fun Section(
    num: String,
    title: String,
    desc: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(bottom = 44.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                num,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun SubLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 12.dp, top = 4.dp),
    )
}

@Composable
private fun DsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        content()
    }
}

/* ───────────────────────── 01 · Colors ───────────────────────── */

private data class Swatch(val name: String, val token: String, val color: Color, val onColor: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorsSection() = Section(
    num = "01",
    title = "Цвета",
    desc = "Палитра M3 в тёмной кино-теме с тёплым уклоном. Контейнеры — фоны компонентов, «on-»-роли — текст и иконки поверх них.",
) {
    val core = listOf(
        Swatch("Primary", "--m3-primary", FilmaxPrimary, Color(0xFF5E1133)),
        Swatch("Primary Container", "--m3-primary-container", FilmaxPrimaryContainer, Color.White),
        Swatch("Secondary", "--m3-secondary", FilmaxSecondary, Color(0xFF432933)),
        Swatch("Secondary Cont.", "--m3-secondary-container", FilmaxSecondaryContainer, Color.White),
        Swatch("Tertiary", "--m3-tertiary", FilmaxTertiary, Color(0xFF4F2500)),
        Swatch("Tertiary Cont.", "--m3-tertiary-container", FilmaxTertiaryContainer, Color.White),
        Swatch("Error", "--m3-error", FilmaxError, Color(0xFF690005)),
    )
    val surfaces = listOf(
        Swatch("Cont. Lowest", "lowest", FilmaxSurfaceContainerLowest, Color.White),
        Swatch("Surface", "--m3-surface", FilmaxSurface, Color.White),
        Swatch("Container Low", "low", FilmaxSurfaceContainerLow, Color.White),
        Swatch("Container", "--m3-surface-container", FilmaxSurfaceContainer, Color.White),
        Swatch("Container High", "high", FilmaxSurfaceContainerHigh, Color.White),
        Swatch("Container Highest", "highest", FilmaxSurfaceContainerHighest, Color.White),
        Swatch("Surface Bright", "bright", FilmaxSurfaceBright, Color.White),
        Swatch("On Surface", "--m3-on-surface", FilmaxOnSurface, Color(0xFF141012)),
        Swatch("On Surface Var.", "variant", FilmaxOnSurfaceVariant, Color(0xFF141012)),
        Swatch("Outline", "--m3-outline", FilmaxOutline, Color(0xFF141012)),
        Swatch("Outline Variant", "variant", FilmaxOutlineVariant, Color.White),
    )
    val accents = listOf(
        Swatch("Rosé · default", "#B4305A", Color(0xFFB4305A), Color.White),
        Swatch("Violet", "#6750A4", Color(0xFF6750A4), Color.White),
        Swatch("Coral", "#E46962", Color(0xFFE46962), Color.White),
        Swatch("Amber", "#FFB86B", Color(0xFFFFB86B), Color(0xFF4F2500)),
        Swatch("Azure", "#1E88E5", Color(0xFF1E88E5), Color.White),
        Swatch("Green", "#2E7D52", Color(0xFF2E7D52), Color.White),
    )

    SubLabel("Основные роли")
    SwatchGrid(core)
    Spacer(Modifier.height(24.dp))
    SubLabel("Поверхности и текст")
    SwatchGrid(surfaces)
    Spacer(Modifier.height(24.dp))
    SubLabel("Акценты (переключаются в Tweaks)")
    SwatchGrid(accents)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchGrid(items: List<Swatch>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { SwatchTile(it) }
    }
}

@Composable
private fun SwatchTile(swatch: Swatch) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(swatch.color),
        )
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                swatch.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                swatch.token,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                hex(swatch.color),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun hex(color: Color): String {
    val r = (color.red * 255f).roundToInt()
    val g = (color.green * 255f).roundToInt()
    val b = (color.blue * 255f).roundToInt()
    return "#%02X%02X%02X".format(r, g, b)
}

/* ───────────────────────── 02 · Typography ───────────────────────── */

@Composable
private fun TypographySection() = Section(
    num = "02",
    title = "Типографика",
    desc = "Семантическая шкала Material 3. Заголовки — насыщенные и плотные, тело — нейтральное и читаемое.",
) {
    val type = MaterialTheme.typography
    val rows = listOf(
        Triple("Display Small", "36 / 800 · заголовки экранов", type.displaySmall),
        Triple("Headline Large", "32 / 700 · герой, названия", type.headlineLarge),
        Triple("Headline Medium", "28 / 700", type.headlineMedium),
        Triple("Headline Small", "24 / 600 · карточки", type.headlineSmall),
        Triple("Title Large", "22 / 600 · заголовки секций", type.titleLarge),
        Triple("Title Medium", "16 / 600", type.titleMedium),
        Triple("Body Large", "16 / 400 · описания", type.bodyLarge),
        Triple("Body Medium", "14 / 400", type.bodyMedium),
        Triple("Label Large", "14 / 600 · кнопки, чипы", type.labelLarge),
    )
    DsCard {
        rows.forEachIndexed { index, (label, spec, style) ->
            if (index > 0) {
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Spacer(Modifier.height(14.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                spec,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Дюна: Часть вторая",
                style = style,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/* ───────────────────────── 03 · Shape & Motion ───────────────────────── */

private data class ShapeSpec(val name: String, val shape: Shape, val expressive: Boolean)

@Composable
private fun ShapeMotionSection() = Section(
    num = "03",
    title = "Форма и движение",
    desc = "Скруглённая шкала форм плюс выразительные асимметричные и cookie-формы — фирменный приём M3 Expressive.",
) {
    val shapes = listOf(
        ShapeSpec("xs · 8", RoundedCornerShape(8.dp), false),
        ShapeSpec("sm · 16", RoundedCornerShape(16.dp), false),
        ShapeSpec("md · 20", RoundedCornerShape(20.dp), false),
        ShapeSpec("lg · 28", RoundedCornerShape(28.dp), false),
        ShapeSpec("xl · 36", RoundedCornerShape(36.dp), false),
        ShapeSpec("Cookie", ShapeCookie, true),
        ShapeSpec("Asym A", ShapeAsymA, true),
        ShapeSpec("Asym B", ShapeAsymB, true),
        ShapeSpec("Full", CircleShape, true),
    )
    SubLabel("Шкала скруглений")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        shapes.forEach { spec ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(78.dp)
                        .clip(spec.shape)
                        .background(
                            if (spec.expressive) {
                                Brush.linearGradient(listOf(Color(0xFFB4305A), Color(0xFF6B4B8F)))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                )
                            },
                        ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    spec.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
    Spacer(Modifier.height(28.dp))
    SubLabel("Кривые движения")
    val motion = listOf(
        "Spring" to "Морфинг FAB, выразительные переходы",
        "Emphasized" to "Появление страниц, развороты",
        "Standard" to "Состояния, наведение",
        "Bounce" to "Лайк, индикатор таб-бара",
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        motion.forEach { (name, use) ->
            DsCard {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    use,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* ───────────────────────── 04 · Buttons ───────────────────────── */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ButtonsSection() = Section(
    num = "04",
    title = "Кнопки",
    desc = "Компонент FilmaxButton — пять вариантов с минимальным API: text + onClick, опционально иконка и вариант.",
) {
    DsCard {
        SubLabel("Варианты")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilmaxButton("Смотреть", onClick = {}, icon = Icons.Filled.PlayArrow)
            FilmaxButton("В список", onClick = {}, variant = FilmaxButtonVariant.Tonal, icon = Icons.Filled.Add)
            FilmaxButton("Скачать", onClick = {}, variant = FilmaxButtonVariant.Outlined, icon = Icons.Filled.Download)
            FilmaxButton("Поделиться", onClick = {}, variant = FilmaxButtonVariant.Elevated, icon = Icons.Filled.Share)
            FilmaxButton("Подробнее", onClick = {}, variant = FilmaxButtonVariant.Text)
        }
    }
}

/* ───────────────────────── 05 · Chips ───────────────────────── */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsSection() = Section(
    num = "05",
    title = "Чипы и фильтры",
    desc = "Компонент FilmaxChip для фильтров. Активное состояние получает контейнерную заливку и галочку.",
) {
    var selected by remember { mutableStateOf("Всё") }
    DsCard {
        SubLabel("Состояния")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Всё", "Фильмы", "Сериалы", "Аниме").forEach { label ->
                FilmaxChip(
                    label = label,
                    selected = selected == label,
                    onClick = { selected = label },
                )
            }
        }
    }
}

/* ───────────────────────── 06 · Inputs & Rating ───────────────────────── */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputsSection() = Section(
    num = "06",
    title = "Поля, бейджи и рейтинг",
    desc = "Поисковое поле FilmaxSearchField, бейджи FilmaxBadge и рейтинг RatingPill с цветовой кодировкой по баллу.",
) {
    var query by remember { mutableStateOf("") }
    DsCard {
        SubLabel("Поиск")
        FilmaxSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Фильмы, сериалы, актёры…",
        )
        Spacer(Modifier.height(24.dp))
        SubLabel("Бейджи")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilmaxBadge("Premium", style = FilmaxBadgeStyle.Primary, icon = Icons.Filled.Star)
            FilmaxBadge("Активна", style = FilmaxBadgeStyle.Success)
            FilmaxBadge("4K HDR", style = FilmaxBadgeStyle.Neutral)
            FilmaxBadge("Скоро", style = FilmaxBadgeStyle.Warning)
            FilmaxBadge("Истекла", style = FilmaxBadgeStyle.Error)
        }
        Spacer(Modifier.height(24.dp))
        SubLabel("Рейтинг")
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RatingPill(rating = 9.1)
            RatingPill(rating = 7.8)
            RatingPill(rating = 6.4)
            RatingPill(rating = 8.6, compact = true)
            RatingPill(rating = null, compact = true)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "≥8.5 бирюзовый · ≥7.5 янтарный · ниже нейтральный",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ───────────────────────── 07 · Posters ───────────────────────── */

@Composable
private fun PostersSection() = Section(
    num = "07",
    title = "Постеры",
    desc = "PosterImage с градиентным плейсхолдером (пока нет реального изображения) и готовая PosterCard с рейтингом и кнопкой избранного.",
) {
    SubLabel("Плейсхолдеры")
    val accents = listOf(
        Color(0xFFB4305A), Color(0xFF6750A4), Color(0xFFE46962),
        Color(0xFFFFB86B), Color(0xFF1E88E5), Color(0xFF2E7D52),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        accents.forEach { accent ->
            Box(
                Modifier
                    .width(140.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                GradientPosterPlaceholder(accent, modifier = Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    RatingPill(rating = 8.4, compact = true)
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    SubLabel("PosterCard")
    PosterCard(
        item = FilmaxPreviewSamples.movie,
        isFav = true,
        onClick = {},
        onFavClick = {},
    )
}

/* ───────────────────────── 08 · Cards ───────────────────────── */

private data class StatSpec(
    val color: Color,
    val shape: Shape,
    val label: String,
    val value: String,
    val sub: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardsSection() = Section(
    num = "08",
    title = "Карточки",
    desc = "Готовые карточные паттерны каталога: ContinueCard, FilmaxCollectionCard и stat-карточки с выразительными формами.",
) {
    SubLabel("Продолжить просмотр")
    ContinueCard(
        item = FilmaxPreviewSamples.movie,
        progress = 0.45f,
        onClick = {},
        modifier = Modifier.width(260.dp),
    )
    Spacer(Modifier.height(24.dp))

    SubLabel("Подборки")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilmaxCollectionCard(
            title = "Оскар 2024",
            subtitle = "Победители и номинанты",
            accent = Color(0xFFD4A84A),
            icon = Icons.Filled.Star,
            onClick = {},
            modifier = Modifier.width(240.dp),
        )
        FilmaxCollectionCard(
            title = "В тренде",
            subtitle = "Что смотрят сейчас",
            accent = Color(0xFFB4305A),
            icon = Icons.Filled.LocalFireDepartment,
            onClick = {},
            modifier = Modifier.width(240.dp),
        )
    }
    Spacer(Modifier.height(24.dp))

    SubLabel("Stat-карточки")
    val stats = listOf(
        StatSpec(Color(0xFFB4305A), ShapeAsymA, "Рейтинг", "8.6", "Filmax"),
        StatSpec(Color(0xFFF4B792), ShapeCookie, "Длительность", "2ч", "46м"),
        StatSpec(Color(0xFF6AC2B0), ShapeAsymB, "Жанр", "Драма", "Фантастика"),
        StatSpec(Color(0xFFE86D9E), RoundedCornerShape(28.dp), "Год", "2024", "—"),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stats.forEach { stat ->
            FilmaxStatCard(
                label = stat.label,
                value = stat.value,
                accent = stat.color,
                sub = stat.sub,
                shape = stat.shape,
                modifier = Modifier.width(160.dp),
            )
        }
    }
}

/* ───────────────────────── 09 · Lists & Surfaces ───────────────────────── */

@Composable
private fun ListsSection() = Section(
    num = "09",
    title = "Списки и поверхности",
    desc = "Сгруппированные строки настроек и пустые состояния.",
) {
    SubLabel("Группа-список")
    FilmaxListGroup {
        FilmaxListRow(
            icon = Icons.Filled.PlayArrow,
            label = "Качество видео",
            value = "Авто (до 4K)",
            accent = Color(0xFFB4305A),
            onClick = {},
            showDivider = true,
        )
        FilmaxListRow(
            icon = Icons.Filled.Download,
            label = "Загрузки",
            value = "12.4 / 60 ГБ",
            accent = Color(0xFF6AC2B0),
            onClick = {},
            showDivider = true,
        )
        FilmaxListRow(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            label = "Субтитры и аудио",
            value = "Русский",
            accent = Color(0xFFF4B792),
            onClick = {},
        )
    }
    Spacer(Modifier.height(20.dp))
    SubLabel("Пустое состояние")
    DsCard {
        FilmaxEmptyState(
            icon = Icons.Filled.Favorite,
            title = "Пока пусто",
            subtitle = "Лайкайте фильмы, чтобы они попадали сюда",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
    }
}

/* ───────────────────────── 10 · Icons ───────────────────────── */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconsSection() = Section(
    num = "10",
    title = "Иконки",
    desc = "Material Symbols (extended). Набор покрывает навигацию, плеер и действия каталога.",
) {
    val icons: List<Pair<String, ImageVector>> = listOf(
        "home" to Icons.Filled.Home,
        "search" to Icons.Filled.Search,
        "bookmark" to Icons.Filled.Bookmark,
        "person" to Icons.Filled.Person,
        "play" to Icons.Filled.PlayArrow,
        "pause" to Icons.Filled.Pause,
        "add" to Icons.Filled.Add,
        "favorite" to Icons.Filled.Favorite,
        "star" to Icons.Filled.Star,
        "fire" to Icons.Filled.LocalFireDepartment,
        "sparkle" to Icons.Filled.AutoAwesome,
        "trending" to Icons.AutoMirrored.Filled.TrendingUp,
        "download" to Icons.Filled.Download,
        "share" to Icons.Filled.Share,
        "cast" to Icons.Filled.Cast,
        "volume" to Icons.AutoMirrored.Filled.VolumeUp,
        "fullscreen" to Icons.Filled.Fullscreen,
        "settings" to Icons.Filled.Settings,
        "tune" to Icons.Filled.Tune,
        "grid" to Icons.Filled.GridView,
        "eye" to Icons.Filled.Visibility,
        "notifications" to Icons.Filled.Notifications,
        "mic" to Icons.Filled.Mic,
        "history" to Icons.Filled.History,
        "clock" to Icons.Filled.Schedule,
        "check" to Icons.Filled.Check,
        "close" to Icons.Filled.Close,
        "more" to Icons.Filled.MoreVert,
        "menu" to Icons.Filled.Menu,
        "back" to Icons.AutoMirrored.Filled.ArrowBack,
        "forward" to Icons.AutoMirrored.Filled.ArrowForward,
    )
    DsCard {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icons.forEach { (name, vector) ->
                Column(
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(vector, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/* ───────────────────────── 11 · Navigation ───────────────────────── */

@Composable
private fun NavigationSection() = Section(
    num = "11",
    title = "Навигация",
    desc = "Нижний таб-бар FilmaxTabBar с морфинг-индикатором активной вкладки. Кликабельно.",
) {
    var tab by remember { mutableStateOf(FilmaxTab.HOME) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(vertical = 12.dp),
    ) {
        FilmaxTabBar(selected = tab, onSelect = { tab = it })
    }
}
