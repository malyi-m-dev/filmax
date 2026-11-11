package com.filmax.shared

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.playback.StreamRepository
import com.filmax.core.domain.search.SearchRepository
import com.filmax.core.domain.source.ContentSourceRepository
import com.filmax.core.domain.source.HomeServerRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watchlist.WatchlistRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Полный data-слой для Swift — единая точка доступа ко всем репозиториям.
 *
 * Koin reified-`get()` не виден из Obj-C/Swift, поэтому репозитории отдаются как свойства этого
 * объекта. Цель — iOS-разработчик вызывает любой метод любого репозитория, **не трогая Kotlin**.
 * Репозитории — Koin-синглтоны, поэтому каждое обращение возвращает тот же экземпляр.
 *
 * Из Swift:
 * ```swift
 * let details = try await RepositoryProvider.shared.catalog.getItemDetails(id: 42)
 * for await items in RepositoryProvider.shared.watchlist.items { … }
 * ```
 *
 * Контракты методов и структуры возвращаемых моделей — см. `docs/ios-shared-layer.md` (§7–§8).
 * Готовые операции-обёртки (device-flow, лента Главной) — в [UseCaseProvider].
 */
object RepositoryProvider : KoinComponent {
    /** OAuth device-flow, поток авторизации, logout. */
    val auth: AuthRepository get() = get()

    /** Каталог: списки/детали/похожие/жанры/подборки. */
    val catalog: CatalogRepository get() = get()

    /** Поиск по названию, актёру, режиссёру. */
    val search: SearchRepository get() = get()

    /** Профиль и папки закладок. */
    val user: UserRepository get() = get()

    /** История, прогресс просмотра, отметка «просмотрено». */
    val watching: WatchingRepository get() = get()

    /** «Буду смотреть» текущего источника (реактивные `Flow`). */
    val watchlist: WatchlistRepository get() = get()

    /** Настройки воспроизведения (качество/аудио/субтитры, локально). */
    val playbackSettings: PlaybackSettingsRepository get() = get()

    /** Источник контента: kino.pub или домашний сервер; `source.value` — текущий. */
    val sources: ContentSourceRepository get() = get()

    /** Вход на домашний сервер: поиск в сети, адрес, пароль или Quick Connect. */
    val homeServer: HomeServerRepository get() = get()

    /** Потоки трека под запуск: домашний сервер выдаёт ссылки на каждый старт. */
    val streams: StreamRepository get() = get()
}
