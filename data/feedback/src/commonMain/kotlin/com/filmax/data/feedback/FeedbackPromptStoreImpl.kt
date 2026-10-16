package com.filmax.data.feedback

import com.filmax.core.domain.feedback.FeedbackPromptState
import com.filmax.core.domain.feedback.FeedbackPromptStore
import com.russhwolf.settings.Settings

/** Память показов окна оценки — в настройках устройства, рядом с настройками воспроизведения. */
internal class FeedbackPromptStoreImpl(private val settings: Settings) : FeedbackPromptStore {

    override suspend fun load() = FeedbackPromptState(
        lastPromptAt = settings.getLongOrNull(KEY_PROMPTED),
        lastSentAt = settings.getLongOrNull(KEY_SENT),
        finishedWatches = settings.getInt(KEY_WATCHES, 0),
        lastSeenVersion = settings.getStringOrNull(KEY_VERSION),
    )

    override suspend fun save(state: FeedbackPromptState) {
        state.lastPromptAt?.let { settings.putLong(KEY_PROMPTED, it) }
        state.lastSentAt?.let { settings.putLong(KEY_SENT, it) }
        settings.putInt(KEY_WATCHES, state.finishedWatches)
        state.lastSeenVersion?.let { settings.putString(KEY_VERSION, it) }
    }

    private companion object {
        const val KEY_PROMPTED = "feedback_prompted_at"
        const val KEY_SENT = "feedback_sent_at"
        const val KEY_WATCHES = "feedback_finished_watches"
        const val KEY_VERSION = "feedback_last_version"
    }
}
