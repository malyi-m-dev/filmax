import SwiftUI

/// Корневой экран Apple TV: сессия-гейт → онбординг или главный.
/// `SessionViewModel` — общий (Shared/), отличается только набор TV-View.
struct TvRootView: View {
    @StateObject private var session = SessionViewModel()

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            switch session.isAuthenticated {
            case .none:
                ProgressView().tint(Theme.accent).scaleEffect(2)
            case .some(true):
                TvMainPlaceholderView()
            case .some(false):
                TvOnboardingView()
            }
        }
        .task { await session.observe() }
    }
}
