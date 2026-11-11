package com.filmax.data.fake

import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.playback.StreamRepository

/** Потоки фейкового каталога уже лежат в треке, как у kino.pub, — трек играется как есть. */
class FakeStreamRepository : StreamRepository {
    override suspend fun playable(itemId: Int, track: MediaTrack): RequestResult<MediaTrack> =
        RequestResult.Success(track)
}
