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

## 3. Как достать API из Swift — `UseCaseProvider`

Koin reified `get()` не виден из Swift, поэтому общий код отдаёт use-case'ы через **явные фабрики**
объекта `UseCaseProvider` (Kotlin `object` → Swift `.shared`):

```swift
let requestCode = UseCaseProvider.shared.requestDeviceCodeUseCase()
let observeAuth = UseCaseProvider.shared.observeAuthStateUseCase()
```

Сейчас экспортированы (файл `shared/.../UseCaseProvider.kt`):

| Метод | Use-case |
|---|---|
| `observeAuthStateUseCase()` | `ObserveAuthStateUseCase` |
| `requestDeviceCodeUseCase()` | `RequestDeviceCodeUseCase` |
| `pollForTokenUseCase()` | `PollForTokenUseCase` |
| `logoutUseCase()` | `LogoutUseCase` |
| `getHomeFeedUseCase()` | `GetHomeFeedUseCase` |
| `toggleWatchlistUseCase()` | `ToggleWatchlistUseCase` |
| `toggleWatchedUseCase()` | `ToggleWatchedUseCase` |

**Нужен use-case/репозиторий, которого тут нет?** Добавь метод в `UseCaseProvider` (Kotlin):

```kotlin
object UseCaseProvider : KoinComponent {
    // репозиторий напрямую…
    fun catalogRepository(): CatalogRepository = get()
    // …или новый тонкий use-case
    fun getItemDetailsUseCase(): GetItemDetailsUseCase = get()
}
```

и, если это новый use-case, зарегистрируй его в `useCaseModule` (`Koin.kt`). Пересобери фреймворк.

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

## 7. Репозитории (полный контракт data-слоя)

Если операции нет в use-case'ах — бери репозиторий (экспонируй через `UseCaseProvider`, §3).
Все `suspend fun ...: RequestResult<...>`, если не указано иное.

**`AuthRepository`** — `isAuthenticated: Flow<Boolean>`, `requestDeviceCode()`, `pollForToken(code, username, timestamp)`, `refreshToken(refreshToken)`, `logout()`.

**`CatalogRepository`** — `getItems(...)`, `getItemsByGenre(...)`, `getHotItems(type, page)`, `getNewItems(type, page)`, `getItemDetails(id): Item`, `getSimilarItems(id): List<Item>`, `getGenres(): List<Genre>`, `getCollections(page): List<Collection>`, `getCollectionItems(collectionId, page): CollectionPage`.

**`SearchRepository`** — `search(...)`, `searchByActor(actor, perPage)`, `searchByDirector(director, perPage)` → `List<Item>`.

**`UserRepository`** — `getProfile(): UserProfile`, `getDeviceSettings(): DeviceSettings`, `updateDeviceSettings(settings)`, `registerDevice(title, hardware, software)`, папки закладок: `getBookmarkFolders()`, `getBookmarkItems(folderId, page)`, `createBookmarkFolder(title)`, `deleteBookmarkFolder(folderId)`, `addToBookmark(itemId, folderId)`, `removeFromBookmark(itemId, folderId)`.

**`WatchingRepository`** — `getHistory(type): List<WatchHistory>`, `saveProgress(itemId, videoId, timeSeconds)`, `saveProgressSerial(...)`, `toggleWatched(itemId)`, `toggleWatchlist(itemId): Boolean`, `clearHistory(itemId)`, уведомления: `getNotifications(): List<Notification>`, `markNotificationRead(id)`, `markAllNotificationsRead()`.

**`FavoritesRepository`** (локальный, реактивный, без `RequestResult`) — `favorites: Flow<List<FavoriteItem>>`, `favoriteIds: Flow<Set<Int>>`, `isFavorite(id): Flow<Boolean>`, `suspend toggle(item): Boolean`, `suspend add(item)`, `suspend remove(id)`.

**`DownloadsRepository`** (локальный) — `downloads: Flow<List<DownloadedItem>>`, `isDownloaded(id): Flow<Boolean>`, `suspend add(item)`, `suspend remove(id)`.

**`PlaybackSettingsRepository`** (локальный, `:core:domain/playback`) — `settings: Flow<PlaybackSettings>`, `setQuality(q)`, `setAudioLanguage(l)`, `setSubtitleLanguage(l)`.

---

## 8. Доменные модели (что приходит в Swift)

```
DeviceCode(code, userCode, verificationUri, expiresIn, interval)
Token(accessToken, refreshToken, expiresIn)

Item(id, title, type: ItemType, year, plot, director, cast, country,
     genres: [Genre], rating: ItemRating, posters: Posters, duration: Duration,
     tracklist: [MediaTrack], trailer: Trailer?, inWatchlist, finished)
Genre(id, title)
ItemRating(filmax, filmaxPercentage, imdb?, kinopoisk?, external: Double?)   // external — усреднённый 0..10
Posters(small, medium, big, wide?)                                          // URL'ы обложек

HomeFeed(hero: Item?, continueWatching: [WatchHistory], collections: [Collection],
         trending: [Item], forYou: [Item], error: String?)

UserProfile(id, username, email?, avatarUrl?, subscription: Subscription?)
Subscription(active, endsAt: Long?, daysLeft: Int?)
DeviceSettings(id, title, supportSsl, supportHevc, …)

ItemPage(items: [Item], pagination: Pagination)   // Pagination(total, current, perPage)
```

Прочие: `Collection`/`CollectionPage`, `MediaTrack`, `Trailer`, `WatchHistory`, `Notification`,
`BookmarkFolder`, `FavoriteItem`, `DownloadedItem`, `PlaybackSettings(quality, audioLanguage, subtitleLanguage)`.
Все — `data class` (в Swift обычные классы; SKIE добавляет удобные инициализаторы/`==`).

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

**Реактивный локальный источник (избранное):** экспонируй `favoritesRepository()` в `UseCaseProvider`, затем
`for await ids in repo.favoriteIds { … }`.

---

## 11. Как расширять shared под новый экран

1. Убедись, что нужный репозиторий/use-case есть в `core:domain` (обычно да).
2. Экспонируй его в `UseCaseProvider` (Swift не видит Koin `get()`), при новом use-case — зарегистрируй в `useCaseModule`.
3. Если возвращается тип из модуля, которого нет в `export(...)` фреймворка — добавь `api(project(...))`
   и `export(project(...))` в `shared/build.gradle.kts`.
4. Пересобери фреймворк (Xcode preBuildScript делает это сам), пиши Swift ViewModel + вью.

> Правило: **новую бизнес-логику не дублируй на Swift** — добавляй use-case в `core:domain` (общий для Android/iOS),
> на Swift оставляй только презентейшен (состояние экрана + UI).
