package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack

/**
 * Доступное качество потока. [urls] — варианты доставки в порядке предпочтения (hls4 → http → hls):
 * у kino.pub они ведут на РАЗНЫЕ CDN-хосты, и hls4 (api.srvkp.com) бывает недоступен из-за
 * DPI/SNI-блокировок. Плеер стартует с первого и при ошибке источника переключается на следующий;
 * mp4 (http) идёт раньше hls, потому что в нём все озвучки, а в hls — одна.
 */
data class StreamQuality(val label: String, val urls: List<String>) {
    init {
        require(urls.isNotEmpty()) { "Качество $label без единой ссылки на поток" }
    }

    /** Основная ссылка варианта — первая в порядке предпочтения. */
    val url: String get() = urls.first()
}

/** Вариант субтитров; [lang] == null означает «Выкл». */
data class SubtitleOption(val label: String, val lang: String?)

/**
 * Аудиодорожка потока. [groupIndex] — индекс аудиогруппы в Media3 `Tracks`: выбор идёт точечным
 * override, а не «предпочитаемым языком» — у тайтла бывает несколько русских озвучек разных
 * студий, и по языку они неотличимы.
 *
 * [label] — уникальная подпись «2. Русский · Многоголосый · LostFilm»: ею дорожка выбирается.
 * Остальное — те же метаданные `audios[]` по частям, чтобы ТВ разложил их по строке варианта:
 * [languageCode] — код языка как его прислал kino.pub (или манифест), [channels] — 0, если неизвестно.
 */
data class AudioOption(
    val label: String,
    val groupIndex: Int,
    val languageCode: String? = null,
    val voiceType: String? = null,
    val voiceAuthor: String? = null,
    val channels: Int = 0,
)

/** Вариант скорости воспроизведения: [value] уходит в ExoPlayer, [label] — на экран. */
data class SpeedOption(val label: String, val value: Float)

/**
 * Набор скоростей воспроизведения — единый для mobile и TV, чтобы список и подписи совпадали.
 * Скорость сессионная: между пересозданием плеера не сохраняется.
 */
object PlaybackSpeeds {
    const val NormalLabel = "Обычная"
    const val NormalSpeed = 1.0f

    val options: List<SpeedOption> = listOf(
        SpeedOption("0.25×", 0.25f),
        SpeedOption("0.5×", 0.5f),
        SpeedOption("0.75×", 0.75f),
        SpeedOption(NormalLabel, NormalSpeed),
        SpeedOption("1.25×", 1.25f),
        SpeedOption("1.5×", 1.5f),
        SpeedOption("1.75×", 1.75f),
        SpeedOption("2×", 2.0f),
    )

    /** Подписи для меню/поповера в порядке возрастания скорости. */
    val labels: List<String> = options.map { it.label }

    /** Подпись текущей скорости; неизвестное значение показываем как «Обычная». */
    fun labelFor(value: Float): String = options.firstOrNull { it.value == value }?.label ?: NormalLabel

    /** Значение скорости по подписи из меню; null — подписи нет в наборе. */
    fun valueFor(label: String): Float? = options.firstOrNull { it.label == label }?.value
}

data class PlayerState(
    val loading: Boolean = true,
    val item: Item? = null,
    /**
     * Играющий трек и соседи по плейлисту — модель выбирает их по маршруту, UI не ищет заново.
     * Предыдущий нужен пульту телефона: там есть кнопка «предыдущая серия».
     */
    val track: MediaTrack? = null,
    val nextTrack: MediaTrack? = null,
    val previousTrack: MediaTrack? = null,
    val streamUrl: String? = null,
    val qualities: List<StreamQuality> = emptyList(),
    val currentQuality: String? = null,
    /** Аудиодорожки потока; пусто, если выбирать не из чего (одна дорожка). */
    val audioTracks: List<AudioOption> = emptyList(),
    val currentAudio: String = "",
    val subtitles: List<SubtitleOption> = emptyList(),
    val currentSubtitle: String = "Выкл",
    /** Скорость воспроизведения; сессионная, дефолт — обычная (1.0). */
    val currentSpeed: Float = PlaybackSpeeds.NormalSpeed,
    /** У аккаунта нет активной подписки — поток не отдаётся, плеер объясняет это плашкой. */
    val subscriptionRequired: Boolean = false,
    val error: String? = null,
    /**
     * Нижняя панель ТВ-плеера: открыта ли, какая вкладка выбрана и какой сезон листают во вкладке
     * «Серии». Живёт в модели, а не в разметке: открытие панели ставит видео на паузу, закрытие —
     * возвращает воспроизведение, и это решение модели, а не экрана.
     */
    val panelOpen: Boolean = false,
    val panelTab: PlayerTab? = null,
    val panelSeason: Int? = null,
    /** Снимок воспроизведения — обновляется слушателем плеера и тиком раз в секунду. */
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
    /**
     * Перемотка в процессе: позиция, которую двигает палец или стрелка пульта до подтверждения
     * seekTo; null — перемотки нет. Пока она идёт, тик плеера позицию не перезаписывает.
     */
    val scrubTargetMs: Long? = null,
    /** Карточка «Далее» и её отсчёт — ведёт тик модели. */
    val autoNext: AutoNext = AutoNext(),
) {
    /** Что показывать на ползунке и часах: палец или пульт, а если перемотки нет — плеер. */
    val shownPositionMs: Long get() = scrubTargetMs ?: playback.positionMs

    val remainingMs: Long get() = (playback.durationMs - playback.positionMs).coerceAtLeast(0)

    /** Выбранная дорожка: рядом с подписью вкладки и в плашке транспорта. */
    val selectedAudio: AudioOption? get() = audioTracks.firstOrNull { it.label == currentAudio }

    /** Сериал — серий больше одной: у него вкладка «Серии» и подписи с номером серии. */
    val isSeries: Boolean get() = item?.tracklist.orEmpty().size > 1

    /**
     * Вкладки, в которых есть из чего выбрать: серии — у сериала, дорожки и качество — когда их больше
     * одной. Скорость есть всегда — набор фиксированный.
     */
    fun panelTabs(): List<PlayerTab> = buildList {
        if (isSeries) add(PlayerTab.Episodes)
        if (audioTracks.size > 1) add(PlayerTab.Audio)
        if (subtitles.size > 1) add(PlayerTab.Subtitles)
        if (qualities.size > 1) add(PlayerTab.Quality)
        add(PlayerTab.Speed)
    }

    /**
     * Вкладка, на которой открывается панель: та, что была выбрана в прошлый раз (если она ещё есть),
     * иначе первая — у сериала это «Серии»: за панелью чаще всего идут переключить серию.
     */
    fun openingTab(): PlayerTab {
        val tabs = panelTabs()
        return panelTab?.takeIf { it in tabs } ?: tabs.first()
    }

    /** Сезон, который сейчас листают во вкладке «Серии»; по умолчанию — сезон играющей серии. */
    fun shownSeason(): PlayerSeason? {
        val seasons = playerSeasons(item?.tracklist.orEmpty())
        val number = panelSeason ?: track?.seasonNumber
        return seasons.firstOrNull { it.number == number } ?: seasons.firstOrNull()
    }
}

