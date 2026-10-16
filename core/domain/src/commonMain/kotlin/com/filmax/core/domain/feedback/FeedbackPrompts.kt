package com.filmax.core.domain.feedback

/**
 * Когда окно оценки открывается само: после обновления приложения и после досмотра. Из Профиля
 * оно открывается всегда — это решает человек, а не политика.
 *
 * Сами по себе окна не должны надоедать:
 * - не чаще раза в [PROMPT_INTERVAL_MS] (60 дней), какой бы триггер ни сработал;
 * - после отправленного отзыва — тишина [AFTER_SENT_MS] (120 дней);
 * - после досмотра — не раньше [MIN_FINISHED_WATCHES]-го: первый фильм ещё не повод судить;
 * - после обновления — только при смене версии, а не при первом запуске после установки.
 *
 * [now] — часы в миллисекундах, подменяются в тестах.
 */
class FeedbackPrompts(
    private val store: FeedbackPromptStore,
    private val appVersion: String,
    private val now: () -> Long,
) {

    /** Запуск приложения: запоминает версию. true — это обновление, и окно пора показать. */
    suspend fun afterAppStart(): Boolean {
        val state = store.load()
        if (state.lastSeenVersion == appVersion) return false
        store.save(state.copy(lastSeenVersion = appVersion))
        val updated = state.lastSeenVersion != null
        return updated && quietPeriodPassed(state)
    }

    /** Фильм или последняя доступная серия досмотрены. true — окно пора показать. */
    suspend fun afterWatchFinished(): Boolean {
        val state = store.load().let { it.copy(finishedWatches = it.finishedWatches + 1) }
        store.save(state)
        return state.finishedWatches >= MIN_FINISHED_WATCHES && quietPeriodPassed(state)
    }

    /** Окно открылось само — отсчёт интервала до следующего показа. */
    suspend fun onPrompted() {
        store.save(store.load().copy(lastPromptAt = now()))
    }

    /** Отзыв отправлен — долгая тишина. */
    suspend fun onSent() {
        store.save(store.load().copy(lastSentAt = now()))
    }

    private fun quietPeriodPassed(state: FeedbackPromptState): Boolean {
        val time = now()
        val promptedLongAgo = state.lastPromptAt?.let { time - it >= PROMPT_INTERVAL_MS } ?: true
        val sentLongAgo = state.lastSentAt?.let { time - it >= AFTER_SENT_MS } ?: true
        return promptedLongAgo && sentLongAgo
    }

    companion object {
        const val MIN_FINISHED_WATCHES = 3
        const val PROMPT_INTERVAL_MS = 60L * DAY_MS
        const val AFTER_SENT_MS = 120L * DAY_MS
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000
