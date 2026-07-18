package com.filmax.feature.player.tv

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvChip
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
import com.filmax.feature.player.common.PlaybackSpeeds
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerScreenModel
import com.filmax.feature.player.common.PlayerState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/** Что сейчас ведёт D-pad: транспорт (пауза/перемотка) или ряд настроек под скраббером. */
private enum class PlayerMode { Transport, Settings }

/** Пункт ряда настроек. Первые четыре открывают поповер выбора, [NextEpisode] — действие сразу. */
private enum class SettingsAction(val label: String) {
    Quality("Качество"),
    Audio("Аудио"),
    Subtitle("Субтитры"),
    Speed("Скорость"),
    NextEpisode("Следующая серия"),
}

/**
 * Ряд настроек в терминах текущего кадра композиции: что показываем и что делать по OK.
 * Передаётся обработчику клавиш параметром — [TvPlayerUiState] про [PlayerState] ничего не знает.
 */
private class PlayerActions(
    val items: List<SettingsAction>,
    val options: (SettingsAction) -> List<String>,
    val selectedIndex: (SettingsAction) -> Int,
    val onSelect: (SettingsAction, String) -> Unit,
    val onNextEpisode: () -> Unit,
)

/**
 * Состояние оверлея и вся раскладка пульта.
 *
 * Плеер — единственный экран, где D-pad НЕ ходит по фокусу: стрелки это транспорт, а не навигация
 * (правило Google, training/tv/playback/controls). Поэтому «курсор» по чипам и по списку поповера
 * эмулируется индексами, а клавиши разбирает один обработчик: пока открыт поповер, всё уходит
 * в него, и панель не может «зависнуть», когда фокус ушёл мимо списка.
 */
@Stable
private class TvPlayerUiState(val player: Player) {

    /** Виден ли оверлей поверх кадра. */
    var visible by mutableStateOf(true)
    var mode by mutableStateOf(PlayerMode.Transport)

    /** Открытая категория поповера выбора; null — поповера нет. */
    var submenu by mutableStateOf<SettingsAction?>(null)
    var settingsCursor by mutableIntStateOf(0)
    var submenuCursor by mutableIntStateOf(0)

    /** Индикатор шага последней перемотки («+30 с»); гаснет сам. */
    var seekLabel by mutableStateOf<String?>(null)

