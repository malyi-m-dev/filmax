// Шапка Главной — логотип, «пульт телевизора», чипы разделов — и баннер «нет сети», которым
// открывается лента после hero. Сама лента — в HomeScreen.

package com.filmax.feature.home.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.GlassState
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.presentation.labels.sectionLabel
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxGlassHeader
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxListRow
import com.filmax.core.ui.components.FilmaxWordmark
import com.filmax.feature.home.common.HomeEvent

/** Разделы чипами под шапкой — самые частые; остальные — в шторке «Разделы». */
private val HeaderSections = listOf(CatalogSection.MOVIES, CatalogSection.SERIES, CatalogSection.ANIME)

/**
 * Шапка поверх ленты. [glassProgress] — сколько hero уехало под шапку: стекло проявляется по нему,
 * а затемнение под статус-баром, нужное только поверх кадра, — гаснет.
 */
@Composable
internal fun HomeHeader(glass: GlassState, glassProgress: () -> Float, onEvent: (HomeEvent) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 1f - glassProgress() }
                .background(TopFade),
        )
        FilmaxGlassHeader(
            glass = glass,
            modifier = Modifier.matchParentSize().graphicsLayer { alpha = glassProgress() }
        ) {}
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            HomeTopBar(onEvent)
            SectionChips(onEvent)
        }
    }
}

/** Логотип и кнопка телевизора: поиск и профиль теперь вкладки навигации. */
@Composable
private fun HomeTopBar(onEvent: (HomeEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopBarHeight)
            .padding(horizontal = FilmaxMetrics.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilmaxWordmark()
        Spacer(Modifier.weight(1f))
        FilmaxIconButton(
            Icons.Filled.Cast,
            contentDescription = "Пульт телевизора",
            onClick = { onEvent(HomeEvent.OpenTv) },
            size = FilmaxMetrics.IconButtonSmall,
        )
    }
}

@Composable
private fun SectionChips(onEvent: (HomeEvent) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = FilmaxMetrics.ScreenPadding)
            .padding(top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderSections.forEach { section ->
            FilmaxChip(
                label = sectionLabel(section),
                selected = false,
                onClick = { onEvent(HomeEvent.OpenSection(section)) },
            )
        }
        FilmaxChip(
            label = "Разделы",
            selected = false,
            onClick = { onEvent(HomeEvent.OpenSections) },
            trailingIcon = Icons.Filled.KeyboardArrowDown,
        )
    }
}

/** Ненавязчивый баннер «нет сети» над кэшированным контентом; тап — повторить загрузку (issue #42). */
@Composable
internal fun OfflineBanner(onReload: () -> Unit) {
    FilmaxListRow(
        title = "Нет сети — показаны сохранённые данные",
        subtitle = "Нажмите, чтобы повторить",
        icon = Icons.Filled.CloudOff,
        onClick = onReload,
        modifier = Modifier.padding(horizontal = FilmaxMetrics.GridPadding, vertical = 8.dp),
    )
}

private val TopBarHeight = 52.dp

/** Затемнение под статус-баром поверх кадра hero — без него часы и логотип пропадают на светлом кадре. */
private val TopFade = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))
