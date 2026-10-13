# Производительность ТВ-интерфейса

ТВ-приставки упираются не в код, а в GPU: трасса Главной на эмуляторе показала ~0.4 мс на запись
кадра и ~60 мс на его растеризацию. Поэтому главное правило — не заставлять экран перерисовываться,
когда ничего не происходит, и не закрашивать один пиксель много раз.

## Правила для эффектов

- **Бесконечные анимации — только пока их видно.** Ken-burns hero идёт, пока hero в фокусе;
  под панелью (`TvSheetHost`) все такие анимации на паузе (`LocalFxPaused`).
- **Анимируемое значение читается при отрисовке**, а не в композиции: `graphicsLayer { … }`,
  `drawWithCache { onDraw { state.value } }`. Иначе элемент перекомпонуется 60 раз в секунду.
- **Дорогое — один раз в текстуру.** Свечение фокуса размывается один раз на размер карточки
  (`FxGlow.kt`), неподвижные затемнения hero и фона деталей — offscreen-слоем
  (`Modifier.fxStaticOverlay`).
- **Фон — у окна.** `FilmaxTvTheme` красит окно цветом палитры; экраны свою сплошную заливку не
  рисуют.
- **Облегчённый режим** (`LocalLightEffects`) — только на устройствах с `isLowRamDevice`: без
  ken-burns, пульса и размытия. Остальные получают полный вид.

## Замеры

Модуль `:benchmark` (Macrobenchmark) гоняет сценарии пульта на сборке app `benchmark` — release с
R8 на фейковых данных mock, поэтому вход и сеть не нужны и каждый прогон видит ту же ленту.

```bash
# время кадра P50/P90/P99 и перерасход кадра: лента Главной, серии сериала, боковое меню, старт
ANDROID_SERIAL=emulator-5554 ./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.filmax.benchmark.TvFrameBenchmarks \
  -Pandroid.testInstrumentationRunnerArguments.compilation=none   # или profile
py scripts/bench-summary.py <после>/com.filmax.benchmark-benchmarkData.json <до>/…json
```

Эмулятор рисует графику на процессоре хоста: при параллельных сборках числа скачут в разы.
Сравнивать «до» и «после» — только на свободной машине и в одном режиме компиляции.

## Baseline Profile

`scripts/baseline-profile.sh` прогоняет `BaselineProfileGenerator` (Android 13+, root не нужен) и
обновляет `app/src/main/baseline-prof.txt`. В APK профиль ставит ProfileInstaller — Google Play
для этого не нужен. Обновлять после заметных изменений экранов.

## Рывки в рабочем приложении

`JankMonitor` (JankStats) пишет сводку по экранам в logcat:

```bash
adb logcat -s FilmaxJank
# TvHomeRoute: кадров 600, рывков 12 (2.0%), в среднем 9.4 мс, худший 48.1 мс
```

Так смотрят, как интерфейс ведёт себя на настоящем телевизоре, а не на эмуляторе.
