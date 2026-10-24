package com.filmax.core.ui.components

private const val MILLIS_IN_SECOND = 1000
private const val SECONDS_IN_MINUTE = 60
private const val SECONDS_IN_HOUR = 3600

/**
 * «1:23:45» / «23:45» — время воспроизведения, единое для плеера телефона и ТВ и для пульта.
 * Живёт в core:ui, а не в плеере: пульт телефона показывает то же время, но от плеера не зависит.
 */
fun formatPlayerTime(ms: Long): String {
    val totalSeconds = ms / MILLIS_IN_SECOND
    val hours = totalSeconds / SECONDS_IN_HOUR
    val minutes = (totalSeconds % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE
    val seconds = totalSeconds % SECONDS_IN_MINUTE
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
