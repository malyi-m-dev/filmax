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

## Заметки по интеропу (проверено на Mac, Xcode + симулятор iPhone)
- **`Flow<Boolean>` приходит как `KotlinBoolean`, не `Bool`** → в `Shared/SessionViewModel` берём `authenticated.boolValue`.
- **`RequestResult<T>` (sealed) разбирается через SKIE `onEnum(of:)`** (`.success`/`.error`) — работает. НО generic-параметр `Success<T>.data` стирается в ObjC-мосте до `Any?`, поэтому payload надо кастовать: `guard let deviceCode = success.data as? DeviceCode else { … }` (см. `Shared/Onboarding/OnboardingViewModel`). Это ограничение ObjC-моста, не SKIE; каст нужен на каждый generic-результат, пока не заведём типизированные обёртки/хелпер.
- **suspend-функции** вызываются как `async` (`try await useCase.invoke()`) — работает без обёрток.
- **Koin-инициализатор из Swift:** `KoinKt.doInitKoin(appDeclaration: { _ in })`. Дефолтный параметр Kotlin (`appDeclaration = {}`) в Swift-интероп не переносится — передаём пустую декларацию явно (в `iOS/iOSApp` и `tvOS/tvOSApp`).
