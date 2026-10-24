package com.filmax.core.domain.tvlink

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Поиск телевизоров в локальной сети (телефон). Показывает только ТВ того же аккаунта kino.pub:
 * чужой телевизор команды не примет, и предлагать его незачем.
 */
interface TvDiscovery {

    /** Найденные ТВ. Поиск идёт, пока поток собирают, и останавливается вместе с подпиской. */
    fun devices(): Flow<List<TvDevice>>

    /**
     * ТВ по адресу, введённому вручную («192.168.1.50» или «192.168.1.50:47100»), — для сетей, где
     * роутер режет mDNS. null — строка не похожа на адрес.
     */
    fun deviceAt(address: String): TvDevice?
}

/** Пульт на телефоне: связь с одним телевизором на всё приложение. */
interface TvRemote {

    val session: StateFlow<RemoteSession>

    /**
     * Подключиться к [device] и пройти проверку аккаунта; прежняя связь рвётся. true — связь есть;
     * иначе причина — в [session] ([RemoteSession.Lost]).
     */
    suspend fun connect(device: TvDevice): Boolean

    /** Команда подключённому телевизору; без связи она молча не уходит. */
    fun send(command: RemoteCommand)

    fun disconnect()
}

/**
 * Приёмник на телевизоре: объявляет ТВ в сети, пускает телефоны своего аккаунта и передаёт их
 * команды. Плеер сообщает ему, что играет, — телефоны получают это снимками.
 */
interface TvLinkReceiver {

    val state: StateFlow<ReceiverState>

    /** Команды телефонов, прошедших проверку аккаунта. */
    val commands: Flow<RemoteCommand>

    /** Последний снимок плеера; null — плеер закрыт. */
    val playback: StateFlow<PlaybackStatus?>

    fun publish(status: PlaybackStatus?)

    /**
     * Принимать телефоны, пока корутина жива: ТВ виден в сети, только когда Filmax на экране. Без
     * входа в аккаунт приёмник не поднимается — проверять телефоны не по чему.
     */
    suspend fun serve()
}
