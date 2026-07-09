import SwiftUI

/// Корневой таб-каркас Apple TV: `TabView` на tvOS рисует верхний таб-бар с focus-навигацией.
/// В каждой вкладке — свой `NavigationStack` для пультовых push-переходов (Детали/Плеер).
/// Заменяет `TvMainPlaceholderView`.
struct TvMainTabView: View {
    var body: some View {
        TabView {
            tab(.home) { TvHomeView() }
            tab(.search) { TvSearchView() }
            tab(.library) { TvLibraryView() }
            tab(.profile) { TvProfileView() }
        }
    }

    private func tab<Content: View>(_ tab: AppTab, @ViewBuilder content: () -> Content) -> some View {
        NavigationStack {
            content().tvAppDestinations()
        }
        .tabItem { Label(tab.title, systemImage: tab.systemImage) }
    }
}

extension View {
    /// Регистрирует переходы `AppRoute` → экраны tvOS.
    func tvAppDestinations() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            switch route {
            case .details(let itemId):
                TvDetailsView(itemId: itemId)
            case .player(let itemId, let videoId):
                TvPlayerView(itemId: itemId, videoId: videoId)
            case .collection(let id, let title):
                TvCollectionDetailView(collectionId: id, title: title)
            case .collectionsList:
                TvCollectionsView()
            }
        }
    }
}
