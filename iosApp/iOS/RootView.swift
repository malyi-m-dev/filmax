import SwiftUI

/// Корневой экран iOS: сессия-гейт → онбординг или главный (iPhone/iPad).
/// `SessionViewModel` — общий (Shared/), различается только набор View.
struct RootView: View {
    @StateObject private var session = SessionViewModel()

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            switch session.isAuthenticated {
            case .none:
                ProgressView().tint(Theme.accent)
            case .some(true):
                MainPlaceholderView()
            case .some(false):
                OnboardingView()
            }
        }
        .task { await session.observe() }
    }
}
