package com.filmax.feature.player.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvFocusHalo
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest

/**
 * Единый стиль плавающих панелей плеера (поповер, панель серий, плашки): полупрозрачная
 * подложка — кадр просвечивает, но текст остаётся читаемым.
 */
internal fun Modifier.playerPanel(): Modifier = this
    .clip(TvMetrics.PanelShape)
    .background(TvSurfaceContainer.copy(alpha = PANEL_ALPHA))
    .border(1.dp, TvSurfaceContainerHighest.copy(alpha = PANEL_ALPHA), TvMetrics.PanelShape)

/** Круг с содержимым по центру — из таких слоёв собраны кнопка паузы и thumb скраббера. */
@Composable
internal fun CircleBox(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Слои оверлея: затемнение, шапка, индикатор шага, транспорт снизу и поповер выбора. */
@Composable
internal fun PlayerOverlay(
    ui: TvPlayerUiState,
    menu: PlayerActions,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                // Затемнение только там, где лежит текст: сверху под шапкой и снизу под транспортом.
                Brush.verticalGradient(
                    0f to TvSurface.copy(alpha = 0.45f),
                    0.45f to TvSurface.copy(alpha = 0f),
                    0.70f to TvSurface.copy(alpha = 0.25f),
                    1f to TvSurface.copy(alpha = 0.85f),
                )
            ),
    ) {
        PlayerTopBar(title = title, subtitle = subtitle, modifier = Modifier.align(Alignment.TopStart))

        ui.seekLabel?.let { label ->
            Text(
                label,
                style = MaterialTheme.typography.headlineMedium.copy(
                    // Тень — единственное, что держит белую подпись на светлом кадре.
                    shadow = Shadow(color = TvFocusHalo, offset = Offset(0f, 2f), blurRadius = 20f),
                ),
                color = TvAccent,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        PlayerTransport(ui = ui, menu = menu, modifier = Modifier.align(Alignment.BottomCenter))

        // Поповер выбора — по центру кадра: у края он терялся, взгляд при выборе смотрит в центр.
        ui.submenu?.let { category ->
            SettingsPopover(
                action = category,
                menu = menu,
                cursor = ui.submenuCursor,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Панель серий — ровно по центру экрана, как и поповеры: у края она терялась.
        if (ui.episodesOpen) {
            menu.episodes?.let { panel ->
                EpisodesPanel(
                    panel = panel,
                    seasonCursor = ui.episodesSeasonCursor,
                    episodeCursor = ui.episodesCursor,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 20.dp),
                )
            }
        }
    }
}

/** Плашка «Дальше: серия N» с отсчётом. OK — сразу, «Назад» — отмена (см. onKey/back). */
@Composable
internal fun AutoNextCard(label: String, seconds: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .widthIn(max = AutoNextCardMaxWidth)
            .playerPanel()
            .padding(horizontal = 18.dp, vertical = 13.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = TvOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "Автостарт через $seconds с · OK — сейчас · «Назад» — отмена",
            style = MaterialTheme.typography.labelSmall,
            color = TvOnSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Плашка «нужна подписка»: без неё kino.pub не отдаст поток, и экран остался бы просто чёрным. */
@Composable
internal fun SubscriptionCard(modifier: Modifier = Modifier) {
    Column(
        modifier
            .widthIn(max = SubscriptionCardMaxWidth)
            .playerPanel()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Нужна подписка",
            style = MaterialTheme.typography.titleMedium,
            color = TvOnSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            "Просмотр доступен только с активной подпиской — оформите её в аккаунте kino.pub",
            style = MaterialTheme.typography.bodySmall,
            color = TvOnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Шапка: название с подстрокой слева, цена выхода справа — «Назад» выходит из плеера сразу. */
@Composable
private fun PlayerTopBar(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = TvMetrics.SafeHorizontal)
            .padding(top = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = TvOnSurface)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvOnSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .border(1.dp, TvSurfaceContainerHighest, MaterialTheme.shapes.extraSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", style = MaterialTheme.typography.bodySmall, color = TvOnSurfaceVariant)
            }
            Text("Назад — выход", style = MaterialTheme.typography.bodySmall, color = TvOnSurfaceVariant)
        }
    }
}

/** Прозрачность плавающих панелей: кадр просвечивает, текст остаётся читаемым. */
internal const val PANEL_ALPHA = 0.85f

private val AutoNextCardMaxWidth = 460.dp
private val SubscriptionCardMaxWidth = 480.dp
