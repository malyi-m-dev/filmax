import Combine
import Shared

/// Сессия-гейт (iOS и tvOS): вход в текущий источник и сам источник. Поток входа идёт через
/// переключатель источника общего слоя, поэтому смена источника — как вход или выход: корень
/// пересобирается, а вкладки начинают с начала (как граф на Android).
@MainActor
final class SessionViewModel: ObservableObject {
    /// nil — состояние ещё не определено (первый кадр загрузки).
    @Published var isAuthenticated: Bool?
    @Published var source: ContentSource = .kinopub

    private let observeAuthState = UseCaseProvider.shared.observeAuthStateUseCase()
    private let sources = RepositoryProvider.shared.sources

    func observe() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.observeAuth() }
            group.addTask { await self.observeSource() }
        }
    }

    private func observeAuth() async {
        // Flow<Boolean> приходит как KotlinBoolean → .boolValue.
        for await authenticated in observeAuthState.invoke() {
            isAuthenticated = authenticated.boolValue
        }
    }

    private func observeSource() async {
        for await current in sources.source {
            source = current
        }
    }
}