    /** Зеркало player.isPlaying: сам плеер не Compose-state и рекомпозицию не вызывает. */
    var isPlaying by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)

    /** Скраббинг: позиция, которую двигает D-pad до подтверждения seekTo. */
    var isScrubbing by mutableStateOf(false)
    var scrubTargetMs by mutableLongStateOf(0L)

    /** Счётчики нажатий — только чтобы перезапускать таймеры автоскрытия и индикатора шага. */
    var interactionTick by mutableIntStateOf(0)
    var seekTick by mutableIntStateOf(0)

    /** Длина текущей серии быстрых нажатий перемотки и время последнего из них. */
    private var seekStreak = 0
    private var lastSeekAtMs = 0L

    /**
     * Оверлей уходит сам, только пока идёт воспроизведение и пользователь не в меню: на паузе
     * и в настройках он остаётся на экране (правило armHide из макета).
     */
    val idleHidesOverlay: Boolean
        get() = visible && isPlaying && mode == PlayerMode.Transport && submenu == null

    /** Было действие пользователя: оверлей на экран, таймер автоскрытия — с нуля. */
    fun touch() {
        visible = true
        interactionTick++
    }

    /**
     * Раскладка D-pad — дословно по гайдлайну Google: Center — пауза/воспроизведение, Left/Right —
     * перемотка (состояние play/pause при этом НЕ меняется), Up/Down — «peek»: показывают прогресс,
     * не ставя на паузу. Неизвестные клавиши не трогаем — иначе съедим громкость и системные.
     */
    fun onKey(key: Key, menu: PlayerActions): Boolean = when {
        submenu != null -> onSubmenuKey(key, menu)
        mode == PlayerMode.Settings -> onSettingsKey(key, menu)
        else -> onTransportKey(key, menu)
    }

    // Три выхода вместо двух: ветка «клавиша не наша» обязана вернуть false, иначе плеер
    // проглотит громкость и системные кнопки. Разворачивать в единый выход — только запутать.
    @Suppress("ReturnCount")
    private fun onSubmenuKey(key: Key, menu: PlayerActions): Boolean {
        val category = submenu ?: return false
        val options = menu.options(category)
        when (key) {
            Key.DirectionUp -> submenuCursor = (submenuCursor - 1).coerceAtLeast(0)
            Key.DirectionDown -> submenuCursor = (submenuCursor + 1).coerceAtMost(options.lastIndex)
            Key.DirectionCenter, Key.Enter -> {
                options.getOrNull(submenuCursor)?.let { option -> menu.onSelect(category, option) }
                submenu = null
            }
            // Горизонталь при открытом поповере глушим: иначе стрелка улетела бы в перемотку.
            Key.DirectionLeft, Key.DirectionRight -> Unit
            else -> return false
        }
        touch()
        return true
    }

    private fun onSettingsKey(key: Key, menu: PlayerActions): Boolean {
        when (key) {
            Key.DirectionLeft -> settingsCursor = (settingsCursor - 1).coerceAtLeast(0)
            Key.DirectionRight -> settingsCursor = (settingsCursor + 1).coerceAtMost(menu.items.lastIndex)
            Key.DirectionUp -> mode = PlayerMode.Transport
            Key.DirectionCenter, Key.Enter -> menu.items.getOrNull(settingsCursor)?.let { activate(it, menu) }
            // Ниже чипов ничего нет, но клавишу гасим: непрочитанная стрелка запустит поиск фокуса,
            // а любой ушедший фокус — это ровно тот баг, из-за которого панель зависала открытой.
            Key.DirectionDown -> Unit
            else -> return false
        }
        touch()
        return true
    }

    private fun onTransportKey(key: Key, menu: PlayerActions): Boolean {
        when (key) {
            Key.DirectionLeft, Key.MediaRewind -> scrub(-1)
            Key.DirectionRight, Key.MediaFastForward -> scrub(1)
            Key.DirectionCenter, Key.Enter, Key.MediaPlayPause -> togglePlay()
            Key.DirectionDown -> {
                if (menu.items.isNotEmpty()) mode = PlayerMode.Settings
                settingsCursor = 0
                seekLabel = null
                touch()
            }
            Key.DirectionUp -> {
                seekLabel = null
                touch()
            }
            else -> return false
        }
        return true
    }

    fun activate(action: SettingsAction, menu: PlayerActions) {
        when (action) {
            SettingsAction.NextEpisode -> menu.onNextEpisode()
            else -> {
                submenu = action
                submenuCursor = menu.selectedIndex(action)
            }
        }
        touch()
    }

    /**
     * «Назад» закрывает ровно один слой и не более: поповер → ряд настроек → выход. Скрытие
     * оверлея слоем НЕ считается — прятать за ним выход значило бы четыре нажатия вместо одного.
     * Возвращает false, если закрывать больше нечего (тогда экран выходит из плеера).
     */
    fun back(): Boolean = when {
        submenu != null -> {
            submenu = null
            touch()
            true
        }
        mode == PlayerMode.Settings -> {
            mode = PlayerMode.Transport
            touch()
            true
        }
        else -> false
    }

    /**
     * Шаг перемотки с разгоном: пока нажатия идут чаще [SEEK_STREAK_WINDOW_MS], шаг берётся
     * следующим по лестнице [SEEK_STEPS_SEC]. Двухчасовой фильм так перематывается за десяток
     * нажатий, а не за 360. Часы монотонные: системное время может прыгнуть и сорвать серию.
     */
    private fun scrub(direction: Int) {
        val duration = durationMs
        if (duration <= 0) return
        val now = SystemClock.uptimeMillis()
        seekStreak = if (now - lastSeekAtMs < SEEK_STREAK_WINDOW_MS) seekStreak + 1 else 0
        lastSeekAtMs = now
        val stepSec = SEEK_STEPS_SEC[seekStreak.coerceAtMost(SEEK_STEPS_SEC.lastIndex)]

        if (!isScrubbing) {
            scrubTargetMs = positionMs
            isScrubbing = true
        }
        scrubTargetMs = (scrubTargetMs + direction * stepSec * MILLIS_IN_SECOND).coerceIn(0L, duration)
        // Подпись показывает шаг именно этого нажатия — на разгоне «10 с» на кнопке было бы враньём.
        seekLabel = formatSeekLabel(direction, stepSec)
        seekTick++
        touch()
    }

    /**
     * Подтверждение перемотки. seekTo на каждое нажатие рвал бы HLS-буфер, поэтому позицию
     * ведём в состоянии и уходим на неё по паузе в нажатиях.
     */
    fun commitScrub() {
        if (!isScrubbing) return
        player.seekTo(scrubTargetMs)
        positionMs = scrubTargetMs
        isScrubbing = false
    }

    private fun togglePlay() {
        // Незакоммиченный скраб не теряем: сначала уходим на выбранную позицию, потом переключаем.
        commitScrub()
        if (player.isPlaying) player.pause() else player.play()
        seekLabel = null
        touch()
    }
}

