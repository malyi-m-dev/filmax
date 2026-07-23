package com.filmax.feature.player.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvFocus
import com.filmax.core.tv.designsystem.TvFocusHalo
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnAccent
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.feature.player.common.formatPlayerTime
import kotlin.math.roundToInt

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

/** Нижний блок: скраббер, подсказки транспорта и ряд настроек. */
@Composable
private fun PlayerTransport(ui: TvPlayerUiState, menu: PlayerActions, modifier: Modifier = Modifier) {
    val hint = when {
        SettingsAction.Episodes in menu.items -> "↓ настройки и серии · ↕ показать прогресс"
        menu.items.isNotEmpty() -> "↓ настройки · ↕ показать прогресс"
        else -> "↕ показать прогресс"
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = TvMetrics.SafeHorizontal)
            .padding(bottom = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Основные контролы (скраббер + пауза/перемотка) прижаты к низу. Ряд настроек — ПОД ними,
        // за AnimatedVisibility: в транспорте его нет вовсе (место не держит), а по «вниз» он
        // раскрывается снизу и толкает основные контролы вверх. Так «вниз» открывает настройки, а
        // не приходится жать «вниз», потом несколько раз «вверх».
        Scrubber(
            positionMs = if (ui.isScrubbing) ui.scrubTargetMs else ui.positionMs,
            durationMs = ui.durationMs,
            active = ui.isScrubbing,
            modifier = Modifier.fillMaxWidth(),
        )
        TransportHints(
            isPlaying = ui.isPlaying,
            // Виртуальный фокус транспорта: пока не перематываем и не в настройках — «работаем»
            // с кнопкой паузы; при перемотке фокус-кольцо переезжает на thumb скраббера.
            focused = ui.mode == PlayerMode.Transport && !ui.isScrubbing,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            hint,
            style = MaterialTheme.typography.labelSmall,
            color = TvOnSurfaceDim,
            modifier = Modifier.padding(top = 12.dp),
        )
        AnimatedVisibility(
            visible = ui.mode == PlayerMode.Settings,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            SettingsBar(
                ui = ui,
                menu = menu,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

/** Полоса с временем: слева — текущее, справа — длительность; обе цифры табличные, чтобы не дёргались. */
@Composable
private fun Scrubber(positionMs: Long, durationMs: Long, active: Boolean, modifier: Modifier = Modifier) {
    val timeStyle = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            formatPlayerTime(positionMs),
            style = timeStyle,
            color = TvOnSurface,
            modifier = Modifier.widthIn(min = 56.dp),
        )
        ScrubTrack(
            fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            active = active,
        )
        Text(
            formatPlayerTime(durationMs),
            style = timeStyle,
            color = TvOnSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 56.dp),
        )
    }
}

/**
 * Трек, заливка и thumb. Ширина известна только на месте — от неё считается позиция thumb.
 * При скраббинге ([active]) полоса и thumb заметно вырастают: видно, что перемотка «взята в руки».
 */
@Composable
private fun RowScope.ScrubTrack(fraction: Float, active: Boolean) {
    val trackHeight by animateDpAsState(if (active) ScrubTrackHeightActive else ScrubTrackHeight, label = "scrubTrack")
    val thumbSize by animateDpAsState(if (active) ScrubThumbActive else ScrubThumb, label = "scrubThumb")
    val haloSize by animateDpAsState(if (active) ScrubThumbHaloActive else ScrubThumbHalo, label = "scrubHalo")
    BoxWithConstraints(
        Modifier
            .weight(1f)
            .height(ScrubThumbHaloActive),
    ) {
        val density = LocalDensity.current
        val trackPx = with(density) { maxWidth.toPx() }

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(TvAccent.copy(alpha = 0.2f)),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraction)
                .height(trackHeight)
                .clip(CircleShape)
                .background(TvAccent),
        )
        // Кольцо фокуса при перемотке, тёмный ореол и сам thumb — тремя концентрическими кругами:
        // белая точка на светлом кадре без ореола теряется.
        val ringSize = haloSize + ScrubFocusRingExtra
        val ringPx = with(density) { ringSize.toPx() }
        CircleBox(
            size = ringSize,
            color = if (active) TvFocus else TvFocus.copy(alpha = 0f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((fraction * trackPx - ringPx / 2f).roundToInt(), 0) },
        ) {
            CircleBox(size = haloSize, color = TvFocusHalo) {
                CircleBox(size = thumbSize, color = TvAccent)
            }
        }
    }
}

/**
 * Подсказки транспорта. Это именно подсказки, а не кнопки: перемотку и паузу ведёт D-pad,
 * фокусу тут ходить не по чему. [focused] — виртуальный фокус транспорта на кнопке OK:
 * белое кольцо с тёмным зазором (белая рамка на белой кнопке иначе не видна, как у TvButton).
 */
