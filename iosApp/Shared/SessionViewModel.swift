import Combine
import Shared

/// Сессия-гейт (общий для iOS и tvOS): слушает общий поток авторизации из KMP.
/// Платформо-независим — используется и в iOS-, и в tvOS-таргете.
@MainActor
final class SessionViewModel: ObservableObject {
    /// nil — состояние ещё не определено (первый кадр загрузки).
    @Published var isAuthenticated: Bool?

    private let observeAuthState = UseCaseProvider.shared.observeAuthStateUseCase()

    func observe() async {
        // SKIE делает Flow<Boolean> AsyncSequence, Kotlin Boolean → Swift Bool.
        for await authenticated in observeAuthState.invoke() {
            isAuthenticated = authenticated
        }
    }
}
