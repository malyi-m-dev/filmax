import SwiftUI

/// Вкладки iPhone/iPad — как на Android-телефоне: Главная · Каталог · Поиск · Моё · Профиль. У каждой
/// свой `NavigationStack`, поэтому стеки переходов (Детали → Плеер) живут по вкладкам.
struct MainTabView: View {
    var body: some View {
        TabView {
            tab(.home) { HomeView() }
            tab(.catalog) { CatalogView() }
            tab(.search) { SearchView() }
            tab(.mine) { MineView() }
            tab(.profile) { ProfileView() }
        }
        .tint(Theme.accent)
    }

    private func tab<Content: View>(_ tab: AppTab, @ViewBuilder content: () -> Content) -> some View {
        NavigationStack {
            content().appDestinations()
        }
        .tabItem { Label(tab.title, systemImage: tab.systemImage) }
    }
}

extension View {
    /// Переходы `AppRoute` → экраны iOS. Вешается на корень каждого `NavigationStack`.
    func appDestinations() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            switch route {
            case .details(let itemId):
                DetailsView(itemId: itemId)
            case .player(let itemId, let target):
                PlayerView(itemId: itemId, target: target)
            case .collection(let id, let title):
                CollectionDetailView(collectionId: id, title: title)
            case .collectionsList:
                CollectionsView()
            case .folder(let id, let title):
                FolderView(folderId: id, title: title)
            case .filmography(let name, let isDirector):
                FilmographyView(name: name, isDirector: isDirector)
            }
        }
    }
}
