import Foundation
import Shared

// Псевдонимы для доменных типов KMP, чьи имена конфликтуют со стандартной библиотекой:
//   `Collection`   ↔ `Swift.Collection` (протокол)
//   `Subscription` ↔ `Combine.Subscription` (протокол, тянется через SwiftUI)
typealias FilmCollection = Shared.Collection
typealias FilmSubscription = Shared.Subscription

// Небольшие презентационные хелперы поверх общих доменных моделей KMP.
// ВАЖНО: бизнес-логику не дублируем (вычисляемые свойства `external`/`fraction`/`initials()`
// живут в Kotlin) — здесь только форматирование строк для UI.

extension Item {
    /// Текст рейтинга для бейджа: берём внешние оценки (String? из общего слоя, без numeric-интеропа).
    var ratingText: String? {
        if let imdb = rating.imdb, !imdb.isEmpty { return imdb }
        if let kp = rating.kinopoisk, !kp.isEmpty { return kp }
        return nil
    }

    /// Строка «год · жанры» для подписи карточки/деталей.
    var metaLine: String {
        var parts: [String] = []
        if year > 0 { parts.append("\(year)") }
        let genresText = genres.prefix(2).map { $0.title }.joined(separator: ", ")
        if !genresText.isEmpty { parts.append(genresText) }
        return parts.joined(separator: " · ")
    }

    /// URL постера среднего размера (для сеток/рельс).
    var posterURL: String { posters.medium }

    /// Широкий бэкдроп, если есть, иначе большой постер.
    var backdropURL: String { posters.wide ?? posters.big }

    var isSeries: Bool { tracklist.count > 1 }
}

extension WatchHistory {
    /// Доля просмотра 0..1 для прогресс-полосы (fraction считается в общем Kotlin-коде).
    var progressFraction: Double? {
        guard let progress else { return nil }
        return Double(progress.fraction)
    }
}

extension FilmCollection {
    var posterURL: String? { posters?.medium ?? posters?.big }
}
