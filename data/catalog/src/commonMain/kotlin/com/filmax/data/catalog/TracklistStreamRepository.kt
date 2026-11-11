package com.filmax.data.catalog

import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.playback.StreamRepository

/** У kino.pub ссылки на поток приходят в треке вместе с тайтлом — уточнять перед стартом нечего. */
internal class TracklistStreamRepository : StreamRepository {
    override suspend fun playable(itemId: Int, track: MediaTrack): RequestResult<MediaTrack> =
        RequestResult.Success(track)
}
