package com.filmax.core.domain.playback

import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.common.RequestResult

/**
 * Потоки трека к старту плеера. У kino.pub ссылки приходят вместе с тайтлом, и трек играется как
 * есть. Домашний сервер решает сам, отдать файл напрямую или перекодировать — по кодекам файла и
 * устройства, — и ссылки выдаёт под конкретный запуск. Поэтому трек уточняется перед стартом, а
 * не при загрузке деталей, где серий бывают сотни.
 */
interface StreamRepository {

    /** Трек [track] тайтла [itemId] с потоками, дорожками и субтитрами для воспроизведения. */
    suspend fun playable(itemId: Int, track: MediaTrack): RequestResult<MediaTrack>
}
