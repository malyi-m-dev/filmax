# Filmax TV (Android TV)

Filmax — **одно приложение на двух форм-факторах**: телефон и Android TV. Аккаунт, данные
и логика общие; различается только presentation-слой (телефонный Compose ↔ 10-foot UI с
D-pad фокусом на `androidx.tv.material3`).

Дизайн-источник: [`docs/design/filmax-tv-all-screens.html`](design/filmax-tv-all-screens.html).

## Один APK, выбор UI по устройству

Сборка одна (`:app`, один `applicationId`). Манифест объявляет `LAUNCHER` и
`LEANBACK_LAUNCHER`, поэтому один и тот же APK ставится и на телефон, и на ТВ.
`MainActivity` на старте выбирает граф навигации:

```kotlin
val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
if (isTv) FilmaxTvTheme { FilmaxTvNavGraph() } else FilmaxTheme { FilmaxNavGraph() }
```

## Вложенная структура модулей (на каждую фичу)

```
:feature:home            ← логика: HomeScreenModel + контракт + DI (без UI)
:feature:home:mobile     ← телефонный UI + навбилдер
:feature:home:tv         ← TV UI + навбилдер   (оба api-зависят от :feature:home)
```

Так — для всех фич: onboarding, home, search, library, profile, details, player.
Особые случаи:
- **categories** (Жанры) — только TV: `:feature:categories:tv` (без логического parent).
- **details/player** — их ScreenModel читает маршрут через `SavedStateHandle.toRoute<…>()`,
  поэтому `DetailsRoute`/`PlayerRoute` лежат в логическом модуле
  (`:feature:{details,player}/navigation`), а навбилдеры в `:mobile`/`:tv` ссылаются на них.

Преимущество: TV-UI не тянет на classpath телефонный (и наоборот), логика не дублируется,
схема симметрична и расширяема.

| Слой | Модули |
|------|--------|
| TV дизайн-система | `:core:tv-designsystem` — `FilmaxTvTheme`, `TvButton`, `TvFocusCard`, focus `#FFD466` |
| TV-каркас | `:app` → `com.filmax.app.tv.navigation` (`FilmaxTvNavGraph`, `TvTopNavBar`) |
| Логика/UI фич | `:feature:X` + `:feature:X:{mobile,tv}` |

## Нативный TV-стек

`androidx.tv:tv-material` (1.0.0). Вся работа с tv-material3 API сосредоточена в
`:core:tv-designsystem` (FilmaxTvTheme + TvButton/TvFocusCard) — экраны её не касаются,
фокус/масштаб/обводка приходят из библиотеки из коробки. TV-токены совпали с
`core:designsystem`; отличие — затемнённые поверхности (`surface #0A0809`) и focus-цвет.

## CI → Telegram

`.github/workflows/android-build.yml` собирает `:app:assembleDebug` на пуш в
`claude/android-tv`/`master` и по кнопке, кладёт APK в Artifacts и шлёт в Telegram
(нужны секреты `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID`; без них шаг отправки
пропускается). Android SDK берётся предустановленный на ubuntu-раннере.

## Статус

- [x] Все экраны TV (onboarding, home, search, categories, library, profile, details, player)
- [x] Вложенная структура модулей на всех фичах (logic / mobile / tv)
- [x] Один `:app` с выбором навграфа по устройству; `:app-tv` удалён
- [x] CI собирает один APK
- [ ] Жанры — статический список (нужен эндпоинт жанров каталога)
- [ ] Профиль — реальный аккаунт/настройки вместо мульти-профиля из макета

## Версии

- `androidx.tv:tv-material` = `1.0.0` (`gradle/libs.versions.toml`). Если конфликтует с
  Compose BOM — поднять версию.
