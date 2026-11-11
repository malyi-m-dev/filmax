import Foundation
import Shared

/// Что играть: серия с сезоном или видео фильма по НОМЕРУ (не id трека). Номер серии уникален
/// только внутри сезона, поэтому адрес — пара. Swift-зеркало доменного `VideoRef`: тот — Kotlin-
/// интерфейс и в `NavigationStack` не кладётся.
struct PlayTarget: Hashable {
    let season: Int32
    let number: Int32

    /// Доменный адрес для прогресса и выбора трека.
    var ref: VideoRef {
        season > 0 ? VideoRefEpisode(season: season, number: number) : VideoRefMovie(number: number)
    }

    func matches(_ track: MediaTrack) -> Bool {
        season > 0 ? track.seasonNumber == season && track.number == number : track.number == number
    }
}

/// Единый контракт переходов (общий для iOS и tvOS). Значения кладутся в `NavigationStack` path,
/// платформенные обёртки разворачивают их в свои экраны.
enum AppRoute: Hashable {
    case details(itemId: Int32)
    /// Плеер: nil — фильм целиком или серия «продолжить» (плеер выберет сам).
    case player(itemId: Int32, target: PlayTarget?)
    case collection(id: Int32, title: String)
    case collectionsList
    case folder(id: Int32, title: String)
    case filmography(name: String, isDirector: Bool)
}

/// Разделы корневого таб-бара — как на Android: на телефоне ещё Поиск отдельной вкладкой.
enum AppTab: Int, CaseIterable, Identifiable {
    case home, catalog, search, mine, profile

    var id: Int { rawValue }

    var title: String {
        switch self {
        case .home: return "Главная"
        case .catalog: return "Каталог"
        case .search: return "Поиск"
        case .mine: return "Моё"
        case .profile: return "Профиль"
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "house.fill"
        case .catalog: return "square.grid.2x2.fill"
        case .search: return "magnifyingglass"
        case .mine: return "bookmark.fill"
        case .profile: return "person.crop.circle.fill"
        }
    }
}
