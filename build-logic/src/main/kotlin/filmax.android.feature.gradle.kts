// UI-модуль фичи (feature:*:mobile / feature:*:tv) — Compose-библиотека. Раньше этот файл
// повторял filmax.android.compose строка в строку; две копии одной настройки расходятся при
// первой же правке, поэтому фича просто применяет compose-конвенцию.
plugins {
    id("filmax.android.compose")
}
