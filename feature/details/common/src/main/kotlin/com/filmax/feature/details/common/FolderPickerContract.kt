package com.filmax.feature.details.common

import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput

/**
 * Панель «В папку» в деталях (экраны 02c, 02d): список папок с отметками и создание новой.
 * Системная «Буду смотреть» в [folders] не входит — её отметка живёт в [DetailsState.isFav].
 */
data class FolderPickerState(
    val open: Boolean = false,
    val loading: Boolean = false,
    /** Папки пользователя, свежие первыми. */
    val folders: List<UserFolder> = emptyList(),
    /** Папки, где тайтл уже лежит. */
    val memberOf: Set<Int> = emptySet(),
    /** Сколько тайтлов в «Буду смотреть» — подпись системной строки. */
    val watchlistCount: Int = 0,
    /** Куда встаёт фокус в списке: первая папка без тайтла или только что созданная. */
    val focusFolderId: Int? = null,
    /** Черновик новой папки; не null — вместо списка открыт редактор. */
    val draft: FolderDraft? = null,
    /** Папка создаётся: повторное «Создать и добавить» до ответа завело бы вторую такую же. */
    val creating: Boolean = false,
    /** Последняя ошибка синхронизации — отметка уже откатилась, панель о ней говорит. */
    val error: String? = null,
) {
    /**
     * Лежит ли тайтл хоть в одной папке — тогда иконка кнопки «В папку» залитая. Известно только
     * после первого открытия панели: проверка стоит запроса на каждую папку, и делать её при
     * каждом входе в детали ради иконки — слишком дорого.
     */
    val inAnyFolder: Boolean get() = memberOf.isNotEmpty()
}

sealed interface FolderPickerEvent {
    data object Open : FolderPickerEvent
    data object Close : FolderPickerEvent

    /** «ОК» на папке: отметка переключается сразу, без подтверждения. */
    data class Toggle(val folderId: Int) : FolderPickerEvent

    data object StartCreate : FolderPickerEvent
    data class Edit(val input: FolderInput) : FolderPickerEvent

    /** «Создать и добавить»: новая папка встаёт наверх списка уже с тайтлом. */
    data object Submit : FolderPickerEvent

    /** «Назад» или «Отмена» в редакторе — обратно к списку, панель остаётся открытой. */
    data object CancelCreate : FolderPickerEvent
}

sealed interface FolderPickerSideEffect
