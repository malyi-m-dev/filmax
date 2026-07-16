package com.filmax.feature.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxAccent
import com.filmax.core.designsystem.FilmaxError
import com.filmax.core.designsystem.FilmaxErrorContainer
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnAccent
import com.filmax.core.designsystem.FilmaxOnSurface
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.designsystem.FilmaxOnSurfaceVariant
import com.filmax.core.designsystem.FilmaxOutline
import com.filmax.core.designsystem.FilmaxOutlineVariant
import com.filmax.core.designsystem.FilmaxSurface
import com.filmax.core.designsystem.FilmaxSurfaceBright
import com.filmax.core.designsystem.FilmaxSurfaceContainer
import com.filmax.core.designsystem.FilmaxSurfaceContainerHigh
import com.filmax.core.designsystem.FilmaxSurfaceContainerHighest
import com.filmax.core.designsystem.FilmaxSurfaceContainerLow
import com.filmax.core.designsystem.FilmaxSurfaceContainerLowest
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.designsystem.ShapeCard
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.designsystem.ShapePoster
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxProgressBar
import com.filmax.core.ui.components.FilmaxProgressCard
import com.filmax.core.ui.components.FilmaxRatingPill
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import kotlin.math.roundToInt

/**
 * DEV-каталог дизайн-системы: токены и компоненты монохромного Filmax живьём.
 * Открывается только в debug-сборке (Профиль → «Разработчикам» → «Дизайн-система»).
 *
 * Каталог собран из тех же токенов и компонентов, что и продуктовые экраны: секция, которую
 * нечем показать, — это сигнал, что общего компонента ещё нет, а не повод нарисовать образец
 * вручную «как должно быть». Хардкод-цветов здесь нет ни одного — только токены.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CatalogTopBar(onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FilmaxMetrics.ScreenPadding),
        ) {
            CatalogIntro()
            ColorsSection()
            TypographySection()
            ShapesSection()
            CardsSection()
            ComponentsSection()
            CatalogOutro()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogTopBar(onBack: () -> Unit) {
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
}

@Composable
private fun CatalogIntro() {
    Column {
        Spacer(Modifier.height(8.dp))
        Text(
            "МОНОХРОМ · ТОЛЬКО ТЁМНАЯ ТЕМА",
            style = MaterialTheme.typography.labelMedium,
            color = FilmaxOnSurfaceDim,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Единственный цвет на экране — постер. Интерфейс ахроматичный: акцент белый, " +
                "цвет остался только у ошибок и никогда не является единственным носителем смысла.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun CatalogOutro() {
    Spacer(Modifier.height(4.dp))
    Text(
        "Filmax Design System · собрано из живых токенов и компонентов",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
    )
    Spacer(Modifier.height(48.dp))
}

/* ───────────────────────── примитивы каталога ───────────────────────── */

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
                style = MaterialTheme.typography.labelMedium,
                color = FilmaxOnSurfaceDim,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
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
        color = FilmaxOnSurfaceDim,
        modifier = Modifier.padding(bottom = 12.dp, top = 4.dp),
    )
}

/** Подложка для образцов. Обводка нужна: контейнер отличается от фона на пару процентов яркости. */
@Composable
private fun DsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeButton)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun DsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/* ───────────────────────── 01 · Цвета ───────────────────────── */

private data class Swatch(val name: String, val token: String, val color: Color)

@Composable
private fun ColorsSection() = Section(
    num = "01",
    title = "Цвета",
    desc = "Ахроматичная лестница: R = G = B. Поверхности отличаются на единицы процентов " +
        "яркости — иерархию держат отступы и вес шрифта, а не цвет.",
) {
    SubLabel("Поверхности")
    SwatchGrid(SurfaceSwatches)
    Spacer(Modifier.height(24.dp))
    SubLabel("Текст и границы")
    SwatchGrid(TextSwatches)
    Spacer(Modifier.height(24.dp))
    SubLabel("Акцент")
    SwatchGrid(AccentSwatches)
    Spacer(Modifier.height(24.dp))
    SubLabel("Ошибка")
    SwatchGrid(ErrorSwatches)
}

