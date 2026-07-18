# Filmax — общий data/domain-слой для iOS

Гид для iOS-разработчика: как из Swift работать с общей бизнес-логикой KMP. Презентейшен на iOS
пишется **на Swift/SwiftUI**, а вся data/domain-логика берётся из общего фреймворка `Shared`.

> Быстрый старт и сборка проекта — в [`iosApp/README.md`](../iosApp/README.md).

---

## 1. Архитектура (что общее, что нет)

| Слой | Где | Платформа |
|---|---|---|
| **domain** (модели, репозитории-интерфейсы, use-case'ы) | `:core:domain` (`commonMain`) | общий |
| **data** (реализации репозиториев, DTO, мапперы, API) | `:data:*`, `:core:network` (`commonMain`/`iosMain`) | общий |
| **сборка/DI для Swift** | `:shared` → фреймворк `Shared` | общий |
| **presentation** (ViewModel + вью) | `iosApp/` (Swift) | **iOS-нативный** |

iOS **не** использует `:core:presentation`, `:feature:*:common`, `:feature:*:mobile/tv` — это Android.
На iOS презентейшен-слой (ViewModel'и + SwiftUI) пишется заново поверх общих use-case'ов/репозиториев.

Фреймворк `Shared` (static) реэкспортирует: `core:domain`, `core:network`, `data:auth`, `data:catalog`,
`data:search`, `data:user`, `data:watching`. Подключён **SKIE** — делает Kotlin-API идиоматичным для Swift.

---

## 2. Инициализация (Koin)

Один раз при старте приложения:

```swift
import Shared

@main
struct iOSApp: App {
    init() { KoinKt.doInitKoin() }   // top-level fun doInitKoin() из Koin.kt
    // ...
}
```

`doInitKoin()` поднимает Koin с общими модулями (network + data + use-case'ы). После этого доступен
`UseCaseProvider`. Хранилище токенов на iOS — `multiplatform-settings` поверх `NSUserDefaults`
(сессия сохраняется между запусками автоматически).

---

## 3. Доступ к API из Swift — два провайдера

Koin reified `get()` не виден из Swift, поэтому общий код отдаёт всё через два Kotlin `object`-а
(→ Swift `.shared`). **iOS-разработчику этих двух достаточно — Kotlin трогать не нужно:**
`RepositoryProvider` даёт весь data-слой, `UseCaseProvider` — готовые операции.

### `RepositoryProvider` — полный data-слой (все репозитории)
Свойство на каждый репозиторий; методы и возвраты — §7, модели — §8.
```swift
let details = try await RepositoryProvider.shared.catalog.getItemDetails(id: 42)
let profile = try await RepositoryProvider.shared.user.getProfile()
for await ids in RepositoryProvider.shared.favorites.favoriteIds { /* Set<Int32> */ }
```

| Свойство | Репозиторий |
|---|---|
| `.auth` | `AuthRepository` |
| `.catalog` | `CatalogRepository` |
| `.search` | `SearchRepository` |
| `.user` | `UserRepository` |
| `.watching` | `WatchingRepository` |
| `.favorites` | `FavoritesRepository` |
| `.downloads` | `DownloadsRepository` |
| `.playbackSettings` | `PlaybackSettingsRepository` |

### `UseCaseProvider` — готовые операции-обёртки
```swift
let code = try await UseCaseProvider.shared.requestDeviceCodeUseCase().invoke()
```

| Метод | Use-case |
|---|---|
| `observeAuthStateUseCase()` | `ObserveAuthStateUseCase` |
| `requestDeviceCodeUseCase()` | `RequestDeviceCodeUseCase` |
| `pollForTokenUseCase()` | `PollForTokenUseCase` |
| `logoutUseCase()` | `LogoutUseCase` |
| `getHomeFeedUseCase()` | `GetHomeFeedUseCase` |
| `toggleWatchlistUseCase()` | `ToggleWatchlistUseCase` |
| `toggleWatchedUseCase()` | `ToggleWatchedUseCase` |

**Нужна операция, которой нет?** Репозитории уже покрывают весь data-слой — обычно этого хватает.
Если нужен новый доменный use-case (общий для Android/iOS) — добавь класс в `core:domain/usecase`,
зарегистрируй в `useCaseModule` (`Koin.kt`) и отдай через `UseCaseProvider`. Бизнес-логику на Swift не дублируй.

---

## 4. Интероп через SKIE (важно)

SKIE делает Kotlin-API «свифтовым». Три паттерна:

**4.1. suspend → `async`.** `suspend fun` вызывается как обычная async-функция:
```swift
let result = try await UseCaseProvider.shared.requestDeviceCodeUseCase().invoke()
```

**4.2. `Flow<T>` → `AsyncSequence`.** Собирается `for await`:
```swift
for await isAuthed in UseCaseProvider.shared.observeAuthStateUseCase().invoke() {
    // isAuthed: Bool  (если SKIE отдаёт KotlinBoolean — .boolValue)
}
```

**4.3. sealed → Swift enum через `onEnum(of:)`.** См. `RequestResult` ниже.

---

## 5. `RequestResult` — обёртка результата (обязательно к пониманию)

Все сетевые вызовы репозиториев возвращают `RequestResult<T>` (sealed):

```kotlin
sealed interface RequestResult<out T> {
    data class Success<T>(val data: T)
    data class Error(val message: String?, val cause: Throwable?)
}
```

Из Swift разбирается через SKIE:
```swift
let result = try await useCase.invoke()          // RequestResult<Item>
switch onEnum(of: result) {
case .success(let success): use(success.data)     // success.data : Item
case .error(let error):     show(error.message)   // error.message : String?
}
```

Хелперы (Kotlin, тоже доступны): `getOrNull()`, `map { }`, `onSuccess { }`, `onError { }`,
`firstErrorMessage(vararg results)`.

### Классификация ошибок — `AppError`
Для человекочитаемых сообщений есть `AppError` (`core/domain/error`): `Offline`, `Timeout`, `Auth`,
`Premium`, `Region`, `NotFound`, `Server`. Резолвится из результата:
`AppError.resolve(message, cause)` / `RequestResult.Error.toAppError()`. Используй для показа
корректного текста/иконки ошибки на экране (маппинг код/текст → тип уже сделан в общем коде).

---

## 6. Use-case'ы (доменные операции)

`invoke()` — оператор; из Swift вызывается как `useCase.invoke(...)`.

### Auth (OAuth device-flow)
| Use-case | Сигнатура | Возврат |
|---|---|---|
| `ObserveAuthStateUseCase` | `invoke()` | `Flow<Boolean>` — авторизован ли |
| `RequestDeviceCodeUseCase` | `invoke()` | `RequestResult<DeviceCode>` |
| `PollForTokenUseCase` | `invoke(code, username, timestamp)` | `RequestResult<Token>` |
| `LogoutUseCase` | `invoke()` | `RequestResult<Unit>` |

### Home / Watching
| Use-case | Сигнатура | Возврат |
|---|---|---|
| `GetHomeFeedUseCase` | `invoke()` | `HomeFeed` (агрегирует несколько запросов параллельно) |
| `ToggleWatchlistUseCase` | `invoke(itemId)` | `RequestResult<Boolean>` (в списке/нет) |
| `ToggleWatchedUseCase` | `invoke(itemId)` | `RequestResult<Unit>` |

---

## 7. Репозитории — полный контракт (что вызвать и что вернётся)

Если операции нет в use-case'ах — бери репозиторий (экспонируй через `UseCaseProvider`, §3).
Параметры со значением по умолчанию можно не передавать. Все возвраты — `RequestResult<T>`
(§5), кроме локальных `FavoritesRepository`/`DownloadsRepository`/`PlaybackSettingsRepository`
(они отдают `Flow`/значение напрямую). Каждый тип возврата описан в §8.

### `AuthRepository` (`:core:domain/auth`)
```kotlin
val isAuthenticated: Flow<Boolean>                       // реактивный флаг сессии
suspend fun requestDeviceCode(): RequestResult<DeviceCode>
suspend fun pollForToken(code: String, username: String, timestamp: Long): RequestResult<Token>
suspend fun refreshToken(refreshToken: String): RequestResult<Token>
suspend fun logout(): RequestResult<Unit>
```

### `CatalogRepository` (`:core:domain/catalog`)
```kotlin
suspend fun getItems(type: ItemType, sort: CatalogSort = UPDATED, page: Int = 1): RequestResult<ItemPage>
suspend fun getItemsByGenre(type: ItemType, genreId: Int, sort: CatalogSort = UPDATED, page: Int = 1): RequestResult<ItemPage>
suspend fun getHotItems(type: ItemType, page: Int = 1): RequestResult<ItemPage>
suspend fun getNewItems(type: ItemType, page: Int = 1): RequestResult<ItemPage>
suspend fun getItemDetails(id: Int): RequestResult<Item>
suspend fun getSimilarItems(id: Int): RequestResult<List<Item>>
suspend fun getGenres(): RequestResult<List<Genre>>
suspend fun getCollections(page: Int = 1): RequestResult<List<Collection>>
suspend fun getCollectionItems(collectionId: Int, page: Int = 1): RequestResult<CollectionPage>
```

### `SearchRepository` (`:core:domain/search`)
```kotlin
suspend fun search(query: String, type: ItemType? = null, perPage: Int = 20): RequestResult<List<Item>>
suspend fun searchByActor(actor: String, perPage: Int = 20): RequestResult<List<Item>>
suspend fun searchByDirector(director: String, perPage: Int = 20): RequestResult<List<Item>>
```

### `UserRepository` (`:core:domain/user`)
```kotlin
suspend fun getProfile(): RequestResult<UserProfile>
suspend fun getDeviceSettings(): RequestResult<DeviceSettings>
suspend fun updateDeviceSettings(settings: DeviceSettings): RequestResult<Unit>
suspend fun registerDevice(title: String, hardware: String, software: String): RequestResult<Unit>
suspend fun getBookmarkFolders(): RequestResult<List<BookmarkFolder>>
suspend fun getBookmarkItems(folderId: Int, page: Int = 1): RequestResult<ItemPage>
suspend fun createBookmarkFolder(title: String): RequestResult<BookmarkFolder>
suspend fun deleteBookmarkFolder(folderId: Int): RequestResult<Unit>
suspend fun addToBookmark(itemId: Int, folderId: Int): RequestResult<Unit>
suspend fun removeFromBookmark(itemId: Int, folderId: Int): RequestResult<Unit>
```

### `WatchingRepository` (`:core:domain/watching`)
```kotlin
suspend fun getHistory(type: String = "all"): RequestResult<List<WatchHistory>>
suspend fun saveProgress(itemId: Int, videoId: Int, timeSeconds: Int): RequestResult<Unit>
suspend fun saveProgressSerial(itemId: Int, season: Int, videoId: Int, timeSeconds: Int): RequestResult<Unit>
suspend fun toggleWatched(itemId: Int): RequestResult<Unit>
suspend fun toggleWatchlist(itemId: Int): RequestResult<Boolean>   // true — теперь в списке
suspend fun clearHistory(itemId: Int): RequestResult<Unit>
suspend fun getNotifications(): RequestResult<List<Notification>>
suspend fun markNotificationRead(id: Int): RequestResult<Unit>
suspend fun markAllNotificationsRead(): RequestResult<Unit>
```

### `FavoritesRepository` (`:core:domain/favorites`) — локальный, реактивный, БЕЗ `RequestResult`
```kotlin
val favorites: Flow<List<FavoriteItem>>
val favoriteIds: Flow<Set<Int>>
fun isFavorite(id: Int): Flow<Boolean>
suspend fun toggle(item: FavoriteItem): Boolean   // true — добавлен, false — убран
suspend fun add(item: FavoriteItem)
suspend fun remove(id: Int)
```

### `DownloadsRepository` (`:core:domain/downloads`) — локальный
```kotlin
val downloads: Flow<List<DownloadedItem>>
fun isDownloaded(id: Int): Flow<Boolean>
suspend fun add(item: DownloadedItem)
suspend fun remove(id: Int)
```

### `PlaybackSettingsRepository` (`:core:domain/playback`) — локальный
```kotlin
val settings: Flow<PlaybackSettings>
suspend fun setQuality(quality: String)
suspend fun setAudioLanguage(language: String)
suspend fun setSubtitleLanguage(language: String)
```

---

## 8. Доменные модели — полный справочник (структура возвращаемых типов)

Все модели — Kotlin `data class` (в Swift SKIE делает обычный класс с memberwise-инициализатором и `==`).
Соответствие типов: `Int → Int32`, `Long → Int64`, `Double → Double`, `Boolean → Bool`,
`String → String`, `T? → T?` (Optional), `List<T> → [T]`, `Set<T> → Set<T>`. `?` = может быть `nil`.

### Auth
```kotlin
DeviceCode(
  code: String,             // device_code — им поллим токен
  userCode: String,         // короткий код, который пользователь вводит на сайте
  verificationUri: String,  // URL, куда идти вводить код (kino.pub/device)
  expiresIn: Int,           // сколько секунд код действителен (общий таймаут поллинга)
  interval: Int,            // пауза между попытками поллинга, сек
)
Token(accessToken: String, refreshToken: String, expiresIn: Int)   // сохраняется data-слоем автоматически
```

### Каталог и медиа
```kotlin
Item(
  id: Int, title: String, type: ItemType, year: Int,
  plot: String, director: String, cast: String, country: String,
  genres: List<Genre>,
  rating: ItemRating,
  posters: Posters,
  duration: Duration,
  tracklist: List<MediaTrack>,   // серии/сезоны; у фильма — 1 трек
  trailer: Trailer?,
  inWatchlist: Boolean,          // в «Буду смотреть»
  finished: Boolean,             // досмотрено полностью
)

enum ItemType { MOVIE, SERIES, ANIME, DOCUMENTARY, TV }   // .apiValue: movie/serial/anime/docuserial/tv
enum CatalogSort { UPDATED, CREATED, RATING, VIEWS, YEAR } // сортировка каталога

Genre(id: Int, title: String)

ItemRating(
  filmax: Int,               // внутренний счёт 0..100
  filmaxPercentage: String,  // тот же счёт строкой с «%»
  imdb: String?,             // "7.9" или nil
  kinopoisk: String?,        // "8.1" или nil
  external: Double?,         // ВЫЧИСЛЯЕМОЕ: среднее из imdb/kinopoisk (0..10) или nil
)

Posters(small: String, medium: String, big: String, wide: String?)   // URL'ы обложек; wide — широкая

Duration(averageMinutes: Double?, totalMinutes: Int?)   // средняя длина серии / суммарная

MediaTrack(                    // серия (или единственный трек фильма)
  id: Int, number: Int, seasonNumber: Int, title: String, thumbnail: String,
  durationSeconds: Int,
  files: List<VideoFile>,      // потоки по качествам
  audios: List<AudioTrack>,
  subtitles: List<SubtitleTrack>,
  watchedSeconds: Int,         // прогресс, сек (0 — не начат)
  watchStatus: Int,            // -1 не начат, 0 в процессе, 1 досмотрен
)
VideoFile(quality: String, hls4: String?, hls: String?, http: String?)   // URL потоков (quality: "1080p"…)
AudioTrack(id: Int, lang: String?, title: String?, channels: Int)
SubtitleTrack(lang: String, url: String, shiftMs: Int)
Trailer(id: String, url: String)

Collection(id: Int, title: String, description: String?, posters: Posters?)   // подборка
CollectionPage(collection: Collection?, items: List<Item>, pagination: Pagination)
ItemPage(items: List<Item>, pagination: Pagination)
Pagination(total: Int, current: Int, perPage: Int)   // total — ВСЕГО СТРАНИЦ (не элементов); + hasNextPage: Bool
```

### Пользователь
```kotlin
UserProfile(id: Int, username: String, email: String?, avatarUrl: String?, subscription: Subscription?)
//   + extension UserProfile.initials(): String — инициалы для аватара
Subscription(active: Boolean, endsAt: Long?, daysLeft: Int?)   // endsAt — unix-время (сек)
BookmarkFolder(id: Int, title: String, count: Int, updatedAt: Long?)   // папка закладок
DeviceSettings(
  id: Int, title: String,
  supportSsl: Boolean, supportHevc: Boolean, supportHdr: Boolean, support4k: Boolean,
  mixedPlaylist: Boolean, streamingType: Int, serverLocation: Int,
)
```

### Просмотр
```kotlin
WatchHistory(itemId: Int, title: String, posterSmall: String?, progress: WatchProgress?)
WatchProgress(
  status: Int,                // -1/0/1 как watchStatus
  timeSeconds: Int?, durationSeconds: Int?,
  videoId: Int?, season: Int?,
  //   + вычисляемое fraction: Float — доля просмотра 0..1 (для прогресс-бара)
)
Notification(id: Int, title: String?, text: String?, createdAt: Long?, read: Boolean, itemId: Int?)
```

### Избранное / загрузки (локальные)
```kotlin
FavoriteItem(id: Int, title: String, posterSmall: String, year: Int, durationMinutes: Int)
DownloadedItem(id: Int, title: String, posterSmall: String, year: Int, durationMinutes: Int)
//   + extension Item.toFavoriteItem() — собрать FavoriteItem из Item
```

### Воспроизведение / Главная
```kotlin
PlaybackSettings(quality: String = "Авто", audioLanguage: String = "Оригинал", subtitleLanguage: String = "Выкл")
//   константы/списки опций: PlaybackSettings.Companion — qualityOptions ["Авто","2160p","1080p","720p","480p","360p"],
//   audioOptions ["Оригинал","Русский","English"], subtitleOptions ["Выкл","Русский","English"]

HomeFeed(hero: Item?, continueWatching: List<WatchHistory>, collections: List<Collection>,
         trending: List<Item>, forYou: List<Item>, error: String?)   // error — мягкая ошибка агрегации
```

> **Вычисляемые свойства** (`external`, `hasNextPage`, `fraction`, `initials()`) считаются в общем Kotlin-коде
> и доступны из Swift как обычные геттеры/функции — не дублируй их логику на Swift.

---

## 9. Сессия и токены

- Логин: device-flow (`requestDeviceCode` → показать `userCode`/`verificationUri` → `pollForToken` в цикле
  раз в `interval` сек до `expiresIn`). При успехе data-слой **сам сохраняет токены** (`TokenStorage` на
  `multiplatform-settings`), а `ObserveAuthStateUseCase` эмитит `true` → переключай экран.
- Logout: `LogoutUseCase().invoke()` — чистит токены, `Flow` эмитит `false`.
- Bearer-заголовок и (частично) refresh уже в общем `HttpClientFactory`. ⚠️ Полноценный
  refresh access-токена по refresh_token — известный долг (issue #39): пока при истечении сессии
  возможен форс-релогин. Учитывай при тестировании длинных сессий.

---

## 10. Рецепты (Swift)

**Авторизация (device-flow):** см. `iosApp/Onboarding/OnboardingViewModel.swift` — готовый пример.

**Загрузка Главной:**
```swift
@MainActor final class HomeViewModel: ObservableObject {
    @Published var feed: HomeFeed?
    private let getHomeFeed = UseCaseProvider.shared.getHomeFeedUseCase()
    func load() async { feed = try? await getHomeFeed.invoke() }   // HomeFeed.error — мягкая ошибка внутри
}
```

**Действие с RequestResult:**
```swift
let result = try await UseCaseProvider.shared.toggleWatchlistUseCase().invoke(itemId: 42)
switch onEnum(of: result) {
case .success(let s): inWatchlist = (s.data as? KotlinBoolean)?.boolValue ?? false
case .error(let e):   errorText = e.message
}
```

**Детали фильма через репозиторий:**
```swift
let result = try await RepositoryProvider.shared.catalog.getItemDetails(id: 42)   // RequestResult<Item>
switch onEnum(of: result) {
case .success(let s): self.item = s.data     // s.data : Item (структура — §8)
case .error(let e):   self.errorText = e.message
}
```

**Реактивный локальный источник (избранное):**
```swift
for await ids in RepositoryProvider.shared.favorites.favoriteIds { self.favIds = ids }
```

---

## 11. Как расширять shared под новый экран

1. Нужен репозиторий? Он **уже доступен** через `RepositoryProvider` — просто вызывай его метод из Swift.
2. Нужен новый доменный use-case? Добавь класс в `core:domain/usecase`, зарегистрируй в `useCaseModule`
   (`Koin.kt`) и отдай методом в `UseCaseProvider`. (Repository для этого экспонировать не нужно — он уже есть.)
3. Если возвращается тип из нового модуля, которого нет в `export(...)` фреймворка — добавь `api(project(...))`
   и `export(project(...))` в `shared/build.gradle.kts`.
4. Пересобери фреймворк (Xcode preBuildScript делает это сам), пиши Swift ViewModel + вью.

> Правило: **новую бизнес-логику не дублируй на Swift** — добавляй use-case в `core:domain` (общий для Android/iOS),
> на Swift оставляй только презентейшен (состояние экрана + UI).
