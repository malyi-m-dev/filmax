package com.filmax.data.catalog.di

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.playback.StreamRepository
import com.filmax.core.network.di.kinopubSingle
import com.filmax.data.catalog.CatalogRepositoryImpl
import com.filmax.data.catalog.TracklistStreamRepository
import com.filmax.data.catalog.remote.CatalogApi
import org.koin.dsl.module

val catalogModule = module {
    single { CatalogApi(get()) }
    kinopubSingle<CatalogRepository> { CatalogRepositoryImpl(api = get()) }
    kinopubSingle<StreamRepository> { TracklistStreamRepository() }
}