/**
 * TV-Плеер: видеоповерхность ExoPlayer и оверлей под пульт поверх неё.
 *
 * [videoId] — эпизод из маршрута, нужен для подстроки «Сезон 2 · Серия 5» и «Следующей серии».
 * [onPlayEpisode] задаёт граф навигации: следующая серия — это новый экран плеера с новым
 * [PlayerScreenModel], а не подмена MediaItem (иначе прогресс писался бы в предыдущую серию).
 */
@Composable
fun TvPlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    videoId: Int = -1,
    onPlayEpisode: ((videoId: Int) -> Unit)? = null,
    screenModel: PlayerScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val ui = remember(screenModel.player) { TvPlayerUiState(screenModel.player) }

    // Играющий трек ищем так же, как модель: нет совпадения по videoId — это фильм/первый трек.
    val tracks = state.item?.tracklist.orEmpty()
    val trackIndex = tracks.indexOfFirst { it.id == videoId }.coerceAtLeast(0)
    val track = tracks.getOrNull(trackIndex)
    val nextTrack = tracks.getOrNull(trackIndex + 1)

    val subtitle = when {
        track == null -> ""
        track.seasonNumber > 0 -> "Сезон ${track.seasonNumber} · Серия ${track.number}"
        tracks.size > 1 -> "Серия ${track.number}"
        else -> listOfNotNull(state.item?.year?.takeIf { it > 0 }?.toString(), state.currentQuality)
            .joinToString(" · ")
    }

    // Настройку показываем только там, где реально есть из чего выбрать.
    val menu = PlayerActions(
        items = buildList {
            if (state.qualities.size > 1) add(SettingsAction.Quality)
            if (state.audioTracks.size > 1) add(SettingsAction.Audio)
            if (state.subtitles.size > 1) add(SettingsAction.Subtitle)
            // Скорость доступна всегда — набор фиксированный, выбирать есть из чего.
            add(SettingsAction.Speed)
            if (nextTrack != null && onPlayEpisode != null) add(SettingsAction.NextEpisode)
        },
        options = { action -> action.options(state) },
        selectedIndex = { action -> action.options(state).indexOf(action.selected(state)).coerceAtLeast(0) },
        onSelect = { action, label -> action.toEvent(label)?.let(screenModel::dispatch) },
        onNextEpisode = { nextTrack?.let { next -> onPlayEpisode?.invoke(next.id) } },
    )

    PlayerEffects(ui = ui, screenModel = screenModel)
    BackHandler { if (!ui.back()) onBack() }

    PlayerContent(ui = ui, state = state, menu = menu, subtitle = subtitle, modifier = modifier)
}

