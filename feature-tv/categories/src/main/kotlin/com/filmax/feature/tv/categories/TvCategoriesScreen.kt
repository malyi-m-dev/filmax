package com.filmax.feature.tv.categories

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.tv.designsystem.TvFocusCard

/** Жанр для плитки. [count] — справочное число для подписи. */
data class TvGenre(val id: String, val name: String, val emoji: String, val color: Color, val count: Int)

// Каталог жанров (как в макете). TODO: при появлении эндпоинта жанров — брать из catalog.
private val GENRES = listOf(
    TvGenre("drama", "Драма", "🎭", Color(0xFFB4305A), 1248),
    TvGenre("action", "Экшен", "💥", Color(0xFFD85A3A), 892),
    TvGenre("comedy", "Комедия", "😂", Color(0xFFE8A43A), 756),
    TvGenre("scifi", "Фантастика", "🚀", Color(0xFF6B4B8F), 534),
    TvGenre("thriller", "Триллер", "🔪", Color(0xFF4A5A8F), 612),
    TvGenre("romance", "Мелодрама", "💞", Color(0xFFE86D9E), 489),
    TvGenre("horror", "Ужасы", "👻", Color(0xFF3A3A4A), 327),
    TvGenre("animation", "Анимация", "🎨", Color(0xFF6AC2B0), 421),
    TvGenre("fantasy", "Фэнтези", "🐉", Color(0xFF94B9A8), 298),
    TvGenre("docu", "Документальное", "📽️", Color(0xFFC67A3E), 214),
    TvGenre("crime", "Криминал", "🕵️", Color(0xFF8B2C2C), 356),
    TvGenre("music", "Музыкальные", "🎵", Color(0xFF1E88E5), 142),
)

// Разноформенные плитки — повторяют «expressive» формы из макета.
private val SHAPES = listOf(
    RoundedCornerShape(topStart = 32.dp, topEnd = 64.dp, bottomEnd = 32.dp, bottomStart = 64.dp),
    RoundedCornerShape(percent = 48),
    RoundedCornerShape(32.dp),
    RoundedCornerShape(topStart = 64.dp, topEnd = 32.dp, bottomEnd = 64.dp, bottomStart = 32.dp),
)

/**
 * TV-Жанры (экран 03 макета): сетка разноформенных плиток. Данные статические —
 * экран презентационный; клик уводит в поиск по жанру.
 */
@Composable
fun TvCategoriesScreen(
    onOpenGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 72.dp, end = 72.dp, top = 120.dp),
    ) {
        Text("Жанры", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text("Найдите кино по настроению", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            items(GENRES, key = { it.id }) { genre ->
                val index = GENRES.indexOf(genre)
                GenreTile(genre = genre, shape = SHAPES[index % SHAPES.size], onClick = { onOpenGenre(genre.id) })
            }
        }
    }
}

@Composable
private fun GenreTile(genre: TvGenre, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit) {
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.height(190.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.linearGradient(listOf(genre.color, genre.color.copy(alpha = 0.6f))))
                .padding(24.dp),
        ) {
            Text(genre.emoji, fontSize = 48.sp, modifier = Modifier.align(androidx.compose.ui.Alignment.TopStart))
            Column(Modifier.align(androidx.compose.ui.Alignment.BottomStart)) {
                Text(genre.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("${genre.count} фильмов", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}
