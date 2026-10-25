// Шапка деталей — общая у фильма и сериала: надзаголовок, название, оценки с метой и описание.
package com.filmax.feature.details.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvDescription
import com.filmax.core.tv.designsystem.TvRatingBadge
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.feature.details.common.DetailsHeading

/**
 * Кегли шапки. У фильма шапка — весь первый экран, у сериала — колонка рядом с панелью серий,
 * поэтому всё на ступень мельче: название, мета и описание.
 */
internal data class HeadingStyle(val title: TextStyle, val meta: TextStyle, val description: TextStyle, val metaGap: Dp)

/** Трекинг крупного названия: на 48sp обычный трекинг Onest разваливает слово. */
private val TitleTracking = (-0.025).em

internal val MovieHeadingStyle = HeadingStyle(
    title = FxType.hero.copy(fontSize = 48.sp, lineHeight = 48.sp, letterSpacing = TitleTracking),
    meta = FxType.metaRow,
    description = FxType.body,
    metaGap = 6.dp,
)

internal val SeriesHeadingStyle = HeadingStyle(
    title = FxType.hero.copy(fontSize = 42.sp, lineHeight = 43.sp, letterSpacing = TitleTracking),
    meta = FxType.meta,
    description = FxType.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
    metaGap = 4.dp,
)

/** Что шапке нужно от экрана: открыть полное описание и куда вернуть фокус, когда панель закроется. */
internal data class HeadingDescription(val onOpen: () -> Unit, val focus: FocusRequester)

/**
 * Надзаголовок, название, оценки и описание — появляются по очереди ([fxEnter]). Описание
 * фокусируется, только если не влезло в три строки; «ОК» открывает полный текст в панели.
 */
@Composable
internal fun DetailsHeading(
    item: Item,
    heading: DetailsHeading,
    description: HeadingDescription,
    style: HeadingStyle,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFxColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            heading.overline.uppercase(),
            style = FxType.overline,
            color = colors.accentText,
            modifier = Modifier.fxEnter(0)
        )
        Text(
            item.title,
            style = style.title,
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fxEnter(1),
        )
        RatingsRow(heading, style, Modifier.fxEnter(2))
        if (item.plot.isNotBlank()) {
            TvDescription(
                text = item.plot,
                onOpen = description.onOpen,
                style = style.description,
                modifier = Modifier.focusRequester(description.focus).fxEnter(3),
            )
        }
    }
}

/** Бейджи оценок и мета рядом: «7.9 КиноПоиск  7.6 IMDb  Фэнтези, Приключения · 2 ч 11 мин». */
@Composable
private fun RatingsRow(heading: DetailsHeading, style: HeadingStyle, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        heading.ratingBadges.forEach { badge ->
            TvRatingBadge(badge.value, badge.source, highlighted = badge.highlighted)
        }
        if (heading.metaLine.isNotBlank()) {
            Text(
                heading.metaLine,
                style = style.meta,
                color = LocalFxColors.current.textSecondary,
                modifier = Modifier.padding(start = style.metaGap)
            )
        }
    }
}
