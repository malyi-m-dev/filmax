package com.filmax.data.user.di

import com.filmax.core.domain.user.FolderAppearanceRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.data.user.FolderAppearanceRepositoryImpl
import com.filmax.data.user.UserRepositoryImpl
import com.filmax.data.user.remote.UserApi
import org.koin.dsl.module

val userModule = module {
    single { UserApi(get()) }
    single<UserRepository> { UserRepositoryImpl(api = get()) }
    // Локальное оформление папок: в kino.pub не ходит, поэтому и мок-сборка берёт его как есть.
    single<FolderAppearanceRepository> { FolderAppearanceRepositoryImpl(settings = get()) }
}
