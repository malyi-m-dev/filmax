package com.filmax.core.domain.user.model

import com.filmax.core.domain.favorites.WATCHLIST_FOLDER_TITLE

/** Сколько цветов у папок. Сами цвета — в дизайн-системе ТВ, домен хранит только номер. */
const val FOLDER_TINT_COUNT = 6

/** Предел длины названия папки — по нему поле ввода показывает счётчик «14 / 40». */
const val FOLDER_TITLE_MAX_LENGTH = 40

/**
 * То, что пользователь задал папке на этом устройстве: цвет и своё название.
 *
 * У kino.pub у папки есть только серверное название — ни цвета, ни переименования в API нет
 * (`bookmarks/create`, `remove-folder`, `add`, `remove-item` — и всё). Поэтому оба поля живут
 * локально: цвет — чисто оформление, а переименование честно действует только на этом
 * устройстве; на телефоне и на сайте папка останется под серверным именем. null — не задано.
 */
data class FolderAppearance(val tint: Int? = null, val title: String? = null)

/**
 * Папка пользователя, как её показывают экраны: серверная папка с наложенным оформлением.
 * [tint] — номер цвета 0 until [FOLDER_TINT_COUNT].
 */
data class UserFolder(
    val id: Int,
    val title: String,
    val count: Int,
    val updatedAt: Long?,
    val tint: Int,
)

/**
 * Папки пользователя из серверного списка: без системной «Буду смотреть» (у неё свой сегмент и
 * своя строка в выборе папки) и с локальным оформлением поверх.
 */
fun userFolders(folders: List<BookmarkFolder>, appearance: Map<Int, FolderAppearance>): List<UserFolder> =
    folders
        .filterNot { it.title == WATCHLIST_FOLDER_TITLE }
        .map { folder -> folder.toUserFolder(appearance[folder.id]) }

/** Системная папка «Буду смотреть» из серверного списка; null — её ещё не создали. */
fun watchlistFolder(folders: List<BookmarkFolder>): BookmarkFolder? =
    folders.firstOrNull { it.title == WATCHLIST_FOLDER_TITLE }

private fun BookmarkFolder.toUserFolder(appearance: FolderAppearance?): UserFolder = UserFolder(
    id = id,
    title = appearance?.title ?: title,
    count = count,
    updatedAt = updatedAt,
    tint = appearance?.tint ?: defaultFolderTint(id),
)

/**
 * Цвет папки, которой его ещё не выбирали: по id, а не по месту в списке — иначе после удаления
 * соседней папки все следующие перекрасились бы.
 */
fun defaultFolderTint(folderId: Int): Int = folderId.mod(FOLDER_TINT_COUNT)