private val SurfaceSwatches = listOf(
    Swatch("Container Lowest", "FilmaxSurfaceContainerLowest", FilmaxSurfaceContainerLowest),
    Swatch("Surface", "FilmaxSurface", FilmaxSurface),
    Swatch("Container Low", "FilmaxSurfaceContainerLow", FilmaxSurfaceContainerLow),
    Swatch("Container", "FilmaxSurfaceContainer", FilmaxSurfaceContainer),
    Swatch("Container High", "FilmaxSurfaceContainerHigh", FilmaxSurfaceContainerHigh),
    Swatch("Container Highest", "FilmaxSurfaceContainerHighest", FilmaxSurfaceContainerHighest),
    Swatch("Surface Bright", "FilmaxSurfaceBright", FilmaxSurfaceBright),
)

private val TextSwatches = listOf(
    Swatch("On Surface", "FilmaxOnSurface", FilmaxOnSurface),
    Swatch("On Surface Variant", "FilmaxOnSurfaceVariant", FilmaxOnSurfaceVariant),
    Swatch("On Surface Dim", "FilmaxOnSurfaceDim", FilmaxOnSurfaceDim),
    Swatch("Outline", "FilmaxOutline", FilmaxOutline),
    Swatch("Outline Variant", "FilmaxOutlineVariant", FilmaxOutlineVariant),
)

private val AccentSwatches = listOf(
    Swatch("Accent", "FilmaxAccent", FilmaxAccent),
    Swatch("On Accent", "FilmaxOnAccent", FilmaxOnAccent),
)

private val ErrorSwatches = listOf(
    Swatch("Error", "FilmaxError", FilmaxError),
    Swatch("Error Container", "FilmaxErrorContainer", FilmaxErrorContainer),
)

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
            .clip(ShapeCard)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeCard),
    ) {
        // Обводка образца — по outline, а не по outlineVariant: почти-чёрные токены иначе
        // сливаются с подложкой плитки и «пропадают».
        Box(
            Modifier
                .padding(6.dp)
                .fillMaxWidth()
                .height(58.dp)
                .clip(ShapePoster)
                .background(swatch.color)
                .border(1.dp, MaterialTheme.colorScheme.outline, ShapePoster),
        )
        Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
            Text(
                swatch.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                swatch.token,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                hex(swatch.color),
                style = MaterialTheme.typography.labelSmall,
                color = FilmaxOnSurfaceDim,
            )
        }
    }
}

/** Подпись считается из самого токена: написанный руками hex рано или поздно разъезжается с цветом. */
private fun hex(color: Color): String {
    val red = (color.red * COLOR_CHANNEL_MAX).roundToInt()
    val green = (color.green * COLOR_CHANNEL_MAX).roundToInt()
    val blue = (color.blue * COLOR_CHANNEL_MAX).roundToInt()
    return "#%02X%02X%02X".format(red, green, blue)
}

private const val COLOR_CHANNEL_MAX = 255f

/* ───────────────────────── 02 · Типографика ───────────────────────── */

private data class TypeRow(val name: String, val spec: String, val style: TextStyle)

@Composable
private fun TypographySection() = Section(
    num = "02",
    title = "Типографика",
    desc = "В монохроме шрифт несёт то, что обычно несёт цвет: веса крупные и контрастные, " +
        "а шкала короткая — на экране одновременно живут 3-4 роли.",
) {
    DsCard {
        typographyRows().forEachIndexed { index, row ->
            if (index > 0) DsDivider()
            TypeSample(row)
        }
    }
}

@Composable
private fun typographyRows(): List<TypeRow> {
    val type = MaterialTheme.typography
    return listOf(
        TypeRow("Display Large", "32 · ExtraBold · hero главной", type.displayLarge),
        TypeRow("Display Medium", "30 · ExtraBold · название в деталях", type.displayMedium),
        TypeRow("Display Small", "26 · ExtraBold · заголовок раздела", type.displaySmall),
        TypeRow("Headline Medium", "26 · ExtraBold · онбординг, имя", type.headlineMedium),
        TypeRow("Headline Small", "24 · ExtraBold", type.headlineSmall),
        TypeRow("Title Large", "17 · Bold · заголовок ряда", type.titleLarge),
        TypeRow("Title Medium", "16 · Bold · кнопка, строка настройки", type.titleMedium),
        TypeRow("Title Small", "15 · SemiBold", type.titleSmall),
        TypeRow("Body Large", "14 · Normal · описание", type.bodyLarge),
        TypeRow("Body Medium", "13 · SemiBold · мета-строка", type.bodyMedium),
        TypeRow("Body Small", "12 · SemiBold · подпись постера", type.bodySmall),
        TypeRow("Label Large", "14 · SemiBold · чип", type.labelLarge),
        TypeRow("Label Medium", "11 · Bold · трекинг 2 · надзаголовок", type.labelMedium),
        TypeRow("Label Small", "11 · SemiBold · вкладка, рейтинг", type.labelSmall),
    )
}

