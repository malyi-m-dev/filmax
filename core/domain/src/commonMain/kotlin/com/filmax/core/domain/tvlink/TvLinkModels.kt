package com.filmax.core.domain.tvlink

/**
 * Телевизор с Filmax, до которого телефон может дотянуться. [id] — имя сервиса в mDNS: уникально в
 * сети и не меняется, когда роутер выдаёт ТВ другой адрес. У ТВ, введённого адресом вручную, [id] —
 * сам адрес.
 */
data class TvDevice(val id: String, val name: String, val host: String, val port: Int)

/** Что телефон просит у телевизора. */
sealed interface RemoteCommand {

    /**
     * Запустить тайтл. [videoId] и [season] — те же, что у `Destination.Player`: номер серии уникален
     * только внутри сезона. [startSeconds] — позиция, с которой продолжить (её знает телефон, если
     * смотрел сам); -1 — с прогресса, сохранённого в kino.pub.
     */
    data class Play(val itemId: Int, val videoId: Int, val season: Int, val startSeconds: Int) : RemoteCommand

    data object Pause : RemoteCommand
    data object Resume : RemoteCommand

    /** Шаг перемотки: отрицательный — назад. */
    data class SeekBy(val deltaMs: Long) : RemoteCommand

    /** Перемотка ползунком пульта — на абсолютную позицию. */
    data class SeekTo(val positionMs: Long) : RemoteCommand

    data object NextEpisode : RemoteCommand
    data object PreviousEpisode : RemoteCommand

    /** Закрыть плеер на телевизоре. */
    data object Stop : RemoteCommand
}

/**
 * Что играет на телевизоре — снимок для пульта. Серия — номерами, а не готовой подписью: подпись
 * собирает экран телефона. [season] ≤ 0 — фильм.
 */
data class PlaybackStatus(
    val itemId: Int,
    val title: String,
    val posterUrl: String?,
    val season: Int,
    val episode: Int,
    val episodeTitle: String?,
    val playing: Boolean,
    val buffering: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)

/** Почему связи с телевизором нет. */
enum class LinkFailure {
    /** По адресу никто не ответил: ТВ выключен, Filmax на нём закрыт или он в другой сети. */
    Unreachable,

    /** ТВ ответил, но вошёл в другой аккаунт kino.pub — команды он не примет. */
    OtherAccount,

    /** Связь была и оборвалась. */
    Dropped,
}

/** Связь телефона с телевизором — одна на приложение. */
sealed interface RemoteSession {

    data object Idle : RemoteSession

    data class Connecting(val device: TvDevice) : RemoteSession

    /** [playback] — что играет; null — плеер на телевизоре закрыт. */
    data class Connected(val device: TvDevice, val playback: PlaybackStatus?) : RemoteSession

    data class Lost(val device: TvDevice, val failure: LinkFailure) : RemoteSession
}

/**
 * Приёмник на телевизоре, как его видит интерфейс ТВ. [address] — «192.168.1.50» (с портом, если он
 * не стандартный): его вводят на телефоне, когда поиск в сети не находит ТВ. [phones] — имена
 * подключённых телефонов.
 */
data class ReceiverState(
    val listening: Boolean = false,
    val address: String? = null,
    val phones: List<String> = emptyList(),
)
