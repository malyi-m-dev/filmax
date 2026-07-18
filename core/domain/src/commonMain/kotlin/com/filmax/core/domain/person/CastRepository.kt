package com.filmax.core.domain.person

/** Актёр с фото и ролью. Фото/роль опциональны — источник (TMDB) может их не дать. */
data class CastMember(
    val name: String,
    val character: String?,
    val photoUrl: String?,
)

/**
 * Актёрский состав с фотографиями. У kino.pub людей нет вовсе — `cast` приходит строкой имён,
 * без id и фото. Поэтому фото тянем извне (TMDB) по IMDb-id тайтла: надёжное совпадение без
 * угадывания по имени.
 *
 * Это украшение, а не основа экрана: любая неудача (нет ключа TMDB, нет совпадения, сбой сети)
 * возвращает пустой список — деталям тогда достаточно строки имён от kino.pub.
 */
interface CastRepository {
    suspend fun getCast(imdbId: String?): List<CastMember>
}
