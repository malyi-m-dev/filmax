# Filmax

Приложение для просмотра фильмов и сериалов (API kino.pub) — **одно приложение на
телефоне и Android TV**. Современный стек: 100% Kotlin, Jetpack Compose, многомодульная
архитектура, Material 3 Expressive дизайн.

> Авторизация — OAuth2 device-flow. Бэкенд: `https://smarttvcdn.online/`.

---

## Стек

| Слой | Технологии |
|------|-----------|
| Язык | Kotlin 2.0.21 |
| UI (телефон) | Jetpack Compose (BOM 2025.05.01), Material 3, Coil 3 |
| UI (Android TV) | `androidx.tv.material3` — D-pad фокус, 10-foot UI |
| Навигация | Navigation Compose 2.8.9 (типобезопасные routes на `@Serializable`) |
| DI | Koin 4.0.4 |
| Сеть | Ktor Client 3.x + `kotlinx.serialization` (+ Chucker — инспектор запросов в debug) |
| Локальное хранилище | multiplatform-settings (избранное, загрузки) |
| Плеер | Media3 ExoPlayer |
| Асинхронность | Coroutines 1.9 + Flow |
| Сборка | AGP 8.7.3, Gradle (convention plugins в `build-logic`) |
| CI | GitHub Actions: сборка debug-APK + доставка в Telegram |
| SDK | minSdk 26, target/compileSdk 35 |

---

## Архитектура

Многомодульная, разбита по слоям. Зависимости однонаправленные:
`app → feature → core/data → core:domain`.

### Один APK, два форм-фактора

Сборка одна (`:app`, один `applicationId`). Манифест объявляет `LAUNCHER` и
`LEANBACK_LAUNCHER`, поэтому APK ставится и на телефон, и на ТВ. `MainActivity`
выбирает граф навигации по типу устройства:

```kotlin
val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
if (isTv) FilmaxTvTheme { FilmaxTvNavGraph() } else FilmaxTheme { FilmaxNavGraph() }
```

### Вложенная структура фич

Каждая фича разбита на **логику** и **UI на форм-фактор** — TV не тянет на classpath
телефонный UI и наоборот, а логика (ScreenModel) не дублируется:

```
:feature:home            # логика: HomeScreenModel + *Contract + DI (без UI)
:feature:home:mobile     # телефонный Screen + navigation
:feature:home:tv         # TV Screen + navigation   (оба api-зависят от :feature:home)
```

```
app/                         # один таргет: телефон + Android TV
├─ MainActivity              # выбор UI по FEATURE_LEANBACK
├─ navigation/               # FilmaxNavGraph (телефон, нижний таб-бар)
└─ tv/navigation/            # FilmaxTvNavGraph + TvTopNavBar (ТВ, верхний таб-бар)
core/
├─ domain/                   # модели, интерфейсы репозиториев, RequestResult, UseCase
├─ network/                  # сетевой клиент, токены
├─ presentation/             # BaseScreenModel (MVI: State/Event/SideEffect)
├─ designsystem/             # цвета, типографика, формы, тема (телефон)
├─ tv-designsystem/          # FilmaxTvTheme, TvButton, TvFocusCard (androidx.tv.material3)
└─ ui/                       # переиспользуемые Composable (PosterImage, FilmaxTabBar, …)
data/                        # реализации репозиториев + DTO + мапперы
└─ auth/ catalog/ search/ user/ watching/
feature/
├─ onboarding/ home/ search/ collections/ library/ profile/ details/ player/
│     └─ каждая: <feature>(логика) + <feature>:mobile + <feature>:tv
└─ designsystem/             # только телефон (каталог компонентов, debug)
```

> Для `details`/`player` маршрут (`DetailsRoute`/`PlayerRoute`) лежит в логическом модуле —
> его читает `ScreenModel` через `SavedStateHandle.toRoute<…>()`, а навбилдеры в
> `:mobile`/`:tv` ссылаются на него.

