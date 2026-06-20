package com.filmax.data.catalog.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemsResponseDto(
    val items: List<ItemDto>,
    val pagination: PaginationDto? = null,
)

@Serializable
data class MovieInfoDto(
    val item: ItemDto,
    @SerialName("blocked_countries") val blockedCountries: List<String>? = null,
)

@Serializable
data class ItemDto(
    val id: Int,
    val title: String,
    @SerialName("type") val type: String ="",
    val year: Int = 0,
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val voice: String = "",
    val rating: Int = 0,
    @SerialName("rating_percentage") val ratingPercentage: Double = 0.0,
    @SerialName("imdb_rating") val imdbRating: Double? = null,
    @SerialName("kinopoisk_rating") val kinopoiskRating: Double? = null,
    val finished: Boolean = false,
    @SerialName("in_watchlist") val inWatchlist: Boolean = false,
    @SerialName("posters") val posters: PostersDto? = null,
    val duration: DurationDto = DurationDto(),
    val genres: List<GenreDto> = emptyList(),
    val countries: List<CountryDto> = emptyList(),
    val videos: List<MediaTrackDto>? = null,
    val trailer: TrailerDto? = null,
)

@Serializable
data class PostersDto(
    val small: String = "",
    val medium: String = "",
    val big: String = "",
    val wide: String? = null,
)

@Serializable
data class DurationDto(
    val average: Double? = null,
    val total: Int? = null,
)

@Serializable
data class GenreDto(
    val id: Int,
    val title: String,
)

@Serializable
data class CountryDto(
    val id: Int,
    val title: String,
)

@Serializable
data class MediaTrackDto(
    val id: Int,
    val number: Int = 0,
    val snumber: Int = 0,
    val title: String = "",
    val thumbnail: String = "",
    val duration: Int = 0,
    val files: List<VideoFileDto> = emptyList(),
    val audios: List<AudioDto> = emptyList(),
    val subtitles: List<SubtitleDto> = emptyList(),
)

@Serializable
data class VideoFileDto(
    val quality: String = "",
    val url: UrlDto? = null,
    val urls: UrlsDto? = null,
)

@Serializable
data class UrlDto(
    val http: String? = null,
    val hls: String? = null,
    val hls4: String? = null,
    val hls2: String? = null,
    val hls1: String? = null,
)

@Serializable
data class UrlsDto(
    val http: String? = null,
    val hls: String? = null,
)

@Serializable
data class AudioDto(
    val id: Int,
    val index: Int = 0,
    val codec: String? = null,
    val channels: Int = 2,
    val lang: String? = null,
    val title: String? = null,
)

@Serializable
data class SubtitleDto(
    val lang: String,
    val url: String,
    val shift: Int = 0,
)

@Serializable
data class TrailerDto(
    val id: Int,
    val type: Int = 0,
    val url: String? = null,
    val file: String? = null,
)

@Serializable
data class PaginationDto(
    val total: Int = 0,
    val current: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
)
