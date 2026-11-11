import SwiftUI

/// Корень iOS: вход → вкладки. Смена источника пересобирает вкладки (`.id`), как граф на Android
/// возвращается к корню: экраны прежнего источника не должны пережить переключение.
struct RootView: View {
    @StateObject private var session = SessionViewModel()

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            switch session.isAuthenticated {
            case .none:
                ProgressView().tint(Theme.accent)
            case .some(true):
                MainTabView().id(session.source)
            case .some(false):
                OnboardingView()
            }
        }
        .task { await session.observe() }
    }
}
