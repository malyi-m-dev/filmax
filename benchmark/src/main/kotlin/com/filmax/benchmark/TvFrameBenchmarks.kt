package com.filmax.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Плавность ТВ-интерфейса под пультом: время кадра (P50/P90/P99) и перерасход бюджета кадра на
 * типичных сценариях. Каждый сценарий — в двух режимах компиляции:
 *
 * - [CompilationMode.None] — без AOT, как у пользователя сразу после установки APK без
 *   Baseline Profile: код идёт через интерпретатор и JIT, первые прокрутки рвутся;
 * - Baseline Profile — как после установки с ProfileInstaller.
 *
 * Разница между ними — вклад профиля; чтобы оценить правку анимаций, сравнивают один и тот же
 * режим до и после.
 */
@RunWith(Parameterized::class)
class TvFrameBenchmarks(private val compilation: CompilationMode) {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startup() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilation,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
    ) { openHome() }

    @Test
    fun homeFeed() = measureFrames { scrollHomeFeed() }

    @Test
    fun seriesEpisodes() = measureFrames { openSeriesAndScrollEpisodes() }

    @Test
    fun sideMenu() = measureFrames { walkSideMenu() }

    /**
     * Кадры сценария [scenario]; старт до ленты — в подготовке, он не замеряется. Режим старта
     * не задаётся: с ним библиотека убивает процесс после подготовки, и сценарий шёл бы по
     * лаунчеру. Одинаковое начало каждого прогона даёт перезапуск задачи в [openHome].
     */
    private fun measureFrames(scenario: MacrobenchmarkScope.() -> Unit) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilation,
        iterations = ITERATIONS,
        setupBlock = { openHome() },
        measureBlock = scenario,
    )

    companion object {
        /** Пять прогонов — компромисс между разбросом на эмуляторе и временем замера. */
        private const val ITERATIONS = 5

        /**
         * Режимы компиляции; аргумент инструментации `compilation` (`none` или `profile`) оставляет
         * один — сравнение «до/после» правки не тратит время на второй режим.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun modes(): List<CompilationMode> {
            val all = mapOf(
                "none" to CompilationMode.None(),
                "profile" to CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
            )
            val chosen = InstrumentationRegistry.getArguments().getString("compilation")
            return all[chosen]?.let(::listOf) ?: all.values.toList()
        }
    }
}
