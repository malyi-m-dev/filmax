# Filmax iOS

Первый вертикальный срез iOS-приложения: **точка входа + онбординг с авторизацией**.

## Архитектура
- **Общее (KMP, `:shared`):** data + domain — репозитории, use-case'ы, модели, сеть, хранилище токенов (`multiplatform-settings`). Экспортируется как статический фреймворк `Shared`; **SKIE** даёт дружелюбный Swift-интероп (Flow → AsyncSequence, sealed → enum, suspend → async).
- **Презентейшен целиком на Swift:** ViewModel'и (`ObservableObject`) + SwiftUI-вью. Логика онбординга (device-flow, поллинг) переписана на Swift поверх общих `RequestDeviceCodeUseCase`/`PollForTokenUseCase`.

Файлы:
- `iosApp/iOSApp.swift` — `@main`, инициализация Koin (`KoinKt.doInitKoin()`).
- `iosApp/RootView.swift` — сессия-гейт: слушает `ObserveAuthStateUseCase` (Flow) → онбординг или главный.
- `iosApp/Onboarding/` — `OnboardingViewModel` (device-code + поллинг) + `OnboardingView` (шаги: приветствие → фичи → код активации).
- `iosApp/Main/MainPlaceholderView.swift` — заглушка авторизованного состояния (+ выход через `LogoutUseCase`).

## Как собрать и запустить (только macOS + Xcode)
1. Установить генератор проекта: `brew install xcodegen`.
2. В папке `iosApp/`: `xcodegen generate` → появится `Filmax.xcodeproj`.
3. Открыть `Filmax.xcodeproj`, выбрать симулятор iPhone/iPad, **Run**.
   - Перед сборкой app автоматически прогонит `:shared:embedAndSignAppleFrameworkForXcode` (см. preBuildScript в `project.yml`) — соберёт KMP-фреймворк.
   - Для запуска на устройстве проставить `DEVELOPMENT_TEAM` в `project.yml`.

## Что проверить (DoD #9)
- Приложение запускается; онбординг проходится (приветствие → фичи → код активации).
- Авторизация: код появляется, ввод на `kino.pub/device` под аккаунтом KinoPub переводит на главный экран.
- Токен сохраняется (перезапуск приложения → сразу главный экран, минуя онбординг).

## Заметки по интеропу (собиралось только код-ревью, без Mac — возможны мелкие правки)
- Считается, что SKIE отдаёт `Flow<Boolean>` как `Bool` (в `RootView`). Если это `KotlinBoolean` — заменить `authenticated` на `authenticated.boolValue`.
- `RequestResult` (sealed) разбирается через SKIE `onEnum(of:)` (`.success`/`.error`). Если имена кейсов иные — поправить `switch`.
- suspend-функции вызываются как `async` (`try await useCase.invoke()`). Если SKIE-конфигурация иная — обернуть в completion-handler.
- Имя Koin-инициализатора из Swift: `KoinKt.doInitKoin()` (Kotlin top-level `doInitKoin` в файле `Koin.kt`).
