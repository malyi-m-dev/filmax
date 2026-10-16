package com.filmax.data.fake

import com.filmax.core.domain.downloads.DownloadsRepository
import com.filmax.core.domain.downloads.model.DownloadedItem
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.feedback.FeedbackPromptState
import com.filmax.core.domain.feedback.FeedbackPromptStore
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.domain.settings.ThemeSettingsRepository
import com.filmax.core.domain.user.FolderAppearanceRepository
import com.filmax.core.domain.user.model.FolderAppearance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Локальные хранилища приложения в памяти. Настоящие живут в multiplatform-settings устройства и
// к kino.pub не ходят, поэтому мок-сборка берёт их как есть, а юнит-тестам ScreenModel'ей нужны
// эти: чистое состояние на каждый тест, без Android-контекста.

/** «Буду смотреть» в памяти — вместо локального кэша избранного. */
class FakeFavoritesRepository : FavoritesRepository {
    private val items = MutableStateFlow<List<FavoriteItem>>(emptyList())

    override val favorites: Flow<List<FavoriteItem>> = items
    override val favoriteIds: Flow<Set<Int>> = items.map { list -> list.mapTo(HashSet()) { it.id } }

    override fun isFavorite(id: Int): Flow<Boolean> = items.map { list -> list.any { it.id == id } }

    override suspend fun toggle(item: FavoriteItem): Boolean {
        val present = items.value.any { it.id == item.id }
        items.value = if (present) items.value.filterNot { it.id == item.id } else items.value + item
        return !present
    }

    override suspend fun add(item: FavoriteItem) {
        if (items.value.none { it.id == item.id }) items.value = items.value + item
    }

    override suspend fun remove(id: Int) {
        items.value = items.value.filterNot { it.id == id }
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

/** Загрузки в памяти — вместо списка скачанного на устройстве. */
class FakeDownloadsRepository : DownloadsRepository {
    private val items = MutableStateFlow<List<DownloadedItem>>(emptyList())

    override val downloads: Flow<List<DownloadedItem>> = items

    override fun isDownloaded(id: Int): Flow<Boolean> = items.map { list -> list.any { it.id == id } }

    override suspend fun add(item: DownloadedItem) {
        if (items.value.none { it.id == item.id }) items.value = items.value + item
    }

    override suspend fun remove(id: Int) {
        items.value = items.value.filterNot { it.id == id }
    }
}

/** Палитра ТВ в памяти — вместо записи в настройках устройства. */
class FakeThemeSettingsRepository : ThemeSettingsRepository {
    private val state = MutableStateFlow(ThemePalette.VIOLET)

    override val palette: StateFlow<ThemePalette> = state

    override suspend fun setPalette(palette: ThemePalette) {
        state.value = palette
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
