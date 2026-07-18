package com.filmax.feature.player.common.navigation

import kotlinx.serialization.Serializable

/**
 * Маршрут трейлера. [url] — временный HLS-плейлист (.m3u8) с истекающим токеном в query-параметрах,
 * поэтому экран трейлера намеренно простой и одноразовый: по протухшему URL воспроизведение всё
 * равно не восстановить, и переживать пересоздание плеера здесь не требуется (для трейлера это ок).
 */
@Serializable
data class TrailerRoute(val url: String, val title: String)
