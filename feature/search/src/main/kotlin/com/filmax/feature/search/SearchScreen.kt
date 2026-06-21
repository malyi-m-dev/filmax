package com.filmax.feature.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.filmax.core.designsystem.ShapeAsymA
import com.filmax.core.designsystem.ShapeAsymB
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill

@Composable
fun SearchScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(top = 16.dp),
    ) {
        SearchBar(
            query = state.query,
            onQueryChange = { screenModel.dispatch(SearchEvent.QueryChange(it)) },
            onClear = { screenModel.dispatch(SearchEvent.QueryChange("")) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(16.dp))

        FilterChips(
            selected = state.filter,
            onSelect = { screenModel.dispatch(SearchEvent.FilterChange(it)) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(8.dp))

        AnimatedContent(
            targetState = when {
                state.loading -> "loading"
                state.results.isNotEmpty() -> "results"
                state.query.length >= 2 -> "empty"
                else -> "discover"
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "search_content",
        ) { target ->
            when (target) {
                "loading" -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                "results" -> SearchResults(items = state.results, onOpenItem = onOpenItem)

                "empty" -> Box(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Ничего не найдено",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                    )
                }

                else -> DiscoverContent(
                    recentQueries = state.recentQueries,
                    trendingQueries = state.trendingQueries,
                    onQueryClick = { screenModel.dispatch(SearchEvent.SubmitQuery(it)) },
                    onRecentClick = { screenModel.dispatch(SearchEvent.SubmitQuery(it)) },
                    onClearRecent = { screenModel.dispatch(SearchEvent.ClearRecent) },
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "Фильмы, сериалы, аниме…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                inner()
            },
        )
        Spacer(Modifier.width(8.dp))
        if (query.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Очистить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Голосовой поиск",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FilterChips(
    selected: ItemType?,
    onSelect: (ItemType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = listOf(
        null             to "Все",
        ItemType.MOVIE   to "Фильмы",
        ItemType.SERIES  to "Сериалы",
        ItemType.ANIME   to "Аниме",
        ItemType.DOCUMENTARY to "Документалки",
    )

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        items(filters) { (type, label) ->
            val isSelected = selected == type
            SuggestionChip(
                onClick = { onSelect(type) },
                label = {
                    Text(
                        label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    labelColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
                border = null,
            )
        }
    }
}

@Composable
private fun SearchResults(items: List<Item>, onOpenItem: (Int) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { item ->
            SearchResultRow(item = item, onClick = { onOpenItem(item.id) })
        }
    }
}

@Composable
private fun SearchResultRow(item: Item, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PosterImage(
            url = item.posters.small,
            contentDescription = item.title,
            modifier = Modifier
                .size(width = 60.dp, height = 88.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(
                    item.year.takeIf { it > 0 }?.toString(),
                    item.type.apiValue,
                ).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            if (item.genres.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    item.genres.take(2).joinToString(", ") { it.title },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        RatingPill(
            rating = item.rating.imdb?.toFloatOrNull() ?: (item.rating.filmax / 10f),
            compact = true,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverContent(
    recentQueries: List<String>,
    trendingQueries: List<String>,
    onQueryClick: (String) -> Unit,
    onRecentClick: (String) -> Unit,
    onClearRecent: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
        if (recentQueries.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Недавние",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(onClick = onClearRecent) {
                        Text("Очистить", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recentQueries.forEach { q ->
                        SuggestionChip(
                            onClick = { onRecentClick(q) },
                            label = { Text(q) },
                            icon = {
                                Icon(
                                    Icons.Filled.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            border = null,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        item {
            Text(
                "В тренде",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                trendingQueries.forEachIndexed { i, q ->
                    val shape = if (i % 2 == 0) ShapeAsymA else ShapeAsymB
                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onQueryClick(q) }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Text(
                            q,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}
