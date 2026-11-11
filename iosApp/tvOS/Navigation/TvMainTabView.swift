import SwiftUI

/// Разделы Apple TV — как на Android TV: Главная · Каталог · Моё · Профиль. Поиск живёт внутри
/// Каталога, отдельной вкладки нет. `TabView` на tvOS — верхний таб-бар под пульт; у каждой
/// вкладки свой `NavigationStack`, поэтому стек переходов (Детали → Плеер) живёт по вкладкам.
struct TvMainTabView: View {
    var body: some View {
        TabView {
            tab(.home) { TvHomeView() }
            tab(.catalog) { TvCatalogView() }
            tab(.mine) { TvMineView() }
            tab(.profile) { TvProfileView() }
        }
    }

    private func tab<Content: View>(_ tab: AppTab, @ViewBuilder content: () -> Content) -> some View {
        NavigationStack {
            content().tvAppDestinations()
        }
        .tabItem { Text(tab.title) }
    }
}

extension View {
    /// Переходы `AppRoute` → экраны tvOS. Вешается на корень каждого `NavigationStack`.
    func tvAppDestinations() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            TvRouteScreen(route: route)
        }
    }
}

/// Единственное место, где адрес превращается в экран tvOS (аналог `TvRoutes` на Android).
private struct TvRouteScreen: View {
    let route: AppRoute

    var body: some View {
        switch route {
        case .details(let itemId):
            TvDetailsView(itemId: itemId)
        case .player(let itemId, let target):
            TvPlayerView(itemId: itemId, target: target)
        case .collection(let id, let title):
            TvCollectionDetailView(collectionId: id, title: title)
        case .collectionsList:
            TvCollectionsView()
        case .folder(let id, let title):
            TvFolderView(folderId: id, title: title)
        case .filmography(let name, let isDirector):
            TvFilmographyView(name: name, isDirector: isDirector)
        }
    }
}
