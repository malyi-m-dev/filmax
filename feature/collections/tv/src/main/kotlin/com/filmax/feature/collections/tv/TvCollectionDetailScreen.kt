package com.filmax.feature.collections.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill
import com.filmax.feature.collections.common.CollectionDetailScreenModel
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)

/**
 * TV-экран одной подборки: сетка постеров поверх общего [CollectionDetailScreenModel]
 * (itemId берётся из маршрута через SavedStateHandle).
 */
@Composable
fun TvCollectionDetailScreen(
    title: String,
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: CollectionDetailScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 72.dp),
    ) {
        // Отступы сетки вынесены в contentPadding, чтобы постер при фокусе (scale 1.1) рос
        // внутрь viewport и не срезался границей по краям/сверху.
        Column(Modifier.padding(horizontal = 72.dp)) {
            Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text("Подборка", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 72.dp, end = 72.dp, top = 16.dp, bottom = 40.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    PosterTile(item = item, onClick = { onOpenItem(item.id) })
                }
            }
        }
    }
}

@Composable
private fun PosterTile(item: Item, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.height(240.dp)) {
        Box(Modifier.fillMaxSize()) {
            PosterImage(
                url = item.posters.medium.ifEmpty { item.posters.big },
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                shape = shape,
                accentColor = Accent,
            )
            RatingPill(
                rating = item.rating.external,
                compact = true,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
    }
}
