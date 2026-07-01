# Filmax — План рефакторинга и развития

_Документ живой. Обновлять по мере выполнения пунктов._

---

## 1. Технический долг (найдено при ревью)

| # | Проблема | Где | Приоритет |
|---|----------|-----|-----------|
| 1 | ✅ ~~**Нет статического анализа/форматтера**~~ — подключены **detekt + ktlint**, baseline проекта = 0, добавлен гейт `detektMain` (type-resolution) в CI | весь проект | ✅ закрыто |
| 2 | **34 хардкод-цвета** `Color(0xFFB4305A)` вне дизайн-системы | `feature/*`, `core/ui` | 🟡 средний |
| 3 | **Строки захардкожены** в Composable («Подборки», «Смотреть», …) — мешают i18n и KMP | `feature/*` | 🟡 средний |
| 4 | **Состояние табов строкой** (`"about"/"cast"/"similar"`) вместо enum | `DetailsScreen` | 🟢 низкий |
| 5 | **Нет обработки ошибок в UI** — `error` лежит в state, но не показывается | все экраны | 🟡 средний |
| 6 | **Нет пагинации** в bottom sheet подборок (есть только page=1) | `feature:collections` | 🟢 низкий |
| 8 | ✅ ~~**DI на Hilt** (Android-only) — блокер для KMP~~ — мигрировано на **Koin 4.0.4** | — | ✅ закрыто |
| 9 | ✅ ~~**`androidx.lifecycle.ViewModel`** в фичах — Android-only~~ — мигрировано на общий **`BaseScreenModel` + MVI** (`core:presentation`) | — | ✅ закрыто |
| 10 | Магические `dp`/`sp` россыпью | `feature/*` | 🟢 низкий |

---

## 2. Рефакторинг — ближайшие шаги (без смены архитектуры)

1. **Подключить ktlint + detekt + `.editorconfig`** в `build-logic`, прогнать
   `ktlintFormat`, включить проверку в CI. Закрывает #1 и предотвращает регресс отступов/`!!`.
2. **Вынести цвета и формы** — заменить 34 `Color(0xFF…)` на токены `MaterialTheme.colorScheme`
   / `core:designsystem`. Единый акцент вместо повторяющегося `Color(0xFFB4305A)` (#2).
3. **Строковые ресурсы** — перенести UI-тексты Android в `strings.xml` (#3). (На iOS тексты
   нативные — презентейшен раздельный, не общий.)
4. **Единый показ ошибок** — общий `Snackbar`/error-state компонент в `core:ui`, подключить ко
   всем экранам (#5).
5. **Enum вместо строк** для табов деталей; пагинация подборок через `loadMore` по паттерну
   из старого `CategoriesViewModel.loadMoreGenreItems` (#4, #6).
6. **Тесты**: unit на `*ViewModel` (fake `CatalogRepository`) и на мапперы в `data/*`;
   далее — Compose UI-тесты ключевых экранов (#7).

---

## 3. Дорожная карта KMP (стратегическая цель)

Цель: **общий `data` + `domain` на Kotlin Multiplatform**, а презентейшен — нативный на каждой
платформе: Android — Compose, **iOS/Apple TV — SwiftUI (НЕ Compose Multiplatform)**.

### Этап A — Подготовка (Android-only, но KMP-ready)
- ✅ ~~Заменить **Hilt → Koin**~~ — **сделано** (Koin 4.0.4, DSL-модули по одному на Gradle-модуль,
  `koinViewModel`/`koinNavViewModel`, `startKoin` в `FilmaxApplication`). Снят блокер #8. KSP удалён.
  ✅ Переезд `koinNavViewModel` → `koinViewModel` сделан (Details/Player; `SavedStateHandle`
  из nav-бэкстека резолвится корректно). Осталось: проверка DI-графа на устройстве.
- ✅ ~~Заменить `androidx.lifecycle.ViewModel` на **общий `ScreenModel` + MVI**
  (State / Event / SideEffect)~~ — **сделано**. Модуль `core:presentation` с
  `BaseScreenModel<STATE, SIDE_EFFECT, EVENT>` (наследует `ViewModel` — единственное
  место касания androidx; `dispatch`/`updateState`/`postSideEffect`/`collectAsState`/
  `collectSideEffect`). Все 8 фич + `RootScreenModel` мигрированы, контракты вынесены в
  `*Contract.kt` (State/Event/SideEffect). `Onboarding`→`SideEffect.Authenticated`,
  `Profile`→`SideEffect.LoggedOut`. Koin `viewModelOf`/`koinViewModel` без изменений.
  Снят #9. Осталось (Этап C): вынести `BaseScreenModel` в commonMain поверх Decompose.
- Ввести `Interactor`-слой между `ScreenModel` и `Repository`.(Там где необходимо)

### Этап B — Вынос data/domain в commonMain ✅ сделано
- ✅ `core:domain` (модели, репозитории-интерфейсы, `RequestResult`, UseCase) — в `commonMain`.
- ✅ `core:network` + `data:*` на **Ktor Client** + `kotlinx.serialization`; общий код в `commonMain`,
  платформенное (Darwin-клиент Ktor, `KeychainSettings`) — в `appleMain`.
- ✅ Хранилище токенов/настроек — **multiplatform-settings**; DI-модули Koin — в `commonMain`.
- ✅ Модуль `:shared` собирает фреймворк `Shared` (таргеты iOS + tvOS) c **SKIE**;
  `RepositoryProvider`/`UseCaseProvider` — Swift-фасады общего слоя.

### Этап C — iOS/Apple TV презентейшен на SwiftUI (в работе)
- **НЕ Compose Multiplatform.** iOS-презентейшен пишется нативно на **SwiftUI** поверх общего
  data/domain; ViewModel'и (Swift `ObservableObject`) переиспользуют KMP use-case'ы/репозитории.
- Платформенные движки: плеер — Media3/ExoPlayer на Android, **AVPlayer** на iOS.
- Роадмап экранов iOS/tvOS — в Backlog доски (метка `ios`).

### Этап D — iOS/tvOS-приложение ✅ каркас готов
- ✅ `iosApp/` — таргеты `Filmax` (iPhone/iPad) и `Filmax-tvOS` (Apple TV) на SwiftUI,
  `KoinKt.doInitKoin()` на старте, онбординг + авторизация на общем слое (XcodeGen `project.yml`).
- Осталось: экраны (Главная/Детали/Плеер/…), iOS-CI. Сборка — `docs/BUILD_MACOS.md`.

### Порядок
A → B можно начинать сразу (наибольшая ценность, наименьший риск). C и D — после
стабилизации общего data/domain. Мигрировать **по одной фиче за раз**, оставляя
приложение собираемым на каждом шаге.

---

## 4. Definition of Done для миграции фичи на KMP
- `domain` и `data` фичи лежат в `commonMain`, не зависят от Android SDK.
- DI через Koin-модуль в `commonMain`.
- `ScreenModel` (MVI) вместо `ViewModel`.
- UI: Android — Compose; iOS/Apple TV — SwiftUI поверх общего слоя (не Compose MP).
