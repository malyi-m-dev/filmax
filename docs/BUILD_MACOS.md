# Сборка Filmax на macOS (iOS + Android)

Инструкция для разработчика на Mac: как собрать и запустить iOS-приложение (SwiftUI + KMP `Shared`),
а также Android-часть и статические гейты. Kotlin/Native для Apple собирается **только на macOS**.

---

## 0. Быстрый старт (по шагам)

Уже стоят Xcode + Android Studio (или отдельный JDK 17+) + Homebrew? Тогда весь путь от нуля до
запущенного iOS-приложения — шесть шагов:

```bash
# 1. Генератор Xcode-проекта (один раз)
brew install xcodegen

# 2. Активный JDK 17+. Можно взять встроенный JBR из Android Studio (см. §1):
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# 3. Путь к Android SDK — нужен ДАЖЕ для iOS (модуль :shared собирает и androidTarget).
#    Создать local.properties в корне репозитория:
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# 4. Сгенерировать проект из project.yml
cd iosApp && xcodegen generate && cd ..

# 5. Открыть в Xcode → выбрать схему Filmax + симулятор iPhone → Run (⌘R)
open iosApp/Filmax.xcodeproj
```

**Шаг 6 — альтернатива Xcode: собрать целиком из CLI** (удобно для проверки/CI):
```bash
xcodebuild -project iosApp/Filmax.xcodeproj -scheme Filmax \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16' build
```

Что происходит под капотом: перед сборкой app Xcode сам вызывает Gradle-таску, которая собирает
`Shared.framework` из общего Kotlin-кода. **Первая сборка долгая** (качается Kotlin/Native-тулчейн,
фреймворк компилируется с нуля — несколько минут), последующие — инкрементально.

Схемы две: **`Filmax`** (iPhone/iPad) и **`Filmax-tvOS`** (Apple TV). Детали, отдельная сборка
фреймворка и траблшутинг — в разделах ниже.

---

## 1. Предустановки

| Что | Зачем | Установка |
|---|---|---|
| **Xcode** (посл. версия) + iOS SDK + симуляторы | сборка iOS | App Store |
| **Xcode Command Line Tools** | тулчейн | `xcode-select --install` |
| **JDK 17+** | проект таргетит `JVM_17` через toolchain; сам Gradle стартует и на 17, и на 21 | `brew install --cask temurin@17`, **либо** встроенный JBR из Android Studio: `/Applications/Android Studio.app/Contents/jbr/Contents/Home` (в него уже входит JDK 21 — проверено, собирается) |
| **Android SDK** | ⚠️ нужен даже для iOS (модуль `:shared` имеет `androidTarget`, Gradle требует SDK при конфигурации) | Android Studio → SDK Manager, либо `cmdline-tools` |
| **Homebrew** | для xcodegen | https://brew.sh |
| **XcodeGen** | генерация `.xcodeproj` из `project.yml` | `brew install xcodegen` |

CocoaPods **не нужен** — фреймворк `Shared` собирается напрямую задачей Gradle (не через Pods).

Проверь версии:
```bash
java -version        # → 17.x или новее
xcodegen --version
xcodebuild -version
```
Нет системного `java`, но есть Android Studio? Активируй её JBR:
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

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

**JDK 17+** должен быть активным: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`
(или укажи JBR из Android Studio, см. §1). При запуске из Xcode preBuildScript наследует `JAVA_HOME`
из окружения — если запускаешь `xcodebuild`/Xcode из терминала, экспортируй переменную там же.

Первый прогон Gradle скачает Kotlin/Native-тулчейн (нужен интернет; несколько минут).

---

## 3. Сборка и запуск iOS / tvOS (симулятор)

```bash
cd iosApp
xcodegen generate          # → Filmax.xcodeproj (две схемы)
open Filmax.xcodeproj
```
В Xcode выбери схему и симулятор → **Run (⌘R)**:
- **`Filmax`** → симулятор iPhone/iPad;
- **`Filmax-tvOS`** → симулятор Apple TV.

Перед сборкой app автоматически прогонит preBuildScript
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` — соберёт `Shared.framework` под нужную
платформу/архитектуру (iphoneos/appletvos/…, см. `iosApp/project.yml`).

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

### XCFramework (мульти-архитектурный бандл — для дистрибуции/CI)
Собрать `Shared.xcframework` (все iOS-архитектуры device + simulator в одном бандле):
```bash
./gradlew :shared:assembleSharedReleaseXCFramework   # release-бандл → shared/build/XCFrameworks/release/
./gradlew :shared:assembleSharedXCFramework          # debug + release
# собрать И положить готовый бандл в iosApp/Frameworks/Shared.xcframework:
./gradlew :shared:syncSharedXCFramework
```
Модель сборки iOS-приложения по умолчанию — per-build `embedAndSignAppleFrameworkForXcode` (Xcode сам
собирает фреймворк нужной архитектуры при каждой сборке, см. `project.yml`). XCFramework — альтернатива:
предсобранный бандл, который линкуется напрямую (быстрее сборка app, удобно для CI/раздачи); при изменениях
Kotlin его нужно пересобрать (`syncSharedXCFramework`).

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
- **Ошибки версии JDK / `Unsupported class file major version`** → активируй JDK 17+ (`JAVA_HOME`).
- **`permission denied: ./gradlew`** (в т.ч. падение preBuildScript в Xcode с ненулевым кодом) → у `gradlew` слетел бит исполнения. Верни: `chmod +x gradlew`. В репозитории он уже помечен исполняемым, но проблема может всплыть после ручного копирования/патча файла.
- **`Please run the embedAndSignAppleFrameworkForXcode task from Xcode`** при ручном запуске таски → это нормально: таске нужны переменные окружения Xcode (`SDK_NAME`, `ARCHS`, …). Запускай её из Xcode/`xcodebuild`, а для проверки Kotlin — отдельные `linkDebugFramework…`-таски (§3).
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
