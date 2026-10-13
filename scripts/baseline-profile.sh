#!/usr/bin/env bash
# Обновляет Baseline Profile приложения: прогоняет BaselineProfileGenerator на подключённом
# ТВ-устройстве или эмуляторе (Android 13+, root не нужен) и кладёт результат в
# app/src/main/baseline-prof.txt. Профиль собран на сборке app `benchmark` с данными mock —
# пути кода те же, что у release.
#
# Запуск: ANDROID_SERIAL=emulator-5554 scripts/baseline-profile.sh
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.filmax.benchmark.BaselineProfileGenerator

# Генератор пишет и стартовые правила (includeInStartupProfile), поэтому файл один —
# *-startup-prof.txt: в нём весь профиль сценария. Берём версию без метки времени.
profile=$(find benchmark/build/outputs/connected_android_test_additional_output -name '*-prof.txt' \
  ! -name '*-prof-2*' | head -n 1)
if [ -z "$profile" ]; then
  echo "Профиль не найден в benchmark/build/outputs" >&2
  exit 1
fi
cp "$profile" app/src/main/baseline-prof.txt
echo "Обновлён app/src/main/baseline-prof.txt ($(wc -l < app/src/main/baseline-prof.txt) правил)"