/** Тики и таймеры плеера: прогресс/SaveProgress, подтверждение скраба, автоскрытие оверлея. */
@Composable
private fun PlayerEffects(ui: TvPlayerUiState, screenModel: PlayerScreenModel) {
    val player = screenModel.player

    DisposableEffect(player) {
        ui.isPlaying = player.isPlaying
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                ui.isPlaying = isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Тик прогресса и сохранения позиции. Пока длительность неизвестна — не сохраняем: записали бы
    // нулевую позицию поверх реального прогресса. Во время скраббинга позицию ведёт пульт.
    LaunchedEffect(player) {
        while (true) {
            delay(PROGRESS_TICK_MS)
            val duration = player.duration.takeIf { it > 0 } ?: continue
            ui.durationMs = duration
            if (!ui.isScrubbing) ui.positionMs = player.currentPosition
            screenModel.dispatch(PlayerEvent.SaveProgress(player.currentPosition))
        }
    }

    LaunchedEffect(ui.isScrubbing, ui.scrubTargetMs) {
        if (ui.isScrubbing) {
            delay(SCRUB_COMMIT_TIMEOUT_MS)
            ui.commitScrub()
        }
    }

    // Индикатор шага — про последнее нажатие, а не про состояние: живёт мгновение и гаснет.
    LaunchedEffect(ui.seekTick) {
        delay(SEEK_LABEL_HOLD_MS)
        ui.seekLabel = null
    }

    LaunchedEffect(ui.interactionTick, ui.idleHidesOverlay) {
        if (ui.idleHidesOverlay) {
            delay(OVERLAY_AUTO_HIDE_MS)
            ui.visible = false
            ui.seekLabel = null
        }
    }
}

/**
 * Кадр и оверлей. Фокусируемый узел на экране ровно один — корневой Box: он и держит фокус,
 * и разбирает клавиши, поэтому пульт работает одинаково при видимом и скрытом оверлее.
 */
@Composable
private fun PlayerContent(
    ui: TvPlayerUiState,
    state: PlayerState,
    menu: PlayerActions,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val keyFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { keyFocus.requestFocus() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface)
            .focusRequester(keyFocus)
            .focusable()
            .onKeyEvent { event ->
                when {
                    event.type != KeyEventType.KeyDown -> false
                    // «Назад» не перехватываем: он весь в BackHandler, иначе система не увидит KeyUp.
                    event.key == Key.Back || event.key == Key.Escape -> false
                    else -> ui.onKey(event.key, menu)
                }
            },
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).also { view ->
                    view.player = ui.player
                    view.useController = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.loading) {
            CircularProgressIndicator(color = TvAccent, modifier = Modifier.align(Alignment.Center))
        }

        AnimatedVisibility(
            visible = ui.visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            PlayerOverlay(ui = ui, state = state, menu = menu, subtitle = subtitle)
        }
    }
}

/** Слои оверлея: затемнение, шапка, индикатор шага, транспорт снизу и поповер выбора. */
@Composable
private fun PlayerOverlay(
    ui: TvPlayerUiState,
    state: PlayerState,
    menu: PlayerActions,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                // Затемнение только там, где лежит текст: сверху под шапкой и снизу под транспортом.
                Brush.verticalGradient(
                    0f to TvSurface.copy(alpha = 0.55f),
                    0.45f to TvSurface.copy(alpha = 0f),
                    0.70f to TvSurface.copy(alpha = 0.35f),
                    1f to TvSurface.copy(alpha = 0.92f),
                )
            ),
    ) {
        PlayerTopBar(
            title = state.item?.title.orEmpty(),
            subtitle = subtitle,
            modifier = Modifier.align(Alignment.TopStart),
        )

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

        ui.submenu?.let { category ->
            SettingsPopover(
                action = category,
                state = state,
                cursor = ui.submenuCursor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = TvMetrics.SafeHorizontal, bottom = PopoverBottom),
            )
        }
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
        menu.items.contains(SettingsAction.NextEpisode) -> "↓ настройки и следующая серия · ↕ показать прогресс"
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
        TransportHints(isPlaying = ui.isPlaying, modifier = Modifier.padding(top = 16.dp))
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
            formatMs(positionMs),
            style = timeStyle,
            color = TvOnSurface,
            modifier = Modifier.widthIn(min = 56.dp),
        )
        ScrubTrack(
            fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            active = active,
        )
        Text(
            formatMs(durationMs),
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
        val haloPx = with(density) { haloSize.toPx() }

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
        // Ореол вокруг thumb: белая точка на светлом кадре иначе теряется.
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((fraction * trackPx - haloPx / 2f).roundToInt(), 0) }
                .size(haloSize)
                .clip(CircleShape)
                .background(TvFocusHalo),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(TvAccent),
            )
        }
    }
}

/**
 * Подсказки транспорта. Это именно подсказки, а не кнопки: перемотку и паузу ведёт D-pad,
 * фокусу тут ходить не по чему.
 */
