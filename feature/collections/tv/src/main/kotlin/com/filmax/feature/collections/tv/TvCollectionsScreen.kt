package com.filmax.feature.collections.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.collections.common.CollectionsScreenModel
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)

/**
 * TV-Подборки: сетка подборок поверх общего [CollectionsScreenModel] (те же данные, что и на
 * телефоне). Клик открывает экран одной подборки.
 */
@Composable
fun TvCollectionsScreen(
    onOpenCollection: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: CollectionsScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 72.dp, end = 72.dp, top = 120.dp),
    ) {
        Text("Подборки", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text("Тематические коллекции", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                items(state.collections, key = { it.id }) { collection ->
                    CollectionCard(collection = collection, onClick = { onOpenCollection(collection.id, collection.title) })
                }
            }
        }
    }
}

@Composable
private fun CollectionCard(collection: Collection, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.height(220.dp)) {
        Box(Modifier.fillMaxSize()) {
            PosterImage(
                url = collection.posters?.medium ?: collection.posters?.big.orEmpty(),
                contentDescription = collection.title,
                modifier = Modifier.fillMaxSize(),
                shape = shape,
                accentColor = Accent,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            0.35f to Color.Transparent,
                            1f to Color(0xF20A0809),
                        )
                    )
            )
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(collection.title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 2)
                collection.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
