import Combine
import Shared

/// Общий ViewModel авторизованного состояния (iOS + tvOS): выход из аккаунта.
@MainActor
final class MainViewModel: ObservableObject {
    private let logout = UseCaseProvider.shared.logoutUseCase()

    func signOut() {
        // Очищает токены общим data-слоем; поток авторизации вернёт на онбординг.
        Task { _ = try? await logout.invoke() }
    }
}
