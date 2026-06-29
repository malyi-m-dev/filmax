# Filmax

Android-приложение для просмотра фильмов и сериалов (API kino.pub).
Современный стек: 100% Kotlin, Jetpack Compose, многомодульная архитектура,
Material 3 Expressive дизайн.

> Авторизация — OAuth2 device-flow. Бэкенд: `https://smarttvcdn.online/`.

---

## Стек

| Слой | Технологии |
|------|-----------|
| Язык | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2025.05.01), Material 3, Coil 3 |
| Навигация | Navigation Compose 2.8.9 (типобезопасные routes на `@Serializable`) |
| DI | Koin 4.0.4 |
| Сеть | Ktor Client 3.x + `kotlinx.serialization` (+ Chucker — инспектор запросов в debug) |
| Локальное хранилище | multiplatform-settings (избранное, загрузки) |
| Плеер | Media3 ExoPlayer |
| Асинхронность | Coroutines 1.9 + Flow |
| Сборка | AGP 8.7.3, Gradle (convention plugins в `build-logic`), KSP |
| SDK | minSdk 26, target/compileSdk 35 |

---

## Архитектура

Многомодульная, разбита по слоям. Каждая фича — отдельный Gradle-модуль
(`Screen` + `*Contract` (State/Event/SideEffect) + `ScreenModel` + `navigation/`),
общается с доменом через репозитории; зависимости — однонаправленные
`app → feature → core/data → core:domain`.

```
app/                        # точка входа, NavGraph, Application, MainActivity
├─ core/
│  ├─ domain/               # модели, интерфейсы репозиториев, RequestResult
│  ├─ network/              # сетевой клиент, токены
│  ├─ presentation/         # BaseScreenModel (MVI: State/Event/SideEffect)
│  ├─ designsystem/         # цвета, типографика, формы (Color/Type/Shape), тема
│  └─ ui/                   # переиспользуемые Composable (PosterImage, FilmaxTabBar, …)
├─ data/                    # реализации репозиториев + DTO + мапперы
│  ├─ auth/  catalog/  search/  user/  watching/
└─ feature/                 # экраны
   ├─ onboarding/  home/  search/  collections/
   ├─ library/  profile/  details/  player/
```

**Поток данных (MVI, однонаправленный):** `Composable` (`collectAsState` / `collectSideEffect`,
события через `dispatch(Event)`) → `ScreenModel` (`BaseScreenModel`, `State` + одноразовые `SideEffect`) →
`Repository` (возвращает `RequestResult<T>`: `Success` / `Error`) → сеть.

### Экраны (нижняя навигация)
**Главная** · **Поиск** · **Подборки** · **Библиотека** · **Профиль**,
плюс **Детали** и **Плеер** (вне таб-бара).

---

## Ключевые возможности

- **Дизайн-система** — переиспользуемые `Filmax*`-компоненты в `core:ui`
  (кнопки, бейджи, постеры, карточки, списки, поля, stat-карточки, модалки ошибок)
  с минимальным API; экраны собираются из них. Каталог компонентов — экран
  **«Дизайн-система»** (из Профиля, только в debug-сборке).
- **Избранное** — серверный watchlist (`togglewatchlist`) + локальный кэш
  (multiplatform-settings) как источник списка для Библиотеки; синхронизация на
  тоггле и импорт флага `inWatchlist` при открытии Деталей.
- **Загрузки** — метаданные скачанного локально, отображение в Библиотеке.
- **Система ошибок** — `AppError` + резолвер в `core:domain`, единая модалка
  `FilmaxErrorModal`; из `ScreenModel` вызывается `showError(...)`.
- **Детали** — коллапсирующий «sticky» hero с параллаксом и компактным app bar.
- **Плеер** — Media3 ExoPlayer, выбор качества из бэкенда, переключение субтитров
  на лету (выпадающий список), кастомные контролы.
- **Настройки воспроизведения** — качество видео и язык аудио/субтитров
  выбираются в bottom-sheet (Профиль), сохраняются локально и применяются в плеере.
- **Регистрация устройства** — `device/notify` при успешной авторизации.

---

## Что осталось сделать

- [ ] **Нестед-скролл на экране Профиля** (коллапсирующая шапка, как в Деталях).
- [ ] **Реальное офлайн-кэширование** загрузок (сейчас сохраняются только метаданные).
- [ ] Серверная синхронизация **списка** избранного (у API нет ручки на список —
      держим локально; есть только тоггл `togglewatchlist` + флаг `inWatchlist`).
- [ ] Довести оставшиеся **строки настроек** Профиля (Загрузки/Уведомления/
      Приватность) — сейчас часть статичные заглушки.
- [ ] Экран **«Списки»/закладки** (папки `bookmarks`) и управление ими.
- [ ] Полноэкранный режим и регулировка громкости в плеере (кнопки-заглушки).
- [ ] Трансляция на устройства (Cast) **не поддерживается** и убрана из UI.

---

## Сборка и запуск

Требуется JDK 17 и Android SDK 35.

```bash
# Debug APK
./gradlew :app:assembleDebug

# Установить на устройство/эмулятор
./gradlew :app:installDebug

# Скомпилировать конкретный модуль
./gradlew :feature:collections:compileDebugKotlin
```

`local.properties` (путь к Android SDK) и каталоги `build/` не коммитятся —
см. `.gitignore`.

---

## Дизайн

Дизайн всех экранов ведётся в Claude Design проекте и портируется поэкранно в
Compose. Дизайн-токены маппятся на `core:designsystem`
(`Color` / `Type` / `Shape`). Акцентный цвет — `#B4305A`.

---

## Развитие

Текущий техдолг и дорожная карта (в т.ч. планируемая миграция на **Kotlin
Multiplatform**: общий `data`/`domain` + Compose Multiplatform под iOS и Android) —
в [`docs/REFACTORING_PLAN.md`](docs/REFACTORING_PLAN.md).
