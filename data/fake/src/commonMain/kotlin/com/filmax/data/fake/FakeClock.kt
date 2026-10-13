package com.filmax.data.fake

/**
 * Текущее время, мс от эпохи. Нужно фикстурам для дат «как на сервере»: конец подписки, правка
 * папок, время уведомлений. kotlinx-datetime в проекте нет, а общих часов в stdlib Kotlin 2.0
 * ещё нет — отсюда expect/actual в одну строку на платформу.
 */
internal expect fun currentEpochMillis(): Long
