package com.filmax.data.fake

import com.filmax.core.domain.feedback.FeedbackPromptState
import com.filmax.core.domain.feedback.FeedbackPromptStore
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.domain.settings.ThemeSettingsRepository
import com.filmax.core.domain.user.FolderAppearanceRepository
import com.filmax.core.domain.user.model.FolderAppearance
import com.filmax.core.domain.watchlist.WatchlistRepository
import com.filmax.core.domain.watchlist.model.WatchlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Локальные хранилища приложения в памяти. Настоящие живут в multiplatform-settings устройства и
// к kino.pub не ходят, поэтому мок-сборка берёт их как есть, а юнит-тестам ScreenModel'ей нужны
// эти: чистое состояние на каждый тест, без Android-контекста.

/**
 * «Буду смотреть» в памяти, без «сервера»: тестам ScreenModel'ей, которым нужен пустой список и
 * тайтлы с произвольными id и оценками. Список и папки фейкового сервера ведёт
 * [FakeWatchlistRepository].
 */
class InMemoryWatchlistRepository : WatchlistRepository {
    private val state = MutableStateFlow<List<WatchlistItem>>(emptyList())

    override val items: Flow<List<WatchlistItem>> = state

    override fun contains(itemId: Int): Flow<Boolean> = state.map { list -> list.any { it.id == itemId } }

    override suspend fun add(item: WatchlistItem) = adopt(item)

    override suspend fun remove(itemId: Int) {
        state.update { list -> list.filterNot { it.id == itemId } }
    }

    override suspend fun adopt(item: WatchlistItem) {
        state.update { list -> if (list.any { it.id == item.id }) list else list + item }
    }
}

/** Оформление папок в памяти — вместо записи в настройках устройства. */
class FakeFolderAppearanceRepository : FolderAppearanceRepository {
    private val state = MutableStateFlow<Map<Int, FolderAppearance>>(emptyMap())

    override val appearances: Flow<Map<Int, FolderAppearance>> = state

    override suspend fun setTint(folderId: Int, tint: Int) = edit(folderId) { it.copy(tint = tint) }

    override suspend fun rename(folderId: Int, title: String) = edit(folderId) { it.copy(title = title) }

    override suspend fun forget(folderId: Int) {
        state.update { it - folderId }
    }

    private fun edit(folderId: Int, transform: (FolderAppearance) -> FolderAppearance) {
        state.update { current -> current + (folderId to transform(current[folderId] ?: FolderAppearance())) }
    }
}

/** Оформление ТВ в памяти — вместо записи в настройках устройства. */
class FakeThemeSettingsRepository : ThemeSettingsRepository {
    private val paletteState = MutableStateFlow(ThemePalette.VIOLET)
    private val plainFocusState = MutableStateFlow(false)

    override val palette: StateFlow<ThemePalette> = paletteState

    override val plainFocus: StateFlow<Boolean> = plainFocusState

    override suspend fun setPalette(palette: ThemePalette) {
        paletteState.value = palette
    }

    override suspend fun setPlainFocus(enabled: Boolean) {
        plainFocusState.value = enabled
    }
}

/** Настройки воспроизведения в памяти — вместо записи в настройках устройства. */
class FakePlaybackSettingsRepository : PlaybackSettingsRepository {
    private val state = MutableStateFlow(PlaybackSettings())
    private val voices = mutableMapOf<Int, String>()

    override val settings: Flow<PlaybackSettings> = state

    override suspend fun setQuality(quality: String) = state.update { it.copy(quality = quality) }

    override suspend fun setAudioLanguage(language: String) = state.update { it.copy(audioLanguage = language) }

    override suspend fun setSubtitleLanguage(language: String) =
        state.update { it.copy(subtitleLanguage = language) }

    override suspend fun voiceKeyFor(itemId: Int): String? = voices[itemId]

    override suspend fun setVoiceKey(itemId: Int, key: String) {
        voices[itemId] = key
    }

    private var decoderLimit: Int? = null

    override suspend fun decoderLimit(): Int? = decoderLimit

    override suspend fun setDecoderLimit(height: Int?) {
        decoderLimit = height
    }
}

/** Память показов окна оценки в памяти — у каждого теста чистая. */
class FakeFeedbackPromptStore : FeedbackPromptStore {
    var state = FeedbackPromptState()

    override suspend fun load(): FeedbackPromptState = state

    override suspend fun save(state: FeedbackPromptState) {
        this.state = state
    }
}
