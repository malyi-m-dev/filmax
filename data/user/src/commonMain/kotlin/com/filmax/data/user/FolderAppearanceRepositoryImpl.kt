package com.filmax.data.user

import com.filmax.core.domain.user.FolderAppearanceRepository
import com.filmax.core.domain.user.model.FolderAppearance
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Оформление папок в настройках устройства — одной JSON-записью: папок единицы, а читать и
 * писать их удобнее целиком, чем ключом на папку. Реактивность — через [MutableStateFlow].
 */
internal class FolderAppearanceRepositoryImpl(private val settings: Settings) : FolderAppearanceRepository {

    private val state = MutableStateFlow(load())

    override val appearances: Flow<Map<Int, FolderAppearance>> = state.asStateFlow()

    override suspend fun setTint(folderId: Int, tint: Int) = edit(folderId) { it.copy(tint = tint) }

    override suspend fun rename(folderId: Int, title: String) = edit(folderId) { it.copy(title = title) }

    override suspend fun forget(folderId: Int) {
        state.update { it - folderId }
        save()
    }

    private fun edit(folderId: Int, transform: (FolderAppearance) -> FolderAppearance) {
        state.update { current -> current + (folderId to transform(current[folderId] ?: FolderAppearance())) }
        save()
    }

    private fun save() {
        val stored = state.value.mapValues { (_, appearance) -> Stored(appearance.tint, appearance.title) }
        settings.putString(KEY, json.encodeToString(stored))
    }

    // Битая запись (ручная правка, смена формата) не должна ронять экраны: оформление — украшение.
    private fun load(): Map<Int, FolderAppearance> =
        settings.getStringOrNull(KEY)
            ?.let { raw -> runCatching { json.decodeFromString<Map<Int, Stored>>(raw) }.getOrNull() }
            ?.mapValues { (_, stored) -> FolderAppearance(stored.tint, stored.title) }
            .orEmpty()

    @Serializable
    private data class Stored(val tint: Int? = null, val title: String? = null)

    private companion object {
        const val KEY = "bookmark_folder_appearance"
        val json = Json { ignoreUnknownKeys = true }
    }
}
