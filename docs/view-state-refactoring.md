# Вёрстка без логики — план рефакторинга экранов

_Ветка `refactor/view-state`. Документ живой: галочки ставятся по мере прохода волн._

## Зачем

Экраны Filmax выросли из прототипа: часть решений («что играет кнопка „Смотреть“», подписи,
счётчики, таймеры карусели, какая шторка открыта) живёт в Compose-вёрстке — в `*Labels.kt`,
`remember { mutableStateOf }`, `LaunchedEffect` и классах-холдерах внутри `feature:*:mobile` и
`feature:*:tv`. Решения дублируются между телефоном и ТВ и расходятся; тестируются они только
через Robolectric-тесты вёрстки.

Образец — urentbike-technic: `View(state, onEvent)` — тупая разметка сверху вниз; ScreenModel
читается линейно и один решает, что показать и куда перейти; State плоский и готов к рендеру.
Сюда же — правила техлида оттуда, которые уже приняты как стиль: без вычислений внутри
`updateState`, без extension-функций, задержки «для глаз» — в анимациях вёрстки, всё остальное —
в модели.

## Что нашли (master, 26.10.2026)

| Признак | Где | Сколько |
|---|---|---|
| `LaunchedEffect` в UI-модулях фич | `feature/*/mobile`, `feature/*/tv` | 73 (ещё 20 в `core:tv-designsystem`, 9 в `:app`) |
| `remember { mutableStateOf }` в UI фич | там же | 64 |
| `derivedStateOf` / `delay(` в UI фич | там же | 26 / 10 |
| Классы, объявленные в UI-модулях | там же | ~90 (без тестов): ~20 `*Actions`, ~12 `*Focus`-холдеров, 3 холдера плеера |
| Файлы подписей/правил в UI | `DetailsLabels`, `TvDetailsLabels`, `TvHomeLabels`, `TvCollectionLabels`, `MineLabels`, `TvPlayerLabels`… | 9 |
| Extension-функции в `feature:*:common` | `*Logic.kt`, `*Format.kt`, `with*()` над State | ~40 |
| Getter'ы с логикой в State | Home, Search, Collections, Details, Remote, Feedback | 7 контрактов |
| Навигация из вёрстки | `Navigator.homeActions()`, `detailsActions(navigator)` … | каждый экран |

Характерные места:

- `feature/details/mobile/DetailsLabels.kt` — `playTarget()` решает, какую серию играет кнопка
  «Смотреть». Это бизнес-правило, и на ТВ оно записано второй раз (`TvDetailsLabels.playLabel`).
- `TvHomeHero`: `LaunchedEffect(focused) { delay(8 с); onNext() }` — таймер карусели меняет State
  из вёрстки. Онбординг: `LaunchedEffect(state.step) { if (step == FEATURES) dispatch(NextStep) }`.
- Плеер: позиция, буфер, «играет/буферизует», перемотка и отсчёт «Далее» — в `PlaybackUi`
  (телефон) и `TvTransportState` (ТВ), каждый со своим тикером; модель получает только
  `SaveProgress`. Коммит перемотки через 700 мс — `delay` в вёрстке, а он ведёт к `seekTo`.
- Какая шторка открыта (`CatalogSheet`, `MineConfirm`, `sectionsOpen`, `streamingSheet`) —
  `remember`/`rememberSaveable` в экране; `MineConfirm` — sealed-класс в UI-модуле.
- `HomeState.hero`, `SearchState.visibleItems/bestMatch/otherResults`, `DetailsState.seasonEpisodes`,
  `CollectionDetailState.titlesCount` — вычисляются в getter'ах State на каждом чтении.
- `HomeScreenModel.updateRows { transform }`, `withTitlesPaging { paging -> … }` — хелперы с
  лямбдой-трансформацией и `map` внутри `updateState`.

## Правила (закреплены в CLAUDE.md и refactoring-guidelines.md)

1. **Вёрстка получает `state` и `onEvent` — и ничего больше.** Никаких `*Actions`-объектов и
   лямбд навигации сверху. Переход — это событие (`OpenItem(id)`, `Play(...)`), как и тап.
2. **Навигацию решает ScreenModel.** `Navigator` — синглтон Koin, модель получает его через
   конструктор и зовёт `navigator.open(Destination.…)` из обработчика события. Экран оставляет
   себе `koinViewModel()`, `collectAsState()` и `collectSideEffect` для того, что умеет только
   вью (share-intent, системные диалоги, фокус).
3. **State плоский и готов к рендеру.** Подписи, цель кнопки, видимость, счётчики — поля,
   посчитанные моделью в одном месте. Getter в State — только тривиальный предикат (`isNotEmpty`).
   Форматтеры дизайн-системы (`posterMeta`, `ratingLabel`, `typeLabel`, `durationLabel` из
   `core:ui`) — часть компонентов, их вёрстка зовёт сама.
4. **Задержки.** Для глаз — вёрстка: `snap(delayMillis)`, автоскрытие контролов, затухание
   скроллбара, «скопировано» на кнопке. Меняет State, шлёт запрос или ведёт на другой экран —
   модель: таймер карусели, отсчёт «Далее», коммит перемотки, тики прогресса, debounce поиска.
5. **ScreenModel линейный.** `onEvent` — одна строка на событие → `private fun onXxx()`. Перед
   `updateState` — готовые `val`, внутри — только `copy`. Защёлка «уже идёт» — `Job?.isActive`,
   не флаг в State, если флаг не нужен вёрстке. Без extension-функций и хелперов с лямбдой
   `transform` — приватные методы класса или методы data class.
