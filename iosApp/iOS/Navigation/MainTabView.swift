import SwiftUI

/// Корневой таб-каркас iPhone/iPad: `TabView` + отдельный `NavigationStack` в каждой вкладке,
/// чтобы стеки push-переходов (Детали/Плеер) сохранялись по вкладкам. Заменяет `MainPlaceholderView`.
struct MainTabView: View {
    var body: some View {
        TabView {
            tab(.home) { HomeView() }
            tab(.search) { SearchView() }
            tab(.library) { LibraryView() }
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
    /// Регистрирует переходы `AppRoute` → экраны iOS. Вешается на корень каждого `NavigationStack`.
    func appDestinations() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            switch route {
            case .details(let itemId):
                DetailsView(itemId: itemId)
            case .player(let itemId, let videoId):
                PlayerView(itemId: itemId, videoId: videoId)
            case .collection(let id, let title):
                CollectionDetailView(collectionId: id, title: title)
            case .collectionsList:
                CollectionsView()
            }
        }
    }
}
