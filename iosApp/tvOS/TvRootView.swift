import SwiftUI

/// Корень Apple TV: вход → разделы. Смена источника пересобирает разделы (`.id`), как граф на
/// Android возвращается к корню: экраны прежнего источника не должны пережить переключение.
struct TvRootView: View {
    @StateObject private var session = SessionViewModel()

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            switch session.isAuthenticated {
            case .none:
                TvLoadingView()
            case .some(true):
                TvMainTabView().id(session.source)
            case .some(false):
                TvOnboardingView()
            }
        }
        .task { await session.observe() }
    }
}