6. **Классы в UI-модуле** — только `Route`-объекты и связки `FocusRequester`'ов (ТВ-фокус — вещь
   вёрстки). Всё, что хранит решение, — State; локальное — `remember` в самом composable.
7. **Локальное UI-состояние** (`remember { mutableStateOf }`) — только то, чего модели знать не
   надо: фокус, анимация, измеренная высота, драг. Открытые шторки, диалоги, подтверждения,
   черновики форм — State.

## Что сознательно не делаем в этой ветке

- Не вводим UI-модели карточек (`TitleCard` вместо `Item` в списках). Списки в State остаются
  domain-моделями, карточки форматируют их форматтерами `core:ui`. Это следующий этап: перенос
  форматтеров в `core:presentation` и маппинг `Item → TitleCard` в моделях.
- Карусель hero на телефоне остаётся на `HorizontalPager` с его автолистанием: страница — это
  состояние пейджера, модель про неё не спрашивают. На ТВ hero — поле State, и таймер у модели.
- Догрузка «у хвоста» (`derivedStateOf` по `layoutInfo`) остаётся в вёрстке: только она знает
  геометрию прокрутки. Копии сводятся к одному хелперу в `core:ui` / `core:tv-designsystem`.
- Фокус ТВ (`rememberTvScreenFocus`, `FocusRequester`, `onPlaced`) не трогаем — это вёрстка.

## Волны

Каждая волна: `common` (State/Event/ScreenModel + Navigator) → `mobile` → `tv` → тесты →
`./gradlew detekt detektMain :app:assembleDebug testDebugUnitTest` → один коммит
`refactor(<фича>): …`. UI проверяется на mock-сборке (`:app:installMock`), ТВ — первым.

- [x] **0. Правила и план** — этот документ, `CLAUDE.md`, `refactoring-guidelines.md`,
  `FakeNavigator` в `:data:fake`.
- [ ] **1. Home (пилот).** `HomeActions`/`HeroActions`/`RailActions`/`TvHomeActions` → `onEvent`;
  навигация и `AskFeedback` → модель; заголовки рядов → `HomeRow.title/subtitle`; `hero`,
  `isEmpty` → поля; таймер карусели ТВ → модель (`HeroFocused`); шторка разделов → State;
  `HomeRowsLogic` → методы модели и `RowPaging`; `FeedFocus`/`HeroModel`/`FeedLayout` → убрать.
- [ ] **2. Details + FolderPicker.** `playTarget`/`PlayLabel`/`overline`/`ratingBadges`/`metaLine`/
  `sheetMeta`/`seasonSummary` → `DetailsState`; `seasonEpisodes` → поле; `*Actions` → `onEvent`;
  навигация (плеер, трейлер, фильмография, похожие, ТВ) → модель; share → SideEffect.
- [ ] **3. Library (Mine, Folder).** `MineConfirm` → State; `MineLabels` → State; `*Actions` →
  `onEvent`; `Refresh` при возврате остаётся событием жизненного цикла из вёрстки.
- [ ] **4. Search / Catalog / Browse / Filmography.** `visibleItems/bestMatch/otherResults` → поля;
  `CatalogSheet` и черновик фильтров → State; `SheetOpeners`/`CatalogVoice` → State + события;
  extension-функции `SearchScreenModel` → методы; `LoadCatalog` из `LaunchedEffect(Unit)` →
  `init`/маршрут.
- [ ] **5. Collections.** `titlesCount`/`headerCollapsed`/`startItemId` → поля; `TvCollectionLabels`
  → State или `core:tv-designsystem` (`scrollThumb` общий с Home); `*Actions` → `onEvent`.
- [ ] **6. Profile + DeviceSettings.** `ProfileFormat` → `ProfileState`; `streamingSheet` → State;
  `SettingRows`/`DeviceToggles` → убрать.
- [ ] **7. Onboarding, Feedback, TvLink.** `LaunchedEffect(state.step) { dispatch(NextStep) }` →
  модель; `FeedbackState.title` → поле; `RemoteState.playback/positionMs/reconnectsItself` → поля;
  `TvLinkTexts` → State.
- [ ] **8. Player.** Снимок плеера (`isPlaying/isBuffering/positionMs/durationMs/bufferedMs`),
  перемотка (`scrubTargetMs`) и отсчёт «Далее» → `PlayerState`, один тикер в модели;
  `PlaybackUi`/`ControlsUi`/`TvTransportState` → убрать (клавиша → событие в ТВ-вёрстке);
  автоскрытие оверлея остаётся в вёрстке. Проверка на обоих эмуляторах обязательна.
- [ ] **9. Итог.** Повторный замер таблицы выше, обновление `docs/ARCHITECTURE.md`
  («Поток данных»), абзац в `CHANGELOG.md`.

## Критерий готовности волны

- В UI-модуле не осталось `*Actions`, навигации и `*Labels.kt`; `LaunchedEffect`/`delay` — только
  анимации, фокус и события жизненного цикла (`Refresh` при возврате, «хвост ряда виден»).
- ScreenModel: `onEvent` из одностроковых веток; `updateState { it.copy(...) }` без вычислений;
  extension-функций нет; у новых решений — юнит-тест на фейках `:data:fake` (`FakeNavigator`
  проверяет переходы).
- Поведение не изменилось: тесты зелёные, экраны проверены на mock-сборке (ТВ — первым).