@Composable
private fun TypeSample(row: TypeRow) {
    Text(
        row.name,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        row.spec,
        style = MaterialTheme.typography.labelSmall,
        color = FilmaxOnSurfaceDim,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        "Дюна: Часть вторая",
        style = row.style,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/* ───────────────────────── 03 · Формы ───────────────────────── */

private data class ShapeSpec(val name: String, val usage: String, val shape: Shape)

private val ShapeSpecs = listOf(
    ShapeSpec("ShapePoster", "постер 2:3, кадр 16:9 · 8", ShapePoster),
    ShapeSpec("ShapeCard", "карточка, превью эпизода · 11", ShapeCard),
    ShapeSpec("ShapeButton", "кнопка, поиск, строка · 13", ShapeButton),
    ShapeSpec("ShapeFull", "чип, pill навигации · 50%", ShapeFull),
)

@Composable
private fun ShapesSection() = Section(
    num = "03",
    title = "Формы",
    desc = "Четыре формы на всё приложение. Асимметричные и «печенька» убраны: по три-четыре " +
        "на экран они читались как шум, а не как система.",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ShapeSpecs.forEach { ShapeTile(it) }
    }
}

@Composable
private fun ShapeTile(spec: ShapeSpec) {
    Column(
        modifier = Modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(78.dp)
                .clip(spec.shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outline, spec.shape),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            spec.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            spec.usage,
            style = MaterialTheme.typography.labelSmall,
            color = FilmaxOnSurfaceDim,
        )
    }
}

/* ───────────────────────── 04 · Карточки ───────────────────────── */

@Composable
private fun CardsSection() = Section(
    num = "04",
    title = "Карточки",
    desc = "Медиа-карточек ровно две: постер 2:3 и карточка 16:9 с прогрессом. Размеры разные, " +
        "компонент один — образцы ниже в реальных метриках макета.",
) {
    SubLabel("FilmaxPosterCard · 2:3")
    PosterCardsRow()
    Spacer(Modifier.height(24.dp))
    SubLabel("FilmaxProgressCard · 16:9")
    ProgressCardsRow()
}

/**
 * Пустой url — это не заглушка ради каталога: [com.filmax.core.ui.components.PosterImage] всегда
 * держит под обложкой градиент-плейсхолдер, и здесь виден ровно он.
 */
@Composable
private fun PosterCardsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
    ) {
        FilmaxPosterCard(
            title = "Дюна: Часть вторая",
            posterUrl = "",
            onClick = {},
            rating = ratingLabel("8.312"),
            meta = posterMeta("Фильм", 2024),
        )
        FilmaxPosterCard(
            title = "Сёгун",
            posterUrl = "",
            onClick = {},
            width = FilmaxMetrics.GridPosterWidth,
            height = FilmaxMetrics.GridPosterHeight,
            rating = ratingLabel(8.7),
            meta = posterMeta("Сериал", 2024),
        )
        FilmaxPosterCard(
            title = "Оппенгеймер",
            posterUrl = "",
            onClick = {},
            width = FilmaxMetrics.SimilarPosterWidth,
            height = FilmaxMetrics.SimilarPosterHeight,
            meta = posterMeta("Фильм", 2023),
        )
    }
}

@Composable
private fun ProgressCardsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
    ) {
        FilmaxProgressCard(
            title = "Разделение",
            meta = "S2 · E5 · осталось 18 мин",
            posterUrl = "",
            progress = 0.45f,
            onClick = {},
        )
        FilmaxProgressCard(
            title = "Тед Лассо",
            meta = "S3 · E9 · осталось 6 мин",
            posterUrl = "",
            progress = 0.82f,
            onClick = {},
            width = FilmaxMetrics.MineCardWidth,
            height = FilmaxMetrics.MineCardHeight,
        )
    }
}

/* ───────────────────────── 05 · Компоненты ───────────────────────── */

