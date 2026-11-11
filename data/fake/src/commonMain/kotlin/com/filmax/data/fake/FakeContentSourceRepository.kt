package com.filmax.data.fake

import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.source.ContentSourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Выбор источника в памяти — для юнит-тестов ScreenModel'ей. */
class FakeContentSourceRepository(initial: ContentSource = ContentSource.KINOPUB) : ContentSourceRepository {

    private val state = MutableStateFlow(initial)

    override val source: StateFlow<ContentSource> = state.asStateFlow()

    override suspend fun select(source: ContentSource) {
        state.value = source
    }
}
