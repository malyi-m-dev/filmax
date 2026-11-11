package com.filmax.data.source

import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.source.ContentSourceRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Источник — настройка устройства. Незнакомое или пустое значение — kino.pub, как до выбора. */
internal class ContentSourceRepositoryImpl(private val settings: Settings) : ContentSourceRepository {

    private val state = MutableStateFlow(stored())

    override val source: StateFlow<ContentSource> = state.asStateFlow()

    override suspend fun select(source: ContentSource) {
        settings.putString(KEY, source.name)
        state.value = source
    }

    private fun stored(): ContentSource =
        settings.getStringOrNull(KEY)?.let { name -> ContentSource.entries.firstOrNull { it.name == name } }
            ?: ContentSource.KINOPUB

    private companion object {
        const val KEY = "content_source"
    }
}
