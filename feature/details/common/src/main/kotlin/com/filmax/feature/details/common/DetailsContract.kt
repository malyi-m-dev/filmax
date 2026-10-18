package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.person.CastMember

data class DetailsState(
    val loading: Boolean = true,
    val item: Item? = null,
    val similar: List<Item> = emptyList(),
    /**
     * Актёры с фото (TMDB) — украшение поверх строки имён от kino.pub. Пустой список, когда фото
     * недоступны (нет ключа TMDB, нет совпадения по IMDb, сбой): экран падает на строку `item.cast`.
     */
    val cast: List<CastMember> = emptyList(),
    val isFav: Boolean = false,
    val isDownloaded: Boolean = false,
    val error: String? = null,
    /** Сезоны и точка «продолжить» — только у сериала, у фильма null. */
    val series: SeriesData? = null,
    /** Номер сезона, чьи серии на экране. Стартует с сезона серии «продолжить». */
    val selectedSeason: Int? = null,
    /** Открыта боковая панель с полным описанием. */
    val descriptionOpen: Boolean = false,
) {
    /** Серии выбранного сезона; у фильма — пусто. */
    val seasonEpisodes: List<MediaTrack>
        get() = series?.seasons?.firstOrNull { it.first == selectedSeason }?.second.orEmpty()

    /**
     * Сезон через [step] от выбранного: −1 — предыдущий, +1 — следующий; null — дальше сезонов
     * нет. Так на ТВ «←/→» с серии листает сезоны, не поднимаясь по сериям к чипам.
     */
    fun adjacentSeason(step: Int): Int? {
        val seasons = series?.seasons.orEmpty()
        val index = seasons.indexOfFirst { it.first == selectedSeason }
        if (index < 0) return null
        return seasons.getOrNull(index + step)?.first
    }
}

sealed interface DetailsEvent {
    data object ToggleFav : DetailsEvent
    data object ToggleDownload : DetailsEvent

    /**
     * Сезон в фокусе. На ТВ сезон меняется фокусом на чипе, без «ОК», — модель выжидает 250 мс,
     * чтобы быстрое пролистывание чипов не перерисовывало каждый сезон по пути.
     */
    data class SelectSeason(val number: Int) : DetailsEvent

    /**
     * Сезон выбран стрелкой с серии — применяется сразу: это одно осознанное нажатие, а не
     * пролистывание чипов, которое стоит переждать. Отменяет выбор чипом, если тот ещё выжидает.
     */
    data class SwitchSeason(val number: Int) : DetailsEvent

    data object OpenDescription : DetailsEvent
    data object CloseDescription : DetailsEvent
}

sealed interface DetailsSideEffect
