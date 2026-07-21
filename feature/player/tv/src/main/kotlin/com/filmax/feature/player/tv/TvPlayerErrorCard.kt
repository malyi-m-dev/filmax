package com.filmax.feature.player.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.error.AppError
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest

/**
 * Карточка ошибки поверх кадра TV-плеера. Кнопок нет намеренно: единственный выход из
 * плеера — «Назад». До неё сбой загрузки серии оставлял чёрный экран без индикации
 * (жалоба «следующая серия не запустилась»).
 */
@Composable
internal fun PlayerErrorCard(error: AppError, modifier: Modifier = Modifier) {
    Column(
        modifier
            .widthIn(max = ErrorCardMaxWidth)
            .clip(TvMetrics.PanelShape)
            .background(TvSurfaceContainer.copy(alpha = 0.97f))
            .border(1.dp, TvSurfaceContainerHighest, TvMetrics.PanelShape)
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val texts = errorTexts(error)
        Text(
            texts.first,
            style = MaterialTheme.typography.titleMedium,
            color = TvOnSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            texts.second,
            style = MaterialTheme.typography.bodySmall,
            color = TvOnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "«Назад» — выйти из плеера",
            style = MaterialTheme.typography.labelSmall,
            color = TvOnSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

/** Тексты ошибки для TV-карточки — компактный аналог мобильной FilmaxErrorModal. */
private fun errorTexts(error: AppError): Pair<String, String> = when (error) {
    AppError.Offline -> "Нет подключения" to "Проверьте интернет-соединение и попробуйте снова."
    AppError.Timeout -> "Сервер долго отвечает" to "Не дождались ответа сервера. Попробуйте ещё раз."
    AppError.NotFound -> "Контент недоступен" to "Похоже, этот тайтл больше не в каталоге."
    AppError.Premium -> "Только для Premium" to "Для просмотра нужна активная подписка."
    AppError.Region -> "Недоступно в регионе" to "Контент ограничен лицензией в вашей стране."
    AppError.Auth -> "Сессия истекла" to "Войдите снова, чтобы продолжить просмотр."
    AppError.Playback -> "Ошибка воспроизведения" to "Не удалось запустить видео. Попробуйте другое качество."
    AppError.Server, AppError.Empty -> "Что-то пошло не так" to "Сбой на сервере. Попробуйте ещё раз через минуту."
}

private val ErrorCardMaxWidth = 520.dp
