import Foundation
import Shared

// Псевдонимы для доменных типов KMP, чьи имена конфликтуют со стандартной библиотекой:
//   `Collection`   ↔ `Swift.Collection` (протокол)
//   `Subscription` ↔ `Combine.Subscription` (протокол, тянется через SwiftUI)
typealias FilmCollection = Shared.Collection
typealias FilmSubscription = Shared.Subscription

// Форматирование доменных моделей для экранов — те же подписи, что у Android
// (core:presentation, core:ui). Бизнес-логика (досортировка, фильтры, оценки) — в Kotlin.

/// Оценка на постер: один знак после запятой; ноль — «оценки нет», а не «ноль баллов».
func ratingLabel(_ value: Double?) -> String? {
    guard let value, value > 0 else { return nil }
    return String(format: "%.1f", locale: Locale(identifier: "en_US"), value)
}

extension RatingSource {
    /// Подпись рядом с числом: «КП 8.1», «IMDb 7.9», у домашнего сервера — «Рейтинг 7.5».
    var shortTitle: String {
        switch self {
        case .kinopoisk: return "КП"
        case .imdb: return "IMDb"
        case .community: return "Рейтинг"
        }
    }

    var title: String {
        switch self {
        case .kinopoisk: return "КиноПоиск"
        case .imdb: return "IMDb"
        case .community: return "Рейтинг"
        }
    }
}

extension ItemType {
    /// Тип на языке зрителя, а не API.
    var label: String {
        switch self {
        case .movie: return "Фильм"
        case .series: return "Сериал"
        case .anime: return "Аниме"
        case .documentary, .documentaryMovie: return "Документальный"
        case .tvShow: return "ТВ-шоу"
        case .concert: return "Концерт"
        case .tv: return "Канал"
        }
    }
}

extension ContentSource {
    var label: String {
        switch self {
        case .kinopub: return "kino.pub"
        case .jellyfin: return "Домашний сервер"
        }
    }
}

extension CatalogSort {
    var label: String {
        switch self {
        case .updated: return "Обновлённые"
        case .created: return "Новые"
        case .rating: return "Рейтинг"
        case .views: return "Просмотры"
        case .year: return "Год"
        case .kinopoiskRating: return "КиноПоиск"
        case .imdbRating: return "IMDb"
        }
    }
}

extension Item {
    /// Главная оценка источника — для бейджа на постере.
    var ratingText: String? { ratingLabel(rating.primary?.doubleValue) }

    /// Все оценки с подписью источника: «КП 8.1 · IMDb 7.9».
    var ratingLines: [String] {
        rating.values.compactMap { value in
            ratingLabel(value.value).map { "\(value.source.shortTitle) \($0)" }
        }
    }

    /// «2021 · Фантастика, Драма» — подпись карточки.
    var metaLine: String {
        var parts: [String] = []
        if year > 0 { parts.append("\(year)") }
        let genresText = genres.prefix(2).map { $0.title }.joined(separator: ", ")
        if !genresText.isEmpty { parts.append(genresText) }
        return parts.joined(separator: " · ")
    }

    /// «Сериал · 2021» — под постером в сетке.
    var posterMeta: String {
        [type.label, year > 0 ? "\(year)" : nil].compactMap { $0 }.joined(separator: " · ")
    }

    var posterURL: String { posters.medium.isEmpty ? posters.big : posters.medium }

    /// Широкий кадр, если есть, иначе большой постер.
    var backdropURL: String { (posters.wide?.isEmpty == false ? posters.wide : nil) ?? posters.big }

    /// Сезоны и серии — по типу, а не по числу видео: у фильма их тоже бывает несколько.
    var isEpisodic: Bool { type.episodic }

    /// Номера сезонов по возрастанию.
    var seasonNumbers: [Int32] {
        Array(Set(tracklist.map { $0.seasonNumber }.filter { $0 > 0 })).sorted()
    }

    /// Серия «продолжить»: начатая, иначе первая недосмотренная после досмотренных, иначе первая.
    var resumeTrack: MediaTrack? {
        if let started = tracklist.last(where: { $0.watchStatus == 0 }) { return started }
        if let lastFinished = tracklist.lastIndex(where: { $0.watchStatus == 1 }) {
            return tracklist.indices.contains(lastFinished + 1) ? tracklist[lastFinished + 1] : nil
        }
        return tracklist.first
    }

    /// Длительность фильма: «2 ч 11 мин».
    var durationText: String? {
        guard let minutes = duration.averageMinutes?.doubleValue, minutes > 0 else { return nil }
        let total = Int(minutes)
        return total >= 60 ? "\(total / 60) ч \(total % 60) мин" : "\(total) мин"
    }
}

extension MediaTrack {
    /// «S1E2» у серии, «Серия 2» без сезона.
    var episodeCode: String {
        seasonNumber > 0 ? "S\(seasonNumber)E\(number)" : "Серия \(number)"
    }

    /// Доля просмотра 0..1; досмотренная серия — полная.
    var progressFraction: Double? {
        if watchStatus == 1 { return 1 }
        guard watchedSeconds > 0, durationSeconds > 0 else { return nil }
        return min(1, Double(watchedSeconds) / Double(durationSeconds))
    }

    /// Адрес серии для плеера и прогресса.
    var target: PlayTarget { PlayTarget(season: seasonNumber, number: number) }
}

extension WatchHistory {
    /// Доля просмотра 0..1 для полосы (считается в общем Kotlin-коде).
    var progressFraction: Double? {
        guard let progress else { return nil }
        return Double(progress.fraction)
    }

    /// «S4E7» у серии, «Фильм» у фильма.
    var subtitle: String {
        guard let season = progress?.season?.int32Value, season > 0,
              let number = progress?.videoId?.int32Value, number > 0 else { return "Фильм" }
        return "S\(season)E\(number)"
    }

    /// Где продолжить: серия с сезоном или фильм целиком.
    var target: PlayTarget? {
        guard let number = progress?.videoId?.int32Value, number > 0 else { return nil }
        return PlayTarget(season: progress?.season?.int32Value ?? 0, number: number)
    }
}

extension FilmCollection {
    var posterURL: String? { posters?.medium ?? posters?.big }
}
