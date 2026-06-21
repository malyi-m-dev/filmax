# Filmax — План рефакторинга и развития

_Документ живой. Обновлять по мере выполнения пунктов._

---

## 1. Технический долг (найдено при ревью)

| # | Проблема | Где | Приоритет |
|---|----------|-----|-----------|
| 1 | **Нет статического анализа/форматтера** — стиль расходится (отсюда `!!` и кривые отступы) | весь проект | 🔴 высокий |
| 2 | **34 хардкод-цвета** `Color(0xFFB4305A)` вне дизайн-системы | `feature/*`, `core/ui` | 🟡 средний |
| 3 | **Строки захардкожены** в Composable («Подборки», «Смотреть», …) — мешают i18n и KMP | `feature/*` | 🟡 средний |
| 4 | **Состояние табов строкой** (`"about"/"cast"/"similar"`) вместо enum | `DetailsScreen` | 🟢 низкий |
| 5 | **Нет обработки ошибок в UI** — `error` лежит в state, но не показывается | все экраны | 🟡 средний |
| 6 | **Нет пагинации** в bottom sheet подборок (есть только page=1) | `feature:collections` | 🟢 низкий |
| 8 | **DI на Hilt** (Android-only) — блокер для KMP | 18 файлов | 🔴 высокий (для KMP) |
| 9 | **`androidx.lifecycle.ViewModel`** в фичах — Android-only | `feature/*` | 🔴 высокий (для KMP) |
| 10 | Магические `dp`/`sp` россыпью | `feature/*` | 🟢 низкий |

---

## 2. Рефакторинг — ближайшие шаги (без смены архитектуры)

1. **Подключить ktlint + detekt + `.editorconfig`** в `build-logic`, прогнать
   `ktlintFormat`, включить проверку в CI. Закрывает #1 и предотвращает регресс отступов/`!!`.
2. **Вынести цвета и формы** — заменить 34 `Color(0xFF…)` на токены `MaterialTheme.colorScheme`
   / `core:designsystem`. Единый акцент вместо повторяющегося `Color(0xFFB4305A)` (#2).
3. **Строковые ресурсы** — перенести UI-тексты в `strings.xml` (а при переходе на KMP —
   в `commonMain` ресурсы Compose Multiplatform) (#3).
4. **Единый показ ошибок** — общий `Snackbar`/error-state компонент в `core:ui`, подключить ко
   всем экранам (#5).
5. **Enum вместо строк** для табов деталей; пагинация подборок через `loadMore` по паттерну
   из старого `CategoriesViewModel.loadMoreGenreItems` (#4, #6).
6. **Тесты**: unit на `*ViewModel` (fake `CatalogRepository`) и на мапперы в `data/*`;
   далее — Compose UI-тесты ключевых экранов (#7).

---

## 3. Дорожная карта KMP (стратегическая цель)

Цель: **общий `data` + `domain` на Kotlin Multiplatform**, общий UI на Compose Multiplatform,
нативные точки входа под **iOS и Android**.

### Этап A — Подготовка (Android-only, но KMP-ready)
- Заменить **Hilt → Koin** (Koin работает в commonMain). Это снимает блокер #8.
- Заменить `androidx.lifecycle.ViewModel` на **общий `ScreenModel` + MVI**
  (State / Event / SideEffect) — паттерн, на который ориентированы агенты проекта
  (`builder-feature`, `refactor-mobile`). Снимает #9.
- Ввести `Interactor`-слой между `ScreenModel` и `Repository`.(Там где необходимо)

### Этап B — Вынос data/domain в commonMain
- Конвертировать `core:domain` (модели, репозитории-интерфейсы, `RequestResult`) в
  `commonMain` — он уже почти чистый Kotlin.
- `core:network` и `data:*`: Retrofit → **Ktor Client**; `kotlinx.serialization` уже есть.
- Хранилище токенов/настроек: Android `DataStore` → **multiplatform-settings** или
  KMP DataStore.
- DI-модули Koin перевести в `commonMain`.

### Этап C — Общий UI (Compose Multiplatform)
- `core:designsystem`, `core:ui` и `feature:*` Composable → `commonMain` Compose MP.
- Платформенные реализации: плеер (ExoPlayer/Media3 на Android, AVPlayer на iOS),
  системные эффекты, ресурсы.

### Этап D — iOS-вход
- `iosApp` (SwiftUI host + `ComposeUIViewController`), сборка фреймворка KMP,
  настройка CI под обе платформы.

### Порядок
A → B можно начинать сразу (наибольшая ценность, наименьший риск). C и D — после
стабилизации общего data/domain. Мигрировать **по одной фиче за раз**, оставляя
приложение собираемым на каждом шаге.

---

## 4. Definition of Done для миграции фичи на KMP
- `domain` и `data` фичи лежат в `commonMain`, не зависят от Android SDK.
- DI через Koin-модуль в `commonMain`.
- `ScreenModel` (MVI) вместо `ViewModel`.
- UI собирается и на Android, и на iOS.