**Поток данных (MVI, однонаправленный):** `Composable` (`collectAsState` / `collectSideEffect`,
события через `dispatch(Event)`) → `ScreenModel` (`BaseScreenModel`, `State` + одноразовые
`SideEffect`) → `Repository` (`RequestResult<T>`: `Success` / `Error`) → сеть.

### Экраны

- **Телефон** (нижний таб-бар): Главная · Поиск · Подборки · Библиотека · Профиль, плюс
  Детали и Плеер.
- **Android TV** (верхний таб-бар, D-pad): Главная · Поиск · Подборки · Библиотека · Профиль,
  плюс Детали и Плеер.

Подробнее про TV — [`docs/TV.md`](docs/TV.md).

---

## Ключевые возможности

- **Дизайн-система** — переиспользуемые `Filmax*`-компоненты в `core:ui`; TV-аналоги
  (`TvButton`, `TvFocusCard`, `FilmaxTvTheme`) — в `core:tv-designsystem` на
  `androidx.tv.material3` с нативным фокусом из коробки.
- **Избранное** — серверный watchlist (`togglewatchlist`) + локальный кэш как источник
  списка для Библиотеки; синхронизация на тоггле и импорт `inWatchlist` в Деталях.
- **Загрузки** — метаданные скачанного локально, отображение в Библиотеке.
- **Система ошибок** — `AppError` + резолвер в `core:domain`, единая модалка
  `FilmaxErrorModal`; из `ScreenModel` вызывается `showError(...)`.
- **Плеер** — Media3 ExoPlayer, выбор качества из бэкенда, переключение субтитров на лету.
- **Настройки воспроизведения** — качество видео и язык аудио/субтитров (Профиль),
  сохраняются локально и применяются в плеере.
- **Регистрация устройства** — `device/notify` при успешной авторизации.

---

## Что осталось сделать

- [ ] **TV: Профиль** — показывает реальный аккаунт/настройки вместо мульти-профиля из макета.
- [ ] **Реальное офлайн-кэширование** загрузок (сейчас только метаданные).
- [ ] Серверная синхронизация **списка** избранного (у API нет ручки на список).
- [ ] Экран **«Списки»/закладки** (папки `bookmarks`) и управление ими.
- [ ] Полноэкранный режим и регулировка громкости в плеере.
- [ ] **Чистка ScreenModel-ей** — вынести воспроизведение/деривации за интерфейсы и UseCase,
      убрать платформенные зависимости из presentation (подготовка к KMP).

---

## Сборка и запуск

Требуется JDK 17 и Android SDK 35.

```bash
# Debug APK (один — и на телефон, и на Android TV)
./gradlew :app:assembleDebug

# Установить на устройство/эмулятор
./gradlew :app:installDebug

# Скомпилировать конкретный модуль
./gradlew :feature:home:tv:compileDebugKotlin
```

`local.properties` (путь к Android SDK) и каталоги `build/` не коммитятся — см. `.gitignore`.

### CI → Telegram

`.github/workflows/android-build.yml` собирает `:app:assembleDebug` на пуш и по кнопке
(Actions → Run workflow), кладёт APK в Artifacts и присылает в Telegram. Нужно один раз
добавить секреты `TELEGRAM_BOT_TOKEN` и `TELEGRAM_CHAT_ID` (Settings → Secrets and variables
→ Actions); без них шаг отправки пропускается.

---

## Дизайн

Дизайн ведётся в Claude Design и портируется поэкранно в Compose. Токены маппятся на
`core:designsystem` (`Color` / `Type` / `Shape`); TV переиспользует их в `core:tv-designsystem`
с затемнёнными под 10-foot поверхностями и focus-цветом. Акцент — `#B4305A`.
TV-макет: [`docs/design/filmax-tv-all-screens.html`](docs/design/filmax-tv-all-screens.html).

---

## Развитие

Текущий техдолг и дорожная карта (в т.ч. планируемая миграция на **Kotlin Multiplatform**:
общий `data`/`domain` + Compose Multiplatform под iOS и Android) —
в [`docs/REFACTORING_PLAN.md`](docs/REFACTORING_PLAN.md).
