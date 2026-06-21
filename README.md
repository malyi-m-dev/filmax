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
| DI | Hilt 2.52 |
| Сеть | Retrofit 2.11 + `kotlinx.serialization` |
| Асинхронность | Coroutines 1.9 + Flow |
| Сборка | AGP 8.7.3, Gradle (convention plugins в `build-logic`), KSP |
| SDK | minSdk 26, target/compileSdk 35 |

---

## Архитектура

Многомодульная, разбита по слоям. Каждая фича — отдельный Gradle-модуль
(`Screen` + `UiState` + `ViewModel` + `navigation/`), общается с доменом через
репозитории; зависимости — однонаправленные `app → feature → core/data → core:domain`.

```
app/                        # точка входа, NavGraph, Application, MainActivity
├─ core/
│  ├─ domain/               # модели, интерфейсы репозиториев, RequestResult
│  ├─ network/              # сетевой клиент, токены
│  ├─ designsystem/         # цвета, типографика, формы (Color/Type/Shape), тема
│  └─ ui/                   # переиспользуемые Composable (PosterImage, FilmaxTabBar, …)
├─ data/                    # реализации репозиториев + DTO + мапперы
│  ├─ auth/  catalog/  search/  user/  watching/
└─ feature/                 # экраны
   ├─ onboarding/  home/  search/  collections/
   ├─ library/  profile/  details/  player/
```

**Поток данных:** `Composable` → `ViewModel` (`StateFlow<UiState>`) →
`Repository` (возвращает `RequestResult<T>`: `Success` / `Error`) → сеть.

### Экраны (нижняя навигация)
**Главная** · **Поиск** · **Подборки** · **Библиотека** · **Профиль**,
плюс **Детали** и **Плеер** (вне таб-бара).

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
