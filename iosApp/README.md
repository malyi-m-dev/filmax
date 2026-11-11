# Filmax iOS + tvOS

Apple-приложения Filmax на iPhone/iPad и Apple TV — те же разделы и сценарии, что у Android, но
нативные: `TabView` и `NavigationStack`, формы и списки iOS, системный плеер `AVPlayerViewController`,
фокус Apple TV. Data/domain — общий KMP-слой (фреймворк `Shared`).

> Требуется **iOS/tvOS 16.0+**. Справочник по общему слою для Swift —
> [`docs/ios-shared-layer.md`](../docs/ios-shared-layer.md), сборка на Mac — [`docs/BUILD_MACOS.md`](../docs/BUILD_MACOS.md).

## Что есть

| Раздел | iPhone/iPad | Apple TV |
|---|---|---|
| Вход | kino.pub по коду или домашний сервер Jellyfin (поиск в сети, адрес, пароль, Quick Connect) | то же, на ТВ по умолчанию — Quick Connect |
| Главная | герой, «Продолжить», новинки, подборки, тематические ряды как на Android | то же рядами |
| Каталог | чипы типа и жанра, сортировка и фильтры по возможностям источника, подгрузка | то же, поиск — внутри Каталога |
| Поиск | отдельная вкладка, недавние и подсказки | — |
| Моё | Продолжить · Буду смотреть · Папки · История; папок у домашнего сервера нет | то же |
| Детали | оценки с подписью источника, сезоны и серии, «Буду смотреть», «В папку», состав, похожие | то же |
| Плеер | `AVPlayer`: прогресс по адресу серии, качество, запасной CDN, следующая серия | то же |
| Профиль | аккаунт и подписка, источник, воспроизведение, выход | то же |

Не повторяют Android: связь телефона с телевизором (протокол Android в локальной сети), окно отзыва
и фото актёров TMDB (ключи и SDK только в Android-сборке), внешние субтитры SRT.

## Устройство

- **Общее (KMP, `:shared`):** репозитории обоих источников, use case'ы, переключатель источника,
  фасад для Swift (`CatalogKit`, `HomeShelves`, `CatalogPager`, `ArrangeCatalog`). Логика, которая
  должна совпадать с Android, живёт здесь, а не в Swift — её проверяют тесты `:shared` и домена.
- **`Shared/`** — Swift для обоих таргетов: ViewModel'и экранов, тема, маршруты (`AppRoute`,
  `PlayTarget`), интероп (`Support/KotlinInterop.swift`), поверхность плеера.
- **`iOS/`** и **`tvOS/`** — SwiftUI-экраны своих таргетов. ViewModel'и общие, различаются только View.

Смена источника пересобирает вкладки (`.id(session.source)`), как граф Android возвращается к корню.

## Как собрать (только macOS + Xcode)

1. `brew install xcodegen`, в `iosApp/` — `xcodegen generate`.
2. Открыть `Filmax.xcodeproj`, схема **`Filmax`** (iPhone/iPad) или **`Filmax-tvOS`** (Apple TV), Run.
   Перед сборкой прогоняется `:shared:embedAndSignAppleFrameworkForXcode` (preBuildScript в `project.yml`).
3. Для устройства — `DEVELOPMENT_TEAM` в `project.yml`.

Swift-код в репозитории собирается только на Mac: в CI и в Linux-окружении проверяется Kotlin-часть
(`./gradlew :shared:testDebugUnitTest`). После правок общего слоя соберите обе схемы.

## Заметки по интеропу (проверено на Mac)

- `Flow<Boolean>` приходит как `KotlinBoolean` → `.boolValue`.
- `RequestResult<T>` — sealed interface: в Swift протокол без generic-параметра; разбор — `onEnum(of:)`,
  данные успеха — `Any?` с приведением типа. Обёртка — `request(fallback:_:)`.
- `suspend` → `async throws`, `object` → `.shared`, companion → `.Companion.shared`.
- `KoinKt.doInitKoin(client:appDeclaration:)` — дефолтные параметры Kotlin в Swift не видны, всё явно.
