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
        // Системная клавиатура телефона отдаёт текст целиком: регистр и пробелы — как набрали.
        is FolderInput.Text -> copy(title = input.text.trimStart().take(FOLDER_TITLE_MAX_LENGTH))
        is FolderInput.Tint -> copy(tint = input.tint.mod(FOLDER_TINT_COUNT))
        is FolderInput.Layout -> copy(layout = input.layout)
    }
}

/**
 * Что делают с черновиком: пульт — клавиша, пробел, «Стереть», раскладка; телефон — текст поля
 * целиком; оба — цвет.
 */
sealed interface FolderInput {
    data class Key(val key: String) : FolderInput

    /** Текст поля целиком — с системной клавиатуры телефона. */
    data class Text(val text: String) : FolderInput
    data object Space : FolderInput
    data object Erase : FolderInput
    data class Tint(val tint: Int) : FolderInput
    data class Layout(val layout: KeyboardLayout) : FolderInput
}
