# Filmax TV (Android TV)

Отдельный TV-таргет с 10-foot UI и D-pad навигацией. Переиспользует весь не-UI слой
мобильного приложения (`core:*`, `data:*`, и MVI-`ScreenModel`-и из `feature:*`) — меняется
только presentation-слой.

Дизайн-источник: [`docs/design/filmax-tv-all-screens.html`](design/filmax-tv-all-screens.html)
(борд «Filmax TV — Все экраны»: Главная, Поиск, Жанры, Библиотека, Профиль, Детали, Плеер).

## Модули

| Модуль | Назначение |
|--------|------------|
| `:app-tv` | Приложение (`LEANBACK_LAUNCHER`, баннер), `TvMainActivity`, DI, навигация + верхний таб-бар |
| `:core:tv-designsystem` | `FilmaxTvTheme` (tv-material3 + compose-material3), focus `#FFD466`, `TvButton`, `TvFocusCard` |
| `:feature-tv:onboarding` | Вход по device-code поверх `OnboardingScreenModel` |
| `:feature-tv:home` | Главная (hero + рельсы) поверх `HomeScreenModel` |
| `:feature-tv:search` | Поиск с экранной клавиатурой поверх `SearchScreenModel` |
| `:feature-tv:categories` | Жанры (статичная сетка плиток) |
| `:feature-tv:library` | Библиотека (табы + сетка) поверх `LibraryScreenModel` |
| `:feature-tv:profile` | Профиль/настройки поверх `ProfileScreenModel` |
| `:feature-tv:details` | Детали поверх `DetailsScreenModel` (тот же `DetailsRoute`) |
| `:feature-tv:player` | Плеер (ExoPlayer) поверх `PlayerScreenModel` (тот же `PlayerRoute`) |

## Принципы

- **Нативный TV-стек.** `androidx.tv.material3`: `FilmaxTvTheme` оборачивает контент в
  tv-material3 + compose-material3 темы; интерактив (`TvButton`, `TvFocusCard`) — на
  tv-material3 `Button`/`Surface` с фокусом/масштабом/обводкой из коробки. Вся работа с
  tv-material3 API сосредоточена в `:core:tv-designsystem` (2 файла) — экраны её не касаются.
- **Дизайн-система общая.** TV-токены совпали с `core:designsystem`; отличие — затемнённые
  поверхности (`surface #0A0809`) и focus-цвет.
- **Логика не дублируется.** TV-экраны берут готовые `ScreenModel`/`Contract`/koin-модули
  из мобильных фич. Детали/Плеер переиспользуют и сами маршруты (`DetailsRoute`/`PlayerRoute`),
  чтобы `SavedStateHandle` отдавал `itemId`.

## Статус

- [x] Каркас `:app-tv` + тема + навигация + верхний таб-бар
- [x] Onboarding, Главная, Поиск, Жанры, Библиотека, Профиль, Детали, Плеер
- [ ] `Жанры` — статический список (нужен эндпоинт жанров каталога)
- [ ] `Профиль` — реальный аккаунт/настройки вместо мульти-профиля из макета (в приложении профилей нет)
- [ ] Сборка не проверялась в этом окружении (нет Android SDK) — собрать на машине с SDK

## CI → Telegram

`.github/workflows/android-build.yml` собирает debug-APK (`:app-tv` по умолчанию) на
каждый пуш в `claude/android-tv`/`master` и по кнопке (Actions → Run workflow) и присылает
файл в Telegram. APK также кладётся в Artifacts (запасной путь, и обход лимита бота 50 МБ).

Нужно один раз добавить два секрета (Settings → Secrets and variables → Actions):
- `TELEGRAM_BOT_TOKEN` — токен бота от @BotFather;
- `TELEGRAM_CHAT_ID` — id чата (свой id можно узнать у @userinfobot).

Без секретов сборка всё равно идёт, а шаг отправки просто пропускается с предупреждением.

## Версии

- `androidx.tv:tv-material` = `1.0.0` (см. `gradle/libs.versions.toml`). Если IDE/сборка
  ругается на несовместимость с Compose BOM `2025.05.01` — поднять версию tv-material.
