# design-sync: заметки по Filmax TV

Проект Claude Design «Filmax TV» (`projectId` в `config.json`) — ТВ-дизайн-система Filmax.

## Как устроено

- Дизайн-система приложения написана на Compose (`core:tv-designsystem` + ТВ-блоки фич). Конвертер
  умеет только React, поэтому в `design/tv-ui` лежит React-зеркало: те же токены (`src/tokens.css`
  ← `TvColor.kt`, `TvType.kt`, `TvMetrics.kt`) и компоненты с той же вёрсткой. Это осознанная
  реимплементация, не скомпилированный код приложения.
- **Источник правды — Kotlin.** Поменяли токен, размер или вид компонента в Compose — повторите
  в `design/tv-ui` и запустите синхронизацию. Иначе дизайн тихо разойдётся с приложением.
- Сборка зеркала: `cd design/tv-ui && npm ci && npm run build` (esbuild + tsc, `build.mjs`).
  Токены встраиваются в `dist/styles.css`, шрифт — отдельным `dist/fonts.css` (`extraFonts`).
- Шрифт — Roboto из Android SDK (`layoutlib/data/fonts/Roboto-Regular.ttf`, вариативный), ужатый
  `fontTools.subset` до латиницы и кириллицы в `design/tv-ui/fonts/Roboto-Variable.woff2`.
  Google Fonts не используем: из РФ грузится нестабильно, и захват превью висел на сети.
- Группы карточек задаются заглушками `.design-sync/groups/<Группа>.md` через `docsMap`.

## Запуск

- Конвертер — с корня репо, `--entry design/tv-ui/dist/index.js --node-modules design/tv-ui/node_modules`.
  Шелл не должен стоять внутри `ds-bundle/`: Windows не даст его удалить (`EPERM rm`).
- Рендер-проверка и захват — только родным браузером Playwright
  (`cd .ds-sync && npx playwright install chromium-headless-shell`). С системным Chrome через
  `DS_CHROMIUM_PATH` первая навигация каждого прогона висит до таймаута.
- **Скриншоты экранов** (`design/tv-ui/screens/*.jpg`) конвертер не копирует — `guidelinesGlob`
  берёт только `.md` (описание `design/tv-ui/tv-screens.md` уходит в `guidelines/tv-screens.md`).
  После каждой сборки и до загрузки скопировать вручную, иначе сверка удалений снесёт их из проекта:
  `mkdir -p ds-bundle/guidelines/screens && cp design/tv-ui/screens/*.jpg ds-bundle/guidelines/screens/`
- Снимки делаются с mock-сборки на AVD `Television_1080p` и ужимаются до 960×540.

## Известные предупреждения рендера

- Нет. Широким карточкам (экран, hero, ряды) задан `cardMode: column`; холсты 960px в превью
  уменьшены `zoom: 0.68` — в дизайне компоненты в натуральную величину.

## Риски повторной синхронизации

- Зеркало дублирует Compose вручную: любой рефактор ТВ-компонентов в Kotlin без правки
  `design/tv-ui` — тихий рассинхрон. Сверять с `guidelines/screens` и свежими снимками mock.
- Картинки превью и примеров — picsum.photos по seed `filmax-<id>`; без сети карточки пустые.
- Снимки экранов устаревают при каждом редизайне — переснимать вместе с синхронизацией.
- Телефон (`core:ui`) не синхронизирован — только ТВ.
