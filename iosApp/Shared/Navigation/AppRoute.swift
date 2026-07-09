import Foundation

/// Единый контракт переходов между фичами (общий для iOS и tvOS).
/// Значения кладутся в `NavigationStack` path; платформенные обёртки объявляют
/// `.navigationDestination(for: AppRoute.self)` со своими View.
enum AppRoute: Hashable {
    /// Экран Деталей фильма/сериала по `itemId`.
    case details(itemId: Int32)
    /// Плеер: `itemId` + опциональный выбранный трек (`videoId`); nil — первый доступный.
    case player(itemId: Int32, videoId: Int32?)
    /// Содержимое подборки.
    case collection(id: Int32, title: String)
    /// Полный список подборок.
    case collectionsList
}

/// Разделы корневого таб-бара (общие для платформ; порядок = порядок вкладок).
enum AppTab: Int, CaseIterable, Identifiable {
    case home, search, library, profile

    var id: Int { rawValue }

    var title: String {
        switch self {
        case .home: return "Главная"
        case .search: return "Поиск"
        case .library: return "Библиотека"
        case .profile: return "Профиль"
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "house.fill"
        case .search: return "magnifyingglass"
        case .library: return "square.stack.fill"
        case .profile: return "person.crop.circle.fill"
        }
    }
}
