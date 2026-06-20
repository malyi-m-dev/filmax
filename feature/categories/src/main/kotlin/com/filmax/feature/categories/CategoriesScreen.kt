package com.filmax.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmax.core.designsystem.ShapeAsymA
import com.filmax.core.designsystem.ShapeAsymB
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.ui.components.PosterCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(top = 16.dp),
    ) {
        Text(
            "Жанры",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        if (state.loadingGenres) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.genres, key = { it.id }) { genre ->
                    GenreTile(
                        genre = genre,
                        index = state.genres.indexOf(genre),
                        onClick = { viewModel.onGenreClick(genre) },
                    )
                }
            }
        }
    }

    if (state.selectedGenre != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissSheet,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) },
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Text(
                    state.selectedGenre!!.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.loadingItems) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    val items = state.genreItems?.items ?: emptyList()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            PosterCard(
                                item = item,
                                isFav = false,
                                onFavClick = {},
                                onClick = {
                                    viewModel.onDismissSheet()
                                    onOpenItem(item.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreTile(genre: Genre, index: Int, onClick: () -> Unit) {
    val shape = when (index % 4) {
        0 -> ShapeAsymA
        1 -> ShapeAsymB
        2 -> ShapeCookie
        else -> RoundedCornerShape(24.dp)
    }
    val containerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(shape)
            .background(containerColors[index % containerColors.size])
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            genre.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 2,
        )
    }
}