@Composable
private fun TransportHints(isPlaying: Boolean, focused: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeekHint(label = "−${SEEK_STEPS_SEC.first()} с")
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            CircleBox(size = PauseFocusOuter, color = if (focused) TvFocus else TvFocus.copy(alpha = 0f)) {
                CircleBox(size = PauseFocusInner, color = if (focused) TvFocusHalo else TvFocusHalo.copy(alpha = 0f)) {
                    CircleBox(size = PauseButtonSize, color = TvAccent) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = TvOnAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Text(
                if (isPlaying) "OK — пауза" else "OK — смотреть",
                style = MaterialTheme.typography.labelSmall,
                color = TvOnSurfaceVariant,
            )
        }
        SeekHint(label = "+${SEEK_STEPS_SEC.first()} с")
    }
}

/** Базовый шаг перемотки в кольце: с разгона реальный шаг показывает подпись по центру кадра. */
@Composable
private fun SeekHint(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .border(1.dp, TvSurfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("${SEEK_STEPS_SEC.first()}", style = MaterialTheme.typography.labelLarge, color = TvOnSurface)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = TvOnSurfaceVariant)
    }
}

/**
 * Ряд настроек. Всплывает по «вниз» через AnimatedVisibility над основными контролами; в транспорте
 * его нет вовсе, поэтому место под себя он не держит и низ панели не прыгает.
 *
 * Чипы намеренно не фокусируемые: в плеере фокус никуда не ходит, курсор ведёт обработчик клавиш.
 * Курсор рисуют белая заливка (`selected`) И увеличение — на ярком кадре одной заливки мало, чтобы
 * сразу читалось, что выбрано. `onClick` остаётся настоящим: у TV-Surface он обслуживает
 * accessibility-действие «активировать».
 */
@Composable
private fun SettingsBar(ui: TvPlayerUiState, menu: PlayerActions, modifier: Modifier = Modifier) {
    // fillMaxWidth + центрирующая раскладка: иначе ряд центрируется лишь по обёртке AnimatedVisibility
    // и съезжает вбок. Так баблы всегда по центру снизу, сколько бы их ни было.
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        menu.items.forEachIndexed { index, action ->
            val isCursor = index == ui.settingsCursor
            TvChip(
                label = action.label,
                selected = isCursor,
                onClick = {
                    ui.settingsCursor = index
                    ui.activate(action, menu)
                },
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    // Чип-курсор поверх соседей: увеличенный масштабом чип иначе уходил ПОД
                    // следующий по порядку отрисовки.
                    .zIndex(if (isCursor) 1f else 0f)
                    .scale(if (isCursor) CURSOR_CHIP_SCALE else 1f),
            )
        }
    }
}

/**
 * Поповер выбора: галочка стоит у текущего значения, подсветка — у курсора, и курсор при открытии
 * встаёт на текущее значение (см. [TvPlayerUiState.activate]).
 */
@Composable
private fun SettingsPopover(
    action: SettingsAction,
    menu: PlayerActions,
    cursor: Int,
    modifier: Modifier = Modifier,
) {
    val options = menu.options(action)
    val current = menu.selected(action)
    Column(
        modifier
            .width(PopoverWidth)
            .playerPanel()
            .padding(12.dp),
    ) {
        TvOverline(action.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        options.forEachIndexed { index, option ->
            SettingsRow(label = option, highlighted = index == cursor, current = option == current)
        }
    }
}

/** [highlighted] — под курсором, [current] — выбранное сейчас значение. Это разные вещи. */
@Composable
private fun SettingsRow(label: String, highlighted: Boolean, current: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(MaterialTheme.shapes.small)
            // Курсор — сплошная белая заливка (акцент), а не еле заметный серый: сразу видно, на чём
            // стоишь. Выбранное сейчас значение помечает галочка независимо от положения курсора.
            .background(if (highlighted) TvAccent else TvSurface.copy(alpha = 0f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (highlighted) TvOnAccent else TvOnSurfaceVariant,
        )
        if (current) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (highlighted) TvOnAccent else TvAccent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Прозрачность плавающих панелей: кадр просвечивает, текст остаётся читаемым. */
private const val PANEL_ALPHA = 0.85f

/** Увеличение чипа-курсора в ряду настроек — белой заливки на ярком кадре мало для читаемости выбора. */
private const val CURSOR_CHIP_SCALE = 1.3f

private val PopoverWidth = 260.dp
private val AutoNextCardMaxWidth = 460.dp
private val SubscriptionCardMaxWidth = 480.dp

private val ScrubTrackHeight = 6.dp
private val ScrubTrackHeightActive = 9.dp
private val ScrubThumb = 15.dp
private val ScrubThumbActive = 24.dp
private val ScrubThumbHalo = 24.dp
private val ScrubThumbHaloActive = 38.dp

/** Насколько кольцо фокуса при перемотке шире тёмного ореола thumb. */
private val ScrubFocusRingExtra = 6.dp

/** Кнопка OK и кольца её виртуального фокуса: белое снаружи и тёмный зазор вокруг круга кнопки. */
private val PauseButtonSize = 50.dp
private val PauseFocusOuter = 62.dp
private val PauseFocusInner = 56.dp
