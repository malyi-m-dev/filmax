package com.filmax.data.fake

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

private const val MILLIS_IN_SECOND = 1000.0

internal actual fun currentEpochMillis(): Long = (NSDate().timeIntervalSince1970 * MILLIS_IN_SECOND).toLong()
