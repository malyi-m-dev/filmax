package com.filmax.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/** Пакет сборки app `benchmark`: release-код на фейковых данных mock. */
const val TARGET_PACKAGE = "com.filmax.app.bench"

private const val MAIN_ACTIVITY = "com.filmax.app.MainActivity"
private const val MOCK_PACKAGE = "com.filmax.app.mock"

/** Сколько ждать появления экрана: холодный старт release-сборки на эмуляторе — секунды. */
private const val SCREEN_TIMEOUT_MS = 10_000L

/**
 * Пауза между нажатиями: пульт жмут не чаще, чем успевает доиграть фокус (160 мс) и прокрутка
 * к опорной точке (280 мс). Чаще — и замер мерил бы очередь событий, а не плавность.
 */
private const val KEY_PAUSE_MS = 350L

/** Подписи, по которым сценарий понимает, что экран открылся (фикстуры :data:fake). */
object Landmarks {
    const val HOME = "Продолжить просмотр"
    const val SERIES_ROW = "Сериалы с высоким рейтингом"
    const val EPISODES = "Эпизоды"
}

/**
 * Холодный старт главной активности и ожидание ленты. Явный компонент, а не launch intent
 * пакета: на ТВ у приложения две категории лаунчера, и выбор между ними не должен решать за нас.
 */
fun MacrobenchmarkScope.openHome() {
    // Mock-сборка рисует тот же интерфейс: если она осталась на экране, сценарий нашёл бы
    // «Продолжить просмотр» у неё, а замер снял бы кадры чужого процесса.
    device.executeShellCommand("am force-stop $MOCK_PACKAGE")
    // Чистая задача: каждый прогон начинается с ленты в начале, а не там, где закончил прошлый.
    val intent = Intent(Intent.ACTION_MAIN)
        .setComponent(ComponentName(TARGET_PACKAGE, MAIN_ACTIVITY))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivityAndWait(intent)
    waitFor(Landmarks.HOME)
}

fun MacrobenchmarkScope.waitFor(text: String) {
    check(device.wait(Until.hasObject(By.textContains(text)), SCREEN_TIMEOUT_MS)) { "не дождались «$text»" }
}

/** Нажатия пульта с паузой — как их жмёт человек. */
fun MacrobenchmarkScope.press(key: Remote, times: Int = 1) {
    repeat(times) {
        when (key) {
            Remote.UP -> device.pressDPadUp()
            Remote.DOWN -> device.pressDPadDown()
            Remote.LEFT -> device.pressDPadLeft()
            Remote.RIGHT -> device.pressDPadRight()
            Remote.OK -> device.pressDPadCenter()
            Remote.BACK -> device.pressBack()
        }
        Thread.sleep(KEY_PAUSE_MS)
    }
}

enum class Remote { UP, DOWN, LEFT, RIGHT, OK, BACK }

/** Лента Главной: вниз по рядам до конца, вбок по ряду и обратно наверх к hero. */
fun MacrobenchmarkScope.scrollHomeFeed() {
    press(Remote.DOWN, times = HOME_ROWS)
    press(Remote.RIGHT, times = RAIL_STEPS)
    press(Remote.LEFT, times = RAIL_STEPS)
    press(Remote.UP, times = HOME_ROWS)
}

/**
 * Из ленты — в детали сериала на семь сезонов («Хроники Пограничья» открывает ряд «Сериалы с
 * высоким рейтингом») и прокрутка списка серий вниз и обратно.
 */
fun MacrobenchmarkScope.openSeriesAndScrollEpisodes() {
    press(Remote.DOWN, times = SERIES_ROW_INDEX)
    waitFor(Landmarks.SERIES_ROW)
    press(Remote.OK)
    waitFor(Landmarks.EPISODES)
    press(Remote.DOWN, times = EPISODE_STEPS)
    press(Remote.UP, times = EPISODE_STEPS)
}

/** Боковое меню: раскрыть, пройти по разделам и вернуться в ленту. */
fun MacrobenchmarkScope.walkSideMenu() {
    press(Remote.LEFT)
    press(Remote.DOWN, times = MENU_ITEMS)
    press(Remote.UP, times = MENU_ITEMS)
    press(Remote.RIGHT)
}

/** Сколько рядов в ленте фикстур: продолжить, тренды, подборки, сериалы, новинки. */
private const val HOME_ROWS = 6
private const val RAIL_STEPS = 6

/** Ряд «Сериалы с высоким рейтингом» — четвёртый от hero. */
private const val SERIES_ROW_INDEX = 4
private const val EPISODE_STEPS = 12
private const val MENU_ITEMS = 3
