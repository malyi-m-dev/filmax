package com.filmax.core.domain.watchlist

import com.filmax.core.domain.watchlist.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

/**
 * «Буду смотреть» — один контракт на источник. Как отметка хранится, решает реализация: у
 * kino.pub это папка-закладка плюс флаг `in_watchlist` (сервер не отдаёт список по флагу), у
 * домашнего сервера — его избранное. Экраны и use case'ы видят только список и отметку.
 *
 * Отметка оптимистичная: [items] и [contains] меняются сразу, сервер догоняет. Расхождение
 * после сбоя выправляет следующее обновление списка с сервера.
 */
interface WatchlistRepository {

    /** Список для «Моего» — из кэша сразу, потом с сервера. */
    val items: Flow<List<WatchlistItem>>

    /** Отмечен ли тайтл — для кнопки в деталях и на Главной. */
    fun contains(itemId: Int): Flow<Boolean>

    suspend fun add(item: WatchlistItem)

    suspend fun remove(itemId: Int)

    /**
     * Источник сам прислал тайтл с отметкой — её поставили в вебе или на другом устройстве.
     * Список подхватывает тайтл, а на сервер ничего не уходит: отметка там уже стоит.
     */
    suspend fun adopt(item: WatchlistItem)
}
