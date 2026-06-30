package com.filmax.feature.search.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvPosterTitle
import com.filmax.core.tv.designsystem.posterSubtitle
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill
import com.filmax.core.ui.components.rememberVoiceSearch
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)

// Раскладка ЙЦУКЕН (как в макете). '⌫' — backspace.
private val KEY_ROWS = listOf(
    "й ц у к е н г ш щ з х".split(" "),
    "ф ы в а п р о л д ж э".split(" "),
    "я ч с м и т ь б ю — ⌫".split(" "),
)

/**
 * TV-Поиск (экран 02 макета): слева экранная клавиатура (D-pad) + голосовой ввод, справа сетка
 * результатов. Размеры подобраны под реальный экран (~960dp), чтобы клавиатура и результаты
 * помещались целиком. Поверх общего [SearchScreenModel] (тот же debounce-поиск, что и на телефоне).
 */
@Composable
fun TvSearchScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    fun type(key: String) {
        val q = if (key == "⌫") state.query.dropLast(1) else state.query + key
        screenModel.dispatch(SearchEvent.QueryChange(q))
    }

    val startVoiceSearch = rememberVoiceSearch { spoken ->
        screenModel.dispatch(SearchEvent.SubmitQuery(spoken))
    }

    val resultsGridState = rememberLazyGridState()
    ScrollToTopOnNavFocus(resultsGridState)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 72.dp, end = 72.dp, top = 108.dp, bottom = 36.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            // ── Клавиатура + голосовой ввод ─────────────────────────────
            Column(modifier = Modifier.width(500.dp)) {
                Text(
                    "Поиск",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(18.dp))
                // Строка ввода + кнопка голосового поиска
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 22.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = state.query.ifEmpty { "Введите название…" },
                            color = if (state.query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            maxLines = 1,
                        )
                    }
                    VoiceButton(onClick = startVoiceSearch)
                }
                Spacer(Modifier.height(16.dp))
                KEY_ROWS.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        row.forEach { key -> KeyCap(key = key, onClick = { type(key) }) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                KeyCap(key = "␣", onClick = { type(" ") }, wide = true)

                if (state.trendingQueries.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    QuickSuggestions(
                        items = state.trendingQueries.take(6),
                        onClick = { screenModel.dispatch(SearchEvent.SubmitQuery(it)) },
                    )
                }
            }

            // ── Результаты ──────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.results.isEmpty() && !state.loading) {
                    Text(
                        if (state.query.length >= 2) "Ничего не найдено" else "Начните вводить запрос",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = resultsGridState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(state.results, key = { it.id }) { item ->
                            PosterTile(item = item, onClick = { onOpenItem(item.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceButton(onClick: () -> Unit) {
    TvFocusCard(onClick = onClick, shape = CircleShape, modifier = Modifier.size(64.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "Голосовой поиск",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun KeyCap(key: String, onClick: () -> Unit, wide: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    TvFocusCard(
        onClick = onClick,
        shape = shape,
        modifier = Modifier.size(width = if (wide) 220.dp else 40.dp, height = 40.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (key == "␣") "Пробел" else key,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickSuggestions(items: List<String>, onClick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { q ->
            val shape = RoundedCornerShape(percent = 50)
            TvFocusCard(onClick = { onClick(q) }, shape = shape) {
                Box(
                    Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        q,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterTile(item: Item, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
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
            TvPosterTitle(
                title = item.title,
                subtitle = posterSubtitle(item.year, item.genres.firstOrNull()?.title),
            )
        }
    }
}
