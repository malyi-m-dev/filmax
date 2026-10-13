# Filmax TV — как собирать экраны

Клиент kino.pub для Android TV. Строгий монохром: цвет на экране есть только у постеров и кадров,
акцент — чистый белый, приглушённый красный — только ошибки и деструктив. Управление — пультом,
поэтому у каждого интерактивного компонента есть проп `focused`, а на экране в фокусе ровно один
элемент.

## Обёртка

Любой экран — внутри `TvScreen`: кадр 960×540 (1080p при density 2), фон `#0A0A0A`, шрифт Roboto,
боковое меню разделов. Отдельный компонент вне экрана — внутри `TvTheme`, иначе текст возьмёт шрифт
браузера.

```jsx
const { TvScreen, TvHero, TvRail, TvPosterCard } = window.FilmaxTV;

<TvScreen section="home" initials="ДЗ">
  <div style={{ paddingTop: "var(--ftv-content-top)" }}>
    <TvHero title="Дом на краю карты" rating="7.9"
      parts={["Фэнтези · Приключения", "2022", "2 ч 11 мин"]}
      backdropUrl="https://picsum.photos/seed/filmax-1/1280/720" focused="play" />
    <div style={{ marginTop: "var(--ftv-row-gap)" }}>
      <TvRail title="В тренде">
        <TvPosterCard title="Кукловод" meta="Сериал · 2024" rating="7.5"
          posterUrl="https://picsum.photos/seed/filmax-13/380/570" />
      </TvRail>
    </div>
  </div>
</TvScreen>
```

`TvScreen`: `section` — `home | catalog | mine | profile`; `showNav={false}` для деталей и
плеера; `navExpanded` + `navFocused` — меню раскрыто, экран затемнён.

## Стилизация своей вёрстки

Пропсы компонентов несут весь их вид. Для своей раскладки между компонентами — только токены и
классы пакета, никаких произвольных цветов и размеров:

- Цвет: `var(--ftv-surface)` фон, `--ftv-surface-container` панели, `--ftv-surface-container-high`
  чипы, `--ftv-surface-container-highest` разделители, `--ftv-on-surface` текст,
  `--ftv-on-surface-variant` вторичный, `--ftv-on-surface-dim` третичный, `--ftv-accent` белый,
  `--ftv-error` ошибки.
- Раскладка: `--ftv-safe-h` 58px поле слева/справа, `--ftv-content-top` 40px сверху,
  `--ftv-card-gap` 18px между карточками, `--ftv-row-gap` 24px между рядами.
- Текст: классы `ftv-display-medium` (44, название hero), `ftv-display-small` (40),
  `ftv-headline-medium` (30, заголовок раздела), `ftv-headline-small` (26), `ftv-title-large`
  (21, заголовок ряда), `ftv-title-medium` (17), `ftv-title-small` (16), `ftv-body-large` (16,
  описание и мета), `ftv-body-medium` (14), `ftv-body-small` (13), `ftv-label-large` (14),
  `ftv-label-medium` (12, верхний регистр), и цвет `ftv-color-primary | secondary | dim | error`.
  Мельче 12px на ТВ не бывает.
- Формы: только скруглённый прямоугольник (`--ftv-radius-poster` 8, `-card` 10, `-panel` 12,
  `-button` 9, `-chip` 20) и круг.

## Фокус

Фокус — белая рамка 3px, тёмный ореол снаружи и увеличение 1.08 (широкие элементы — 1.02).
Его рисуют сами компоненты по `focused`; для своей плитки оберните её в `TvFocusCard`. Соседи
сфокусированной карточки в ряду приглушены до 55% — это поведение `TvPosterCard` и
`TvProgressCard` по умолчанию (`dimmed`).

## Где правда

- `styles.css` — токены и все классы `ftv-*`.
- `guidelines/tv-screens.md` и снимки `guidelines/screens/*.jpg` — существующие экраны
  приложения (Главная, Каталог, Моё, Профиль, детали, плеер) и из чего каждый собран. Новый экран
  начинайте со сверки с ними.
- `components/<группа>/<Имя>/<Имя>.prompt.md` — пропсы и примеры каждого компонента.
