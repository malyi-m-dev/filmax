package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.Item

/** Доступное качество потока (метка + ссылка), приходит с бэкенда. */
data class StreamQuality(val label: String, val url: String)

/** Вариант субтитров; [lang] == null означает «Выкл». */
data class SubtitleOption(val label: String, val lang: String?)

/** Аудиодорожка потока (язык из манифеста); [lang] == null — язык неизвестен. */
data class AudioOption(val label: String, val lang: String?)

data class PlayerState(
    val loading: Boolean = true,
    val item: Item? = null,
    val streamUrl: String? = null,
    val qualities: List<StreamQuality> = emptyList(),
    val currentQuality: String? = null,
    /** Аудиодорожки потока; пусто, если выбирать не из чего (одна дорожка). */
    val audioTracks: List<AudioOption> = emptyList(),
    val currentAudio: String = "",
    val subtitles: List<SubtitleOption> = emptyList(),
    val currentSubtitle: String = "Выкл",
    val error: String? = null,
)

sealed interface PlayerEvent {
    data class SaveProgress(val positionMs: Long) : PlayerEvent
    data class SelectQuality(val label: String) : PlayerEvent
    data class SelectAudio(val label: String) : PlayerEvent
    data class SelectSubtitle(val label: String) : PlayerEvent
}

sealed interface PlayerSideEffect
