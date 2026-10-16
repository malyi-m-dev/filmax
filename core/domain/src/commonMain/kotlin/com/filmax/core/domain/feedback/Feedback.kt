package com.filmax.core.domain.feedback

import com.filmax.core.domain.common.RequestResult

/**
 * Отзыв о приложении: оценка от 1 до 5 и необязательный комментарий. [trigger] — где окно
 * открылось: по нему видно, хвалят ли после досмотра и ругают ли после обновления.
 */
data class Feedback(
    val rating: Int,
    val comment: String,
    val trigger: FeedbackTrigger,
    val client: ClientInfo,
    /** Логин kino.pub — чтобы ответить человеку или найти его краши; null — не вошёл. */
    val username: String?,
) {
    init {
        require(rating in RATING_RANGE) { "Оценка $rating вне ${RATING_RANGE.first}–${RATING_RANGE.last}" }
    }

    companion object {
        val RATING_RANGE = 1..5
        const val COMMENT_MAX_LENGTH = 1000
    }
}

/** Откуда открылось окно оценки. */
enum class FeedbackTrigger { Profile, AfterUpdate, AfterWatch }

/**
 * Что известно о приложении и устройстве: версия, форм-фактор, модель и Android. Собирает
 * `:app` — он один знает BuildConfig и Build.MODEL.
 */
data class ClientInfo(
    val appVersion: String,
    val isTv: Boolean,
    val device: String,
    val os: String,
)

/** Куда уходят отзывы. Отправка без входа в Firebase: правила базы разрешают только создать отзыв. */
interface FeedbackRepository {
    suspend fun send(feedback: Feedback): RequestResult<Unit>
}

/**
 * Память показов окна на устройстве: когда показывали и отправляли, сколько досмотров было и
 * с какой версией приложение запускалось в прошлый раз.
 */
interface FeedbackPromptStore {
    suspend fun load(): FeedbackPromptState

    suspend fun save(state: FeedbackPromptState)
}

data class FeedbackPromptState(
    /** Когда окно последний раз открылось само (мс от эпохи); null — ни разу. */
    val lastPromptAt: Long? = null,
    /** Когда последний раз отправили отзыв; null — ни разу. */
    val lastSentAt: Long? = null,
    /** Сколько фильмов и серий досмотрено до конца с момента установки. */
    val finishedWatches: Int = 0,
    /** Версия при прошлом запуске; null — первый запуск после установки. */
    val lastSeenVersion: String? = null,
)
