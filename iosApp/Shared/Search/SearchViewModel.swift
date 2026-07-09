import Combine
import Foundation
import Shared

/// Презентейшен поиска (iOS + tvOS) поверх общего `SearchRepository`. Дебаунс ввода — здесь;
/// сам поиск (по названию/актёру/режиссёру) — в общем слое.
@MainActor
final class SearchViewModel: ObservableObject {
    @Published var query = "" { didSet { scheduleSearch() } }
    @Published var results: [Item] = []
    @Published var isLoading = false
    @Published var error: String?
    /// true — пользователь ещё не искал (стартовое состояние).
    @Published var isIdle = true

    private let search = RepositoryProvider.shared.search
    private var task: Task<Void, Never>?

    private func scheduleSearch() {
        task?.cancel()
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else {
            results = []
            error = nil
            isIdle = true
            return
        }
        task = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 400_000_000) // дебаунс 400 мс
            if Task.isCancelled { return }
            await self?.performSearch(trimmed)
        }
    }

    private func performSearch(_ text: String) async {
        isLoading = true
        error = nil
        isIdle = false
        do {
            let result = try await search.search(query: text, type: nil, perPage: 24)
            switch onEnum(of: result) {
            case .success(let success):
                results = (success.data as? [Item]) ?? []
            case .error(let requestError):
                error = requestError.message ?? "Не удалось выполнить поиск."
                results = []
            }
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}
