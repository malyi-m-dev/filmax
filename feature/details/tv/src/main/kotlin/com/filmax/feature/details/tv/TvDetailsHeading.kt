// Шапка деталей — общая у фильма и сериала: надзаголовок, название, оценки с метой и описание.
package com.filmax.feature.details.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvDescription
import com.filmax.core.tv.designsystem.TvDescriptionInset
import com.filmax.core.tv.designsystem.TvRatingBadge
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.feature.details.common.SeriesData

/**
 * Надзаголовок, название, оценки и описание — появляются по очереди ([fxEnter]). Описание
 * фокусируется, только если не влезло в три строки; «ОК» открывает полный текст в панели.
 * [descriptionFocus] — куда вернуть фокус, когда панель закроется.
 */
@Composable
internal fun DetailsHeading(
    item: Item,
    series: SeriesData?,
    onOpenDescription: () -> Unit,
    descriptionFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFxColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            overline(item, series).uppercase(),
            style = FxType.overline,
            color = colors.accentText,
            modifier = Modifier.fxEnter(0)
        )
        Text(
            item.title,
            style = FxType.hero,
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fxEnter(1),
        )
        RatingsRow(item, series, Modifier.fxEnter(2))
        if (item.plot.isNotBlank()) {
            TvDescription(
                text = item.plot,
                onOpen = onOpenDescription,
                // Внутреннее поле описания уходит за левый край колонки: текст стоит в линию
                // с названием, а рамка фокуса — снаружи.
                modifier = Modifier
                    .offset(x = -TvDescriptionInset)
                    .focusRequester(descriptionFocus)
                    .fxEnter(3),
            )
        }
    }
}

/** Бейджи оценок и мета рядом: «7.9 КиноПоиск  7.6 IMDb  Фэнтези, Приключения · 2 ч 11 мин». */
@Composable
private fun RatingsRow(item: Item, series: SeriesData?, modifier: Modifier) {
    val meta = metaLine(item, series)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ratingBadges(item).forEach { badge ->
            TvRatingBadge(badge.value, badge.source, highlighted = badge.highlighted)
        }
        if (meta.isNotBlank()) {
            Text(
                meta,
                style = FxType.metaRow,
                color = LocalFxColors.current.textSecondary,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
