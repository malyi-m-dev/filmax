# Filmax iOS + tvOS

Первый вертикальный срез Apple-приложений: **точка входа + онбординг с авторизацией** — на iPhone/iPad **и** Apple TV.

> 📖 **Справочник по общему data/domain-слою для Swift** — [`docs/ios-shared-layer.md`](../docs/ios-shared-layer.md): все use-case'ы, репозитории, модели, интероп SKIE, обработка ошибок и рецепты.

## Архитектура
- **Общее (KMP, `:shared`):** data + domain — репозитории, use-case'ы, модели, сеть, хранилище токенов (`multiplatform-settings`). Экспортируется как статический фреймворк `Shared` (таргеты iOS **и** tvOS); **SKIE** даёт дружелюбный Swift-интероп (Flow → AsyncSequence, sealed → enum, suspend → async).
- **Презентейшен на Swift, раздельно iOS/tvOS:** общие ViewModel'и (`ObservableObject`) переиспользуются обоими; различаются только SwiftUI-View (тач-навигация на iOS vs focus engine/пульт на tvOS). На Apple разделение TV/mobile — это **отдельные таргеты**, а не runtime-флаг (в отличие от Android).

Структура (`iosApp/`):
```
Shared/    ← общий Swift для ОБОИХ таргетов
  Theme.swift · SessionViewModel · Onboarding/OnboardingViewModel · Main/MainViewModel
iOS/       ← таргет Filmax (iPhone/iPad)
  iOSApp.swift (@main) · RootView · Onboarding/OnboardingView · Main/MainPlaceholderView
tvOS/      ← таргет Filmax-tvOS (Apple TV)
  tvOSApp.swift (@main) · TvRootView · Onboarding/TvOnboardingView · Main/TvMainPlaceholderView
```
ViewModel'и (`Shared/`) вызывают общие use-case'ы KMP и одинаковы на обеих платформах; `Koin` инициализируется в точке входа каждого таргета.

## Как собрать и запустить (только macOS + Xcode)
> 🛠 **Полная инструкция по сборке на Mac (предустановки, iOS + Android, траблшутинг)** — [`docs/BUILD_MACOS.md`](../docs/BUILD_MACOS.md). Ниже — краткий путь.

1. Установить генератор проекта: `brew install xcodegen`.
2. В папке `iosApp/`: `xcodegen generate` → появится `Filmax.xcodeproj` (с двумя схемами).
3. Открыть `Filmax.xcodeproj`, выбрать схему и симулятор, **Run**:
   - **`Filmax`** → симулятор iPhone/iPad;
   - **`Filmax-tvOS`** → симулятор Apple TV.
   - Перед сборкой app автоматически прогонит `:shared:embedAndSignAppleFrameworkForXcode` (см. preBuildScript в `project.yml`) — соберёт KMP-фреймворк под нужную платформу.
   - Для запуска на устройстве проставить `DEVELOPMENT_TEAM` в `project.yml`.

## Что проверить (DoD #9)
- Приложение запускается; онбординг проходится (приветствие → фичи → код активации).
- Авторизация: код появляется, ввод на `kino.pub/device` под аккаунтом KinoPub переводит на главный экран.
- Токен сохраняется (перезапуск приложения → сразу главный экран, минуя онбординг).

## Заметки по интеропу (собиралось только код-ревью, без Mac — возможны мелкие правки)
- Считается, что SKIE отдаёт `Flow<Boolean>` как `Bool` (в `Shared/SessionViewModel`). Если это `KotlinBoolean` — заменить `authenticated` на `authenticated.boolValue`.
- `RequestResult` (sealed) разбирается через SKIE `onEnum(of:)` (`.success`/`.error`). Если имена кейсов иные — поправить `switch`.
- suspend-функции вызываются как `async` (`try await useCase.invoke()`). Если SKIE-конфигурация иная — обернуть в completion-handler.
- Имя Koin-инициализатора из Swift: `KoinKt.doInitKoin()` (Kotlin top-level `doInitKoin` в файле `Koin.kt`).
