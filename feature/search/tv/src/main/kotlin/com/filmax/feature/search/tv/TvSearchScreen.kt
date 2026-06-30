package com.filmax.feature.search.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)

// Раскладка ЙЦУКЕН (как в макете). '⌫' — backspace, '␣' — пробел.
private val KEY_ROWS = listOf(
    "й ц у к е н г ш щ з х".split(" "),
    "ф ы в а п р о л д ж э".split(" "),
    "я ч с м и т ь б ю — ⌫".split(" "),
)

/**
 * TV-Поиск (экран 02 макета): слева экранная клавиатура (D-pad), справа сетка результатов.
 * Поверх общего [SearchScreenModel] (тот же debounce-поиск, что и на телефоне).
 */
@Composable
fun TvSearchScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    fun type(key: String) {
        val q = when (key) {
            "⌫" -> state.query.dropLast(1)
            "␣" -> state.query + " "
            else -> state.query + key
        }
        screenModel.dispatch(SearchEvent.QueryChange(q))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 72.dp, end = 72.dp, top = 120.dp, bottom = 40.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            // ── Клавиатура ──────────────────────────────────────────────
            Column(modifier = Modifier.width(620.dp)) {
                Text("Поиск", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(24.dp))
                // Строка ввода
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = state.query.ifEmpty { "Введите название…" },
                        color = if (state.query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                    )
                }
                Spacer(Modifier.height(20.dp))
                KEY_ROWS.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        row.forEach { key -> KeyCap(key = key, onClick = { type(key) }) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                KeyCap(key = "␣", onClick = { type("␣") }, wide = true)

                // Быстрые подсказки (тренды)
                if (state.trendingQueries.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    QuickSuggestions(
                        items = state.trendingQueries.take(6),
                        onClick = { screenModel.dispatch(SearchEvent.SubmitQuery(it)) },
                    )
                }
            }

            // ── Результаты ──────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
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

@Composable
private fun KeyCap(key: String, onClick: () -> Unit, wide: Boolean = false) {
    val shape = RoundedCornerShape(14.dp)
    TvFocusCard(
        onClick = onClick,
        shape = shape,
        modifier = Modifier.size(width = if (wide) 200.dp else 52.dp, height = 52.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (key == "␣") "Пробел" else key, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(q, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun PosterTile(item: Item, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.height(264.dp)) {
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
