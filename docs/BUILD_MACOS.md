# Сборка Filmax на macOS (iOS + Android)

Инструкция для разработчика на Mac: как собрать и запустить iOS-приложение (SwiftUI + KMP `Shared`),
а также Android-часть и статические гейты. Kotlin/Native для Apple собирается **только на macOS**.

---

## 1. Предустановки

| Что | Зачем | Установка |
|---|---|---|
| **Xcode** (посл. версия) + iOS SDK + симуляторы | сборка iOS | App Store |
| **Xcode Command Line Tools** | тулчейн | `xcode-select --install` |
| **JDK 17** | проект на `JVM_17` | `brew install --cask temurin@17` (или JDK из Android Studio) |
| **Android SDK** | ⚠️ нужен даже для iOS (модуль `:shared` имеет `androidTarget`, Gradle требует SDK при конфигурации) | Android Studio → SDK Manager, либо `cmdline-tools` |
| **Homebrew** | для xcodegen | https://brew.sh |
| **XcodeGen** | генерация `.xcodeproj` из `project.yml` | `brew install xcodegen` |

CocoaPods **не нужен** — фреймворк `Shared` собирается напрямую задачей Gradle (не через Pods).

Проверь версии:
```bash
java -version        # → 17.x
xcodegen --version
xcodebuild -version
```

---

## 2. Первичная настройка

```bash
git clone git@github.com:malyi-m-dev/filmax.git
cd filmax
```

**`local.properties`** в корне (gitignored) — путь к Android SDK:
```
sdk.dir=/Users/<ты>/Library/Android/sdk
```
(или экспортируй `ANDROID_HOME`). Без этого даже iOS-таск не сконфигурируется, т.к. `:shared`
собирает и Android-таргет.

**JDK 17** должен быть активным: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`.

Первый прогон Gradle скачает Kotlin/Native-тулчейн (нужен интернет; несколько минут).

---

## 3. Сборка и запуск iOS (симулятор)

```bash
cd iosApp
xcodegen generate          # → Filmax.xcodeproj
open Filmax.xcodeproj
```
В Xcode: выбери симулятор iPhone → **Run (⌘R)**.
Перед сборкой app автоматически прогонит preBuildScript
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` — соберёт `Shared.framework` под нужную
платформу/архитектуру (см. `iosApp/project.yml`).

**Запуск на устройстве:** проставь свой `DEVELOPMENT_TEAM` в `iosApp/project.yml`, перегенерируй
(`xcodegen generate`), выбери устройство.

### Полезно: собрать фреймворк отдельно (быстрее ловить Kotlin-ошибки)
```bash
# Apple Silicon (симулятор):
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
# реальное устройство:
./gradlew :shared:linkDebugFrameworkIosArm64
# Intel Mac (симулятор):
./gradlew :shared:linkDebugFrameworkIosX64
```
Если эти таски зелёные — общий Kotlin-код компилируется под iOS; дальше проблема (если есть) уже в Swift.

### Сборка iOS из CLI (опц., для CI)
```bash
xcodebuild -project iosApp/Filmax.xcodeproj -scheme Filmax \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build
```

---

## 4. Android на Mac

```bash
./gradlew :app:assembleDebug
# установка на запущенный эмулятор/устройство:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Запуск: `com.filmax.app/.MainActivity`. TV-UI проверять на AVD `Television_1080p` (leanback);
mobile — на телефонном AVD. Эмуляторы — из Android Studio (Device Manager) или `emulator -avd <name>`.

---

## 5. Статические гейты (как в CI)

```bash
./gradlew detekt        # обычный анализ + ktlint (baseline проекта = 0)
./gradlew detektMain    # type-resolution (ловит !!/NoNameShadowing/UseOrEmpty)
```

---

## 6. Траблшутинг

- **`SDK location not found`** → нет `local.properties`/`ANDROID_HOME`. Нужен даже для iOS (см. §2), т.к. `:shared` собирает `androidTarget`.
- **Ошибки версии JDK / `Unsupported class file major version`** → активируй JDK 17 (`JAVA_HOME`).
- **`framework not found Shared`** в Xcode → прогони фреймворк-таск вручную (§3), проверь `FRAMEWORK_SEARCH_PATHS` в `project.yml` и перегенерируй проект (`xcodegen generate`).
- **Не та архитектура симулятора** → Apple Silicon: `iosSimulatorArm64`; Intel Mac: `iosX64`. preBuildScript выбирает по `$PLATFORM_NAME`/`$ARCHS` автоматически.
- **Долгая первая сборка iOS** — Kotlin/Native компилирует фреймворк с нуля (норм); последующие — инкрементально.
- **Подпись/`No signing team`** при запуске на устройстве → задай `DEVELOPMENT_TEAM` в `project.yml`.
- **SKIE-интероп не совпал** (типы `Bool`/`KotlinBoolean`, кейсы `onEnum`) → точечные правки в Swift, см. заметки в `iosApp/README.md` и [`docs/ios-shared-layer.md`](ios-shared-layer.md).

---

## 7. Что где

- iOS-приложение и сборка — [`iosApp/README.md`](../iosApp/README.md).
- Общий data/domain-слой для Swift (API, интероп, рецепты) — [`docs/ios-shared-layer.md`](ios-shared-layer.md).
- Общая логика (data/domain) — `:core:*`, `:data:*`, экспорт для iOS — `:shared`.
