// Черновик папки в панели «Новая папка» / «Переименовать»: название с экранной клавиатуры и
// цвет. Общий для «Моё» и деталей тайтла — обе модели держат его в своём состоянии и меняют
// одним событием с FolderInput, а панель лишь рисует черновик.
package com.filmax.core.presentation.folders

import com.filmax.core.domain.user.model.FOLDER_TINT_COUNT
import com.filmax.core.domain.user.model.FOLDER_TITLE_MAX_LENGTH
import com.filmax.core.presentation.input.KeyboardLayout
import com.filmax.core.presentation.input.SPACE_KEY
import com.filmax.core.presentation.input.erasedText
import com.filmax.core.presentation.input.typedText

/**
 * Черновик папки. [folderId] — какую папку переименовываем; null — создаём новую.
 * [layout] — раскладка клавиатуры: её выбор тоже часть набора, а не вида.
 */
data class FolderDraft(
    val title: String = "",
    val tint: Int = 0,
    val layout: KeyboardLayout = KeyboardLayout.RU,
    val folderId: Int? = null,
) {
    /** Создать/сохранить можно только непустое название. */
    val canSubmit: Boolean get() = title.isNotBlank()

    val isRename: Boolean get() = folderId != null

    /** Название без хвостового пробела — таким оно уходит на сервер. */
    val cleanTitle: String get() = title.trim()

    fun apply(input: FolderInput): FolderDraft = when (input) {
        is FolderInput.Key -> copy(title = typedText(title, input.key, FOLDER_TITLE_MAX_LENGTH))
        FolderInput.Space -> copy(title = typedText(title, SPACE_KEY, FOLDER_TITLE_MAX_LENGTH))
        FolderInput.Erase -> copy(title = erasedText(title))
        is FolderInput.Tint -> copy(tint = input.tint.mod(FOLDER_TINT_COUNT))
        is FolderInput.Layout -> copy(layout = input.layout)
    }
}

/** Что пульт делает с черновиком: клавиша, пробел, «Стереть», цвет, раскладка. */
sealed interface FolderInput {
    data class Key(val key: String) : FolderInput
    data object Space : FolderInput
    data object Erase : FolderInput
    data class Tint(val tint: Int) : FolderInput
    data class Layout(val layout: KeyboardLayout) : FolderInput
}
