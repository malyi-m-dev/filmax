package com.filmax.data.jellyfin.mapper

/**
 * Ссылки на картинки сервера. Размер просим под место на экране — сервер сам ужимает постер, и
 * ТВ-приставка не декодирует оригинал 2000×3000 ради карточки. `tag` — версия картинки: с ним
 * Coil не держит устаревший кэш, когда постер на сервере заменили.
 */
internal class JellyfinImages(private val address: String) {

    fun primary(itemId: String?, tag: String?, maxHeight: Int): String =
        image(itemId, tag, "Primary", "maxHeight=$maxHeight")

    fun backdrop(itemId: String?, tag: String?): String? =
        image(itemId, tag, "Backdrop/0", "maxWidth=$BACKDROP_WIDTH").ifEmpty { null }

    fun user(userId: String?, tag: String?): String? =
        if (userId == null || tag == null) null else "$address/Users/$userId/Images/Primary?tag=$tag&maxHeight=$AVATAR"

    private fun image(itemId: String?, tag: String?, kind: String, size: String): String =
        if (itemId == null || tag == null) "" else "$address/Items/$itemId/Images/$kind?$size&quality=$QUALITY&tag=$tag"

    companion object {
        const val POSTER_SMALL = 300
        const val POSTER_MEDIUM = 450
        const val POSTER_BIG = 900
        const val EPISODE_STILL = 270
        private const val BACKDROP_WIDTH = 1280
        private const val AVATAR = 200
        private const val QUALITY = 90
    }
}
