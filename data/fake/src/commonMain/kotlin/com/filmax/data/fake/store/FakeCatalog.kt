package com.filmax.data.fake.store

import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack

/** Сколько тайтлов в «Похожем» — примерно столько отдаёт kino.pub. */
private const val SIMILAR_LIMIT = 12

/**
 * Каталог «сервера»: неизменяемые фикстуры и запросы к ним — выдача с фильтрами, поиск,
 * фильмография, похожее. Состояния пользователя здесь нет, поэтому каталог можно читать из
 * любого потока без синхронизации.
 */
internal class FakeCatalog(
    val entries: List<CatalogEntry>,
    val genres: List<Genre>,
    val countries: List<Country>,
    val collections: List<FakeCollection>,
) {
    private val byId: Map<Int, CatalogEntry> = entries.associateBy { it.id }

    fun entry(id: Int): CatalogEntry? = byId[id]

    fun entryByImdbId(imdbId: String): CatalogEntry? = entries.firstOrNull { it.item.imdbId == imdbId }

    fun collection(id: Int): FakeCollection? = collections.firstOrNull { it.collection.id == id }

    /** Трек по ключу прогресса: номер серии уникален только внутри сезона, поэтому ищем по паре. */
    fun track(key: TrackKey): MediaTrack? =
        entry(key.itemId)?.item?.tracklist?.firstOrNull { it.seasonNumber == key.season && it.number == key.number }

    fun browse(query: CatalogQuery): List<CatalogEntry> =
        entries.filter { it.matches(query) }.arrangedBy(query.sort)

    /**
     * «Похожее»: тот же формат (фильм к фильмам, сериальное к сериальному) и хотя бы один общий
     * жанр; больше общих жанров — выше, при равенстве — популярнее.
     */
    fun similarTo(entry: CatalogEntry): List<CatalogEntry> {
        val genreIds = entry.item.genres.mapTo(HashSet()) { it.id }
        return entries.asSequence()
            .filter { it.id != entry.id && it.isEpisodic == entry.isEpisodic }
            .map { candidate -> candidate to candidate.item.genres.count { it.id in genreIds } }
            .filter { (_, sharedGenres) -> sharedGenres > 0 }
            .sortedWith(
                compareByDescending<Pair<CatalogEntry, Int>> { (_, sharedGenres) -> sharedGenres }
                    .thenByDescending { (candidate, _) -> candidate.views },
            )
            .map { (candidate, _) -> candidate }
            .take(SIMILAR_LIMIT)
            .toList()
    }

    /**
     * Поиск по подстроке названия без учёта регистра и разницы «е/ё». Совпадения с начала
     * названия — выше: так ведёт себя и поиск kino.pub. Пустой запрос ничего не находит.
     */
    fun searchByTitle(text: String, type: ItemType?): List<CatalogEntry> {
        val needle = text.searchable()
        if (needle.isEmpty()) return emptyList()
        return entries
            .filter { (type == null || it.isOfType(type)) && needle in it.item.title.searchable() }
            .sortedByDescending { it.item.title.searchable().startsWith(needle) }
    }

    /** Фильмография актёра: имя входит в строку `cast` тайтла. Свежие работы — первыми. */
    fun withActor(name: String): List<CatalogEntry> = withPerson(name) { it.item.cast }

    /** Работы режиссёра: имя входит в поле `director`. */
    fun withDirector(name: String): List<CatalogEntry> = withPerson(name) { it.item.director }

    private fun withPerson(name: String, credits: (CatalogEntry) -> String): List<CatalogEntry> {
        val needle = name.searchable()
        if (needle.isEmpty()) return emptyList()
        return entries
            .filter { needle in credits(it).searchable() }
            .sortedByDescending { it.item.year }
    }
}

/** Нормализация для поиска: регистр и «ё» не должны мешать найти «Шёпот в соснах» по «шепот». */
private fun String.searchable(): String = trim().lowercase().replace('ё', 'е')