@Composable
private fun TransportHints(isPlaying: Boolean, modifier: Modifier = Modifier) {
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
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(TvAccent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = TvOnAccent,
                    modifier = Modifier.size(22.dp),
                )
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
    state: PlayerState,
    cursor: Int,
    modifier: Modifier = Modifier,
) {
    val options = action.options(state)
    val current = action.selected(state)
    Column(
        modifier
            .width(260.dp)
            .clip(TvMetrics.PanelShape)
            .background(TvSurfaceContainer.copy(alpha = 0.97f))
            .border(1.dp, TvSurfaceContainerHighest, TvMetrics.PanelShape)
            .padding(12.dp),
    ) {
        TvOverline(action.menuTitle, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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

/** Заголовок поповера: у «Аудио» он длиннее, чем подпись чипа. */
private val SettingsAction.menuTitle: String
    get() = when (this) {
        SettingsAction.Quality -> "Качество"
        SettingsAction.Audio -> "Аудиодорожка"
        SettingsAction.Subtitle -> "Субтитры"
        SettingsAction.Speed -> "Скорость"
        SettingsAction.NextEpisode -> ""
    }

private fun SettingsAction.options(state: PlayerState): List<String> = when (this) {
    SettingsAction.Quality -> state.qualities.map { it.label }
    SettingsAction.Audio -> state.audioTracks.map { it.label }
    SettingsAction.Subtitle -> state.subtitles.map { it.label }
    SettingsAction.Speed -> PlaybackSpeeds.labels
    SettingsAction.NextEpisode -> emptyList()
}

private fun SettingsAction.selected(state: PlayerState): String = when (this) {
    SettingsAction.Quality -> state.currentQuality.orEmpty()
    SettingsAction.Audio -> state.currentAudio
    SettingsAction.Subtitle -> state.currentSubtitle
    SettingsAction.Speed -> PlaybackSpeeds.labelFor(state.currentSpeed)
    SettingsAction.NextEpisode -> ""
}

private fun SettingsAction.toEvent(label: String): PlayerEvent? = when (this) {
    SettingsAction.Quality -> PlayerEvent.SelectQuality(label)
    SettingsAction.Audio -> PlayerEvent.SelectAudio(label)
    SettingsAction.Subtitle -> PlayerEvent.SelectSubtitle(label)
    SettingsAction.Speed -> PlaybackSpeeds.valueFor(label)?.let { PlayerEvent.SetSpeed(it) }
    SettingsAction.NextEpisode -> null
}

private fun formatSeekLabel(direction: Int, stepSec: Int): String =
    if (direction > 0) "+$stepSec с" else "−$stepSec с"

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** Тик прогресса и сохранения позиции (мс). Ровно секунда: на нём же держится SaveProgress. */
private const val PROGRESS_TICK_MS = 1000L

/** Бездействие, после которого оверлей уходит с кадра. */
private const val OVERLAY_AUTO_HIDE_MS = 4200L

/** Пауза в нажатиях, после которой скраббинг подтверждается seekTo. */
private const val SCRUB_COMMIT_TIMEOUT_MS = 700L

/** Сколько держится индикатор шага перемотки после последнего нажатия. */
private const val SEEK_LABEL_HOLD_MS = 800L

/** Пауза между нажатиями, которая сбрасывает разгон перемотки в начало лестницы. */
private const val SEEK_STREAK_WINDOW_MS = 450L

private const val MILLIS_IN_SECOND = 1000L

/** Увеличение чипа-курсора в ряду настроек — белой заливки на ярком кадре мало для читаемости выбора. */
private const val CURSOR_CHIP_SCALE = 1.3f

/** Лестница разгона перемотки (секунды): шаг растёт, пока пользователь давит стрелку. */
private val SEEK_STEPS_SEC = listOf(10, 10, 20, 30, 60, 90, 120)

private val ScrubTrackHeight = 6.dp
private val ScrubTrackHeightActive = 9.dp
private val ScrubThumb = 15.dp
private val ScrubThumbActive = 24.dp
private val ScrubThumbHalo = 24.dp
private val ScrubThumbHaloActive = 38.dp
private val PopoverBottom = 120.dp
