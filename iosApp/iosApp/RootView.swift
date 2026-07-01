import SwiftUI
import Shared

/// Сессия-гейт: слушает общий поток авторизации и выбирает онбординг либо главный экран.
@MainActor
final class SessionViewModel: ObservableObject {
    /// nil — состояние ещё не определено (первый кадр загрузки).
    @Published var isAuthenticated: Bool?

    private let observeAuthState = UseCaseProvider.shared.observeAuthStateUseCase()

    func observe() async {
        // SKIE делает Flow<Boolean> AsyncSequence, Kotlin Boolean → Swift Bool.
        // (Если SKIE отдаёт KotlinBoolean — заменить на `authenticated.boolValue`.)
        for await authenticated in observeAuthState.invoke() {
            isAuthenticated = authenticated
        }
    }
}

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