sealed interface PlayerEvent {
    /** Пауза или воспроизведение — кнопка по центру оверлея телефона и «ОК» пульта. */
    data object TogglePlay : PlayerEvent

    /** Шаг перемотки кнопками ⟲10 / 10⟳ телефона: сразу seekTo. */
    data class SeekBy(val deltaMs: Long) : PlayerEvent

    /** Телефон: палец на ползунке — позиция пальца, плеер пока не трогаем. */
    data class Scrub(val positionMs: Long) : PlayerEvent

    /** Телефон: палец отпущен — перемотка подтверждается. */
    data object ScrubFinished : PlayerEvent

    /**
     * ТВ: стрелка перемотки. [repeatCount] и [eventTimeMs] — из события пульта: первое событие
     * удержания (repeatCount 0) запоминает начало, следующие считают разгон от него. Подтверждение
     * — паузой в нажатиях ([SCRUB_COMMIT_TIMEOUT_MS]), её ведёт модель.
     */
    data class SeekStep(val direction: Int, val repeatCount: Int, val eventTimeMs: Long) : PlayerEvent

    data class SelectQuality(val label: String) : PlayerEvent
    data class SelectAudio(val label: String) : PlayerEvent
    data class SelectSubtitle(val label: String) : PlayerEvent
    data class SetSpeed(val speed: Float) : PlayerEvent

    /** Нижняя панель ТВ: открыть (видео встаёт на паузу) и закрыть (воспроизведение продолжается). */
    data object OpenPanel : PlayerEvent
    data object ClosePanel : PlayerEvent

    /** Телефон: открыть панель сразу на вкладке [tab] — по чипу «Аудио», «Субтитры» под видео. */
    data class OpenPanelAt(val tab: PlayerTab) : PlayerEvent

    /** Фокус пришёл на вкладку панели — она и выбрана. */
    data class SelectTab(val tab: PlayerTab) : PlayerEvent

    /** Во вкладке «Серии» листают сезон [season] — без запуска серии. */
    data class ShowSeason(val season: Int) : PlayerEvent

    /** Другая серия того же тайтла — новый экран плеера на месте этого; сезон обязателен. */
    data class PlayEpisode(val season: Int, val videoId: Int) : PlayerEvent

    /** Следующая серия — с чипа «Далее» телефона или «ОК» на карточке ТВ. */
    data object PlayNext : PlayerEvent

    /** ТВ: «Назад» при карточке «Далее» — серия дотечёт до конца и остановится. */
    data object DismissAutoNext : PlayerEvent

    /**
     * Экран плеера появился и пропал (HOME, лаунчер, уход с экрана). Скрытый плеер на паузе, и
     * загрузка, закончившаяся за кадром, видео не запускает — иначе звук шёл бы поверх лаунчера.
     */
    data object ScreenShown : PlayerEvent
    data object ScreenHidden : PlayerEvent

    /** Телефон: «Смотреть на ТВ» — продолжить с этого места на телевизоре. */
    data object ContinueOnTv : PlayerEvent

    /** Выход из плеера: «Назад» и кнопка в оверлее. */
    data object Exit : PlayerEvent
}

/** Одноразовых эффектов у плеера нет: окно оценки, другая серия, выход и выбор ТВ — навигатор в модели. */
sealed interface PlayerSideEffect
