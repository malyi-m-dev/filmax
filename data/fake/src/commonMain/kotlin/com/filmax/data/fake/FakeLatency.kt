package com.filmax.data.fake

import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Искусственная задержка «сети». Мгновенные ответы прячут состояния загрузки — скелетоны,
 * индикаторы догрузки, «фото актёров доезжают позже», — а ради них мок-сборка и нужна. Поэтому
 * по умолчанию каждый ответ ждёт 300–600 мс. Тестам ждать незачем — для них [None].
 */
class FakeLatency(
    private val minMillis: Long,
    private val maxMillis: Long,
    private val random: Random = Random.Default,
) {
    init {
        require(minMillis in 0..maxMillis) { "Неверный диапазон задержки: $minMillis..$maxMillis мс" }
    }

    /** Ждёт случайное время из диапазона; нулевой диапазон не ждёт вовсе. */
    suspend fun await() {
        if (maxMillis == 0L) return
        delay(random.nextLong(minMillis, maxMillis + 1))
    }

    companion object {
        /** Похоже на мобильную сеть: загрузку видно, но ждать не утомительно. */
        val Realistic: FakeLatency = FakeLatency(minMillis = 300, maxMillis = 600)

        /** Без задержки — для юнит-тестов. */
        val None: FakeLatency = FakeLatency(minMillis = 0, maxMillis = 0)
    }
}
