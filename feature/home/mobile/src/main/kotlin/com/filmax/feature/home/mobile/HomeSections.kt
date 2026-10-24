// Шторка «Разделы» с Главной: все разделы каталога плитками. Тап — раздел поверх Главной.

package com.filmax.feature.home.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.ui.components.FilmaxSectionTile
import com.filmax.core.ui.components.FilmaxSheet
import com.filmax.core.ui.components.hue
import com.filmax.core.ui.components.icon
import com.filmax.core.ui.components.label

/** Плиток в ряду: три — подпись «Документальное» ещё помещается на узком телефоне. */
private const val TILES_PER_ROW = 3

@Composable
internal fun HomeSectionsSheet(onOpenSection: (CatalogSection) -> Unit, onDismiss: () -> Unit) {
    FilmaxSheet(onDismiss = onDismiss, title = "Разделы", subtitle = "Всё, что есть в Filmax") {
        val sheet = this
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmaxMetrics.GridPadding)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            CatalogSection.entries.chunked(TILES_PER_ROW).forEach { rowSections ->
                Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                    rowSections.forEach { section ->
                        FilmaxSectionTile(
                            label = section.label,
                            icon = section.icon,
                            hue = section.hue,
                            onClick = {
                                onOpenSection(section)
                                sheet.dismiss()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Неполный последний ряд держит ширину плиток, а не растягивает их.
                    repeat(TILES_PER_ROW - rowSections.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private val TileGap = 10.dp
