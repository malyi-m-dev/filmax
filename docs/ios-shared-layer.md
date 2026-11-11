# Filmax — общий data/domain-слой для iOS

Гид для iOS-разработчика: как из Swift работать с общей логикой KMP. Презентейшен iOS и tvOS —
**Swift/SwiftUI** (`iosApp/`), вся data/domain-логика — из фреймворка `Shared`.

> Сборка — [`BUILD_MACOS.md`](BUILD_MACOS.md), структура приложения — [`iosApp/README.md`](../iosApp/README.md).

## 1. Что общее, что нет

| Слой | Где | Платформа |
|---|---|---|
| domain: модели, контракты репозиториев, use case'ы | `:core:domain` (`commonMain`) | общий |
| data: kino.pub, домашний сервер Jellyfin, переключатель источника | `:data:*`, `:core:network` | общий |
| фасад для Swift: Koin, провайдеры, `CatalogKit`, `HomeShelves` | `:shared` → фреймворк `Shared` | общий |
| презентейшен: ViewModel'и и экраны | `iosApp/` | iOS/tvOS |

`:core:presentation` и `:feature:*` — Android. Логика экранов, которая должна совпадать с Android
(запрос витрины, досортировка, ряды Главной), живёт в домене или в `:shared`, а не дублируется в Swift.
Фреймворк реэкспортирует `core:domain`, `core:network`, `data:auth`, `data:catalog`, `data:search`,
`data:user`, `data:watching`, `data:jellyfin`, `data:source`. Подключён **SKIE**.

## 2. Инициализация

```swift
let client = ClientInfo(appVersion: version, isTv: false, device: UIDevice.current.model,
                        os: "iOS \(UIDevice.current.systemVersion)")
KoinKt.doInitKoin(client: client, appDeclaration: { _ in })
```

`ClientInfo` нужен домашнему серверу (заголовок клиента Jellyfin, профиль кодеков ТВ). Дефолтные
параметры Kotlin в Swift не видны — `appDeclaration` передаётся явно.

## 3. Точки входа

`RepositoryProvider.shared` — репозитории текущего источника (переключатель источника внутри):

| Свойство | Контракт | Для чего |
|---|---|---|
| `auth` | `AuthRepository` | вход kino.pub (device flow), `isAuthenticated`, выход |
| `catalog` | `CatalogRepository` | выдача, детали, похожие, жанры, страны, подборки |
| `search` | `SearchRepository` | поиск по названию, актёру, режиссёру |
| `user` | `UserRepository` | профиль, папки-закладки |
| `watching` | `WatchingRepository` | история, прогресс (`saveProgress(itemId:video:timeSeconds:)`), «просмотрено» |
| `watchlist` | `WatchlistRepository` | «Буду смотреть»: `items`, `contains`, `add`, `remove`, `adopt` |
| `playbackSettings` | `PlaybackSettingsRepository` | качество, озвучка, субтитры |
| `sources` | `ContentSourceRepository` | `source.value` — kino.pub или домашний сервер, `select` |
| `homeServer` | `HomeServerRepository` | поиск сервера в сети, адрес, пароль, Quick Connect |
| `streams` | `StreamRepository` | потоки трека под запуск (домашний сервер выдаёт ссылки на каждый старт) |

`UseCaseProvider.shared`: `getHomeFeedUseCase()`, `observeAuthStateUseCase()`,
`requestDeviceCodeUseCase()`, `pollForTokenUseCase()`, `logoutUseCase()`, `toggleWatchlistUseCase()`,
`toggleWatchedUseCase()`, `bookmarkFolders()`, `homeShelves()`.

Общая логика экранов:

- `CatalogKit.shared` — фильтры без условий (`noFilters`), возможности источника
  (`capabilities(source:)`), запрос витрины по чипам (`query(type:genreId:filters:sort:)`), правка
  фильтров по полю (`withYears`, `withMinRating`, `withCountry`, `withOnly4k`, `withFinished`).
- `CatalogPager(catalog:)` — страницы витрины по нескольким типам: `first(query:)`, `next()`,
  `endReached`, `count(shown:)`. Ответ — `CatalogPage` (`.loaded`, `.failed`, `.unchanged`, `.outdated`).
- `ArrangeCatalog.shared` — досортировка и повторы id: `firstPage`, `nextPage`, `searchResults`.
- `HomeShelves.load(shelf:page:)` — тематические ряды Главной (`HomeShelf`: популярные сериалы,
  мультфильмы, комедии, фантастика, аниме, дорамы, документальное, ТВ-шоу, концерты).

## 4. Возможности источника

`SourceCapabilities`: какие оценки (`ratings`) и сортировки (`sorts`) есть, есть ли страны, папки,
подписка. У домашнего сервера нет папок, стран и оценок КиноПоиска — экран не показывает кнопку
«В папку», сегмент «Папки», фильтр стран и порог КП, а не ошибку после нажатия.

## 5. Модели, которые легко перепутать

- **Оценки** — `ItemRating.values: [Rating(source, value)]`, `primary` — главная оценка источника
  (на постер). Подпись — по `RatingSource`: «КП», «IMDb», у сервера — «Рейтинг».
- **Адрес видео** — `VideoRef`: `VideoRefEpisode(season:number:)` или `VideoRefMovie(number:)`. `number` —
  НОМЕР видео (`MediaTrack.number`), а не id трека: тем же числом kino.pub принимает и отдаёт прогресс.
  Номер серии уникален только в сезоне. У трека — `track.ref`. В `NavigationStack` кладётся Swift-зеркало
  `PlayTarget` (Kotlin-интерфейс не `Hashable`).
- **«Буду смотреть»** — список источника, а не флаг тайтла: кнопка следит за `watchlist.contains`,
  отметку с другого устройства детали отдают в `watchlist.adopt`.
- **`watchStatus`**: −1 — не начат, 0 — в процессе, 1 — досмотрен.

## 6. Интероп (проверено на Mac)

- `RequestResult<T>` — sealed interface, в Swift — протокол без generic-параметра: разбирается
  `onEnum(of:)`, данные успеха приходят как `Any?` и приводятся к типу (`success.data as? Item`).
  В приложении это делает `request(fallback:_:)` из `Shared/Support/KotlinInterop.swift`.
- `suspend` → `async throws`; `Flow` → `AsyncSequence` (`for await value in flow`); `StateFlow` — ещё
  и `.value`.
- `Int` → `Int32`, `Int?` → `KotlinInt?`, `Double?` → `KotlinDouble?`, `Boolean` в Flow — `KotlinBoolean`.
- Kotlin `enum` → Swift `enum` с case'ами в lowerCamelCase (`ItemType.tvShow`, `ContentSource.jellyfin`).
- `object` → `.shared` (`CatalogKit.shared`), companion → `.Companion.shared`
  (`PlaybackSettings.Companion.shared.qualityOptions`).
- Имена, совпадающие со стандартной библиотекой, — через псевдонимы: `FilmCollection`, `FilmSubscription`.

## 7. Что на iOS не повторяется

- Связь телефона с телевизором (`data:tvlink`) — Android-протокол в локальной сети.
- Окно отзыва (`data:feedback`, Firestore) и фото актёров TMDB — ключи и SDK только в Android-сборке.
- Внешние субтитры SRT в `AVPlayer` не подгружаются; озвучки и вшитые субтитры HLS выбираются
  системным меню плеера.
