// Потолок декодера устройства: качество, выше которого приставка уже не вытянула кадр.
package com.filmax.feature.player.common

import com.filmax.core.domain.common.ErrorReporting

/** Ключ отчёта Crashlytics: высота кадра, на которой декодер устройства не справился. */
private const val DECODER_LIMIT_KEY = "decoder_limit"

/**
 * Потолок декодера (см. PlaybackSettingsRepository.decoderLimit). Понижается, когда декодер падает
 * на кадре, и снимается, когда кадр выше потолка всё-таки показан. Это не баг приложения, а предел
 * железа, поэтому в телеметрию — крошкой и ключом, не событием. [persist] сохраняет потолок для
 * устройства: следующий запуск сразу стартует не выше него.
 */
internal class DecoderLimit(private val persist: (Int?) -> Unit) {

    /** Высота кадра, выше которой не стартуем; null — потолка нет. */
    var height: Int? = null
        private set

    /**
     * Сохранённый потолок. Full HD и ниже мог остаться от 2.2.0, где его записывал занятый декодер,
     * — такой не действует.
     */
    fun restore(saved: Int?) {
        height = saved?.let(QualityLadder::limitOf)
    }

    /** Декодер не справился с кадром [failedHeight]: потолок — не выше него, поток уходит на [lowerLabel]. */
    fun lowerTo(failedHeight: Int, lowerLabel: String) {
        ErrorReporting.reporter.log("player: decoder can't play ${failedHeight}p, step down to $lowerLabel")
        ErrorReporting.reporter.setKey(DECODER_LIMIT_KEY, failedHeight.toString())
        height = minOf(failedHeight, height ?: failedHeight)
        persist(height)
    }

    /**
     * Показан кадр высотой [shownHeight] — не ниже потолка: зритель выбрал его сам, и он идёт (скажем,
     * 4K в HEVC там, где 4K H.264 падал), значит потолок был ложным.
     */
    fun liftIfReached(shownHeight: Int?) {
        val limit = height
        if (limit == null || shownHeight == null || shownHeight < limit) return
        ErrorReporting.reporter.log("player: ${shownHeight}p plays, decoder limit ${limit}p lifted")
        height = null
        persist(null)
    }
}
