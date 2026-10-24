package com.filmax.feature.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxPalettes
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.radialWash
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxSegmentedControl
import org.koin.compose.koinInject

/** Палитры каталога в порядке выбора — те же, что в Профиле. */
private val CatalogPalettes = listOf(
    "Фиолет" to FilmaxPalettes.Violet,
    "Коралл" to FilmaxPalettes.Coral,
    "Мята" to FilmaxPalettes.Mint
)

/**
 * DEV-каталог дизайн-системы: токены и компоненты Filmax живьём, в любой из трёх палитр.
 * Открывается только в debug-сборке (Профиль → «Приложение» → «Дизайн-система»).
 *
 * Каталог собран из тех же токенов и компонентов, что и продуктовые экраны: секция, которую
 * нечем показать, — это сигнал, что общего компонента ещё нет, а не повод нарисовать образец
 * вручную «как должно быть». Палитра здесь своя: её переключают, не трогая настройку приложения.
 */
@Composable
fun DesignSystemScreen(
    modifier: Modifier = Modifier,
    navigator: Navigator = koinInject(),
) {
    var paletteIndex by rememberSaveable { mutableIntStateOf(0) }
    FilmaxTheme(palette = CatalogPalettes[paletteIndex].second) {
        val palette = FilmaxTheme.palette
        Box(
            modifier
                .fillMaxSize()
                .background(palette.background)
                .radialWash(palette.wash, centerX = 1f, centerY = 0f, radiusX = 1f, radiusY = 0.3f),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CatalogHeader(onBack = navigator::back)
                FilmaxSegmentedControl(
                    options = CatalogPalettes.map { it.first },
                    selectedIndex = paletteIndex,
                    onSelect = { paletteIndex = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = FilmaxMetrics.GridPadding),
                )
                ColorsSection()
                TypographySection()
                ButtonsSection()
                ChipsSection()
                BadgesSection()
                CardsSection()
                RowsSection()
                FieldsSection()
                PlaybackSection()
                StatesSection()
            }
        }
    }
}

@Composable
private fun CatalogHeader(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = FilmaxMetrics.GridPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilmaxIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", onClick = onBack)
        Column {
            Text("Дизайн-система", style = FilmaxType.sheetTitle, color = FilmaxColors.Text)
            Text("Токены и компоненты core:ui", style = FilmaxType.meta, color = FilmaxColors.TextDim)
        }
    }
}
