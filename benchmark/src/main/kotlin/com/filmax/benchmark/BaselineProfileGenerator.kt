package com.filmax.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Генератор Baseline Profile: проходит основные сценарии пульта, и ART заранее компилирует код,
 * который они задевают, — лента, детали сериала, меню. Результат кладётся в
 * `app/src/main/baseline-prof.txt` (скрипт `scripts/baseline-profile.sh`); профиль ставит в APK
 * ProfileInstaller, Google Play для этого не нужен.
 *
 * Генерация требует root на устройстве — подходит эмулятор с образом без Google Play.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
        openHome()
        scrollHomeFeed()
        walkSideMenu()
        // Детали — с чистой ленты: после меню фокус возвращается туда, откуда в него ушли, и
        // счёт рядов до «Сериалов с высоким рейтингом» сбился бы.
        openHome()
        openSeriesAndScrollEpisodes()
        press(Remote.BACK)
        waitFor(Landmarks.SERIES_ROW)
    }
}
