// Подписи ТВ-раздела «Моё» и экрана папки — чистые функции без UI: серия на кадре, остаток
// времени, подпись папки, подсказки сортировки.
package com.filmax.feature.library.tv

import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.core.presentation.labels.durationLabel
import com.filmax.core.presentation.labels.titlesLabel
import com.filmax.feature.library.common.MineFolder

private const val SECONDS_IN_MINUTE = 60

/** Серия на кадре: «S4E7». Фильм или неизвестная серия — null, плашки нет. */
internal fun episodeBadge(progress: WatchProgress?): String? {
    val season = progress?.season?.takeIf { it > 0 }
    val number = progress?.videoId?.takeIf { it > 0 }
    return if (season != null && number != null) "S${season}E$number" else null
}

/** Под карточкой «Продолжить»: «осталось 41 мин»; остатка нет — «просмотрено». */
internal fun remainingLabel(progress: WatchProgress?): String? {
    val watched = progress?.timeSeconds
    val total = progress?.durationSeconds?.takeIf { it > 0 }
    if (watched == null || total == null) return null
    val minutes = (total - watched) / SECONDS_IN_MINUTE
    return if (minutes > 0) "осталось ${durationLabel(minutes)}" else "просмотрено"
}

/** Под карточкой истории: «S4E7 · 21:40» — что и когда смотрели. */
internal fun historyMeta(progress: WatchProgress?, time: String?): String? =
    listOfNotNull(episodeBadge(progress), time).joinToString(" · ").ifBlank { null }

/** Под карточкой папки: «6 тайтлов · изменена вчера». */
internal fun folderCaption(folder: MineFolder): String =
    listOfNotNull(titlesLabel(folder.folder.count), folder.changed).joinToString(" · ")
