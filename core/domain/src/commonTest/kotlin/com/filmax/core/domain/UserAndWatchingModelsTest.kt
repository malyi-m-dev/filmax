package com.filmax.core.domain

import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.domain.user.model.initials
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class UserAndWatchingModelsTest {

    @Test
    fun `progress fraction is watched share of the duration`() {
        assertEquals(0.5f, progress(timeSeconds = 600, durationSeconds = 1200).fraction)
    }

    @Test
    fun `progress fraction is zero without time or duration`() {
        assertEquals(0f, progress(timeSeconds = null, durationSeconds = 1200).fraction)
        assertEquals(0f, progress(timeSeconds = 600, durationSeconds = 0).fraction)
    }

    @Test
    fun `progress fraction never exceeds one`() {
        // HLS отдаёт позицию за заявленной длительностью — полоса не должна вылезать за карточку.
        assertEquals(1f, progress(timeSeconds = 1300, durationSeconds = 1200).fraction)
    }

    @Test
    fun `wide card image prefers the episode frame, then the wide poster`() {
        val history = WatchHistory(
            itemId = 1,
            title = "Тайтл",
            posterSmall = "poster",
            progress = null,
            posterWide = "wide",
            episodeThumbnail = "frame",
        )

        assertEquals("frame", history.wideOrPoster)
        assertEquals("wide", history.copy(episodeThumbnail = " ").wideOrPoster)
        assertEquals("poster", history.copy(episodeThumbnail = null, posterWide = null).wideOrPoster)
    }

    @Test
    fun `initials take the first letters of up to two name parts`() {
        assertEquals("ДЗ", profile("Демо Зритель").initials())
        assertEquals("JD", profile("john.doe").initials())
        assertEquals("M", profile("maxim").initials())
    }

    private fun progress(timeSeconds: Int?, durationSeconds: Int?) = WatchProgress(
        status = 0,
        timeSeconds = timeSeconds,
        durationSeconds = durationSeconds,
        videoId = 1,
        season = 1,
    )

    private fun profile(username: String) =
        UserProfile(id = 0, username = username, email = null, avatarUrl = null, subscription = null)
}
