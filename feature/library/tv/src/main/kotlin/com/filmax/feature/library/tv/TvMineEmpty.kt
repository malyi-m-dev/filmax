package com.filmax.feature.library.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors

/** Пустые сегменты: что здесь будет и как туда что-то попадает. */
internal enum class MineEmptyText(val title: String, val hint: String) {
    Continue("Ничего не начато", "Включите тайтл — он появится здесь с того места, где вы остановились"),
    Watchlist("Список пуст", "Добавляйте тайтлы кнопкой «Буду смотреть» в карточке"),
    History("История пуста", "Здесь появится всё, что вы смотрели"),
    Folder("В папке пусто", "Добавляйте тайтлы кнопкой «В папку» в карточке"),
}

/** Пустое состояние сегмента или папки: иконка, заголовок, подсказка. */
@Composable
internal fun MineEmpty(text: MineEmptyText, icon: ImageVector, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.textDim, modifier = Modifier.size(36.dp))
        Text(text.title, style = FxType.rowTitle, color = colors.text)
        Text(text.hint, style = FxType.body, color = colors.textSecondary)
    }
}