@Composable
private fun ComponentsSection() = Section(
    num = "05",
    title = "Компоненты",
    desc = "Кнопки, чипы и строка настройки пока живут внутри экранов: в core/ui остались их " +
        "доредизайновые версии (заливка primaryContainer, галочка, цветная плитка-иконка), " +
        "и показывать их как эталон нельзя. Образцы ниже — тот вид, который ждёт переезда в core/ui.",
) {
    ButtonsBlock()
    Spacer(Modifier.height(24.dp))
    ChipsBlock()
    Spacer(Modifier.height(24.dp))
    IndicatorsBlock()
    Spacer(Modifier.height(24.dp))
    EmptyStateBlock()
    Spacer(Modifier.height(24.dp))
    SettingsRowBlock()
}

@Composable
private fun ButtonsBlock() {
    SubLabel("Кнопки")
    DsCard {
        DsPrimaryButton(text = "Смотреть", icon = Icons.Filled.PlayArrow, onClick = {})
        Spacer(Modifier.height(11.dp))
        DsSecondaryButton(text = "Буду смотреть", icon = Icons.Filled.Add, onClick = {})
        Spacer(Modifier.height(10.dp))
        Text(
            "Заливка в монохроме означает главное действие — вторая белая кнопка на экране " +
                "отнимает у первой весь смысл.",
            style = MaterialTheme.typography.bodySmall,
            color = FilmaxOnSurfaceDim,
        )
    }
}

/** Главная кнопка: белая заливка, тёмный контент. Высота 50 — тап-цель ≥48dp. */
@Composable
private fun DsPrimaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.PrimaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun DsSecondaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.SecondaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsBlock() {
    var selected by remember { mutableStateOf("Фильмы") }
    SubLabel("Чипы")
    DsCard {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            ChipLabels.forEach { label ->
                DsChip(label = label, selected = selected == label, onClick = { selected = label })
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Выбор отмечает заливка, и только она: галочек и рамок макет не знает.",
            style = MaterialTheme.typography.bodySmall,
            color = FilmaxOnSurfaceDim,
        )
    }
}

private val ChipLabels = listOf("Всё", "Фильмы", "Сериалы", "Аниме")

@Composable
private fun DsChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(FilmaxMetrics.ChipHeight)
            .clip(ShapeFull)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun IndicatorsBlock() {
    SubLabel("Рейтинг и прогресс")
    DsCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // ratingLabel(0) → null: kino.pub отдаёт ноль для тайтлов без оценки, и такой пилюли
            // в списке просто нет — здесь это видно на третьем аргументе.
            listOfNotNull(ratingLabel("8.312"), ratingLabel(7.5), ratingLabel(0.0))
                .forEach { FilmaxRatingPill(rating = it) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Пилюля живёт поверх постера: подложка полупрозрачная, цветового кодирования нет — " +
                "оценку несёт число.",
            style = MaterialTheme.typography.bodySmall,
            color = FilmaxOnSurfaceDim,
        )
        Spacer(Modifier.height(16.dp))
        FilmaxProgressBar(progress = 0.45f)
        Spacer(Modifier.height(10.dp))
        Text(
            "FilmaxProgressBar · 3dp, белая заливка по треку в 25% белого",
            style = MaterialTheme.typography.bodySmall,
            color = FilmaxOnSurfaceDim,
        )
    }
}

@Composable
private fun EmptyStateBlock() {
    SubLabel("Пустое состояние")
    DsCard {
        FilmaxEmptyState(
            icon = Icons.Outlined.BookmarkBorder,
            title = "Пока пусто",
            subtitle = "Добавляйте фильмы в список — они появятся здесь",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun SettingsRowBlock() {
    SubLabel("Строка настройки")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DsSettingsRow(label = "Качество видео", value = "Авто (до 4K)")
        DsSettingsRow(label = "Язык озвучки", value = "Русский")
        // Деструктив — единственное место, где в монохроме появляется цвет, и текст говорит
        // то же самое: цвет здесь дублирует смысл, а не несёт его.
        DsSettingsRow(label = "Выйти из аккаунта", value = null, labelColor = FilmaxError)
    }
}

@Composable
private fun DsSettingsRow(label: String, value: String?, labelColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.SettingsRowHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = labelColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!value.isNullOrBlank()) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = FilmaxOnSurfaceDim,
            )
        }
    }
}
