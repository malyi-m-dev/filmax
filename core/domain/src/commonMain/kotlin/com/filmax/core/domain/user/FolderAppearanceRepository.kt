package com.filmax.core.domain.user

import com.filmax.core.domain.user.model.FolderAppearance
import kotlinx.coroutines.flow.Flow

/**
 * Локальное оформление папок-закладок: цвет и название, заданное на этом устройстве. Сервер о
 * нём не знает (см. [FolderAppearance]), поэтому хранилище — настройки устройства, а экраны
 * подписываются на [appearances] и сразу видят перекраску и переименование.
 */
interface FolderAppearanceRepository {

    /** Оформление по id папки. Папки без записи — с цветом по умолчанию и серверным названием. */
    val appearances: Flow<Map<Int, FolderAppearance>>

    suspend fun setTint(folderId: Int, tint: Int)

    suspend fun rename(folderId: Int, title: String)

    /** Папку удалили — её оформление больше не нужно. */
    suspend fun forget(folderId: Int)
}
