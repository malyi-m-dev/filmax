# Filmax TV (Android TV)

Отдельный TV-таргет с 10-foot UI и D-pad навигацией. Переиспользует весь не-UI слой
мобильного приложения (`core:*`, `data:*`, и MVI-`ScreenModel`-и из `feature:*`) — меняется
только presentation-слой.

Дизайн-источник: [`docs/design/filmax-tv-all-screens.html`](design/filmax-tv-all-screens.html)
(борд «Filmax TV — Все экраны»: Главная, Поиск, Жанры, Библиотека, Профиль, Детали, Плеер).

## Модули

| Модуль | Назначение |
|--------|------------|
| `:app-tv` | Приложение (`LEANBACK_LAUNCHER`, баннер), `TvMainActivity`, DI, навигация |
| `:core:tv-designsystem` | `FilmaxTvTheme` (поверх токенов `core:designsystem`), focus-цвет `#FFD466`, `Modifier.tvFocusable`, `TvButton` |
| `:feature-tv:onboarding` | TV-экран входа поверх `OnboardingScreenModel` |
| `:feature-tv:home` | TV-Главная поверх `HomeScreenModel` |

## Принципы

- **Дизайн-система общая.** TV-токены из макета совпали с `core:designsystem`
  (`primary #FFB1C8`, `primaryContainer #B4305A`, `onSurface #EFDFE3`). `FilmaxTvTheme`
  лишь затемняет поверхности (`surface #0A0809`) под просмотр с дивана и добавляет
  focus-цвет.
- **Логика не дублируется.** TV-экраны берут готовые `ScreenModel`/`Contract`/koin-модули
  из мобильных фич (`feature:onboarding`, `feature:home`).
- **Фокус — штатный Compose.** `Modifier.tvFocusable` даёт фокусируемость + клик (OK/DPAD)
  + подсветку (scale 1.08 + жёлтая обводка). D-pad навигация — спатиальный поиск Compose.

## Статус

- [x] Каркас `:app-tv` + тема + навигация (Splash → Onboarding → Home)
- [x] Onboarding (вход по device-code)
- [x] Главная (hero + рельсы)
- [ ] Детали, Плеер, Поиск, Жанры, Библиотека, Профиль
- [ ] `androidx.tv.material3` (карточки/карусели) — добавить при необходимости
- [ ] Экраны деталей/плеера: `onOpenItem` на Главной пока no-op
