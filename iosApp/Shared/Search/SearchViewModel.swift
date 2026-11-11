import Combine
import Foundation
import Shared

/// Поиск (iOS и tvOS) поверх общего `SearchRepository`: дебаунс ввода, недавние запросы и
/// подсказки. Повторы id убирает `ArrangeCatalog` — сетка с дублем ключа падает.
@MainActor
final class SearchViewModel: ObservableObject {
    @Published var query = "" { didSet { scheduleSearch() } }
    @Published var results: [Item] = []
    @Published var recent: [String] = []
    @Published var isLoading = false
    @Published var error: String?

    let trending = ["Мстители", "Дюна", "Офис", "Ведьмак", "Интерстеллар", "Во все тяжкие", "Оппенгеймер"]

    /// true — запрос короче двух символов: показываем подсказки, а не выдачу.
    var isIdle: Bool { query.trimmingCharacters(in: .whitespacesAndNewlines).count < Self.minQueryLength }

    private static let minQueryLength = 2
    private static let recentLimit = 8
    private let search = RepositoryProvider.shared.search
    private var task: Task<Void, Never>?

    /// Открыт результат — запрос запоминается в недавних, а не каждая набранная подстрока.
    func remember() {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= Self.minQueryLength else { return }
        recent = Array(([trimmed] + recent.filter { $0 != trimmed }).prefix(Self.recentLimit))
    }

    private func scheduleSearch() {
        task?.cancel()
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= Self.minQueryLength else {
            results = []
            error = nil
            isLoading = false
            return
        }
        // Загрузка — уже на время дебаунса: иначе на первом запросе мелькает «ничего не нашлось».
        isLoading = true
        task = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 300_000_000)
            if Task.isCancelled { return }
            await self?.perform(trimmed)
        }
    }

    private func perform(_ text: String) async {
        isLoading = true
        error = nil
        let result: Loaded<[Item]> = await request(fallback: "Не удалось выполнить поиск.") {
            try await self.search.search(query: text, type: nil, perPage: 30)
        }
        // Пока шёл запрос, зритель набрал дальше — устаревший ответ свежий не затирает.
        if Task.isCancelled { return }
        switch result {
        case .value(let items):
            results = ArrangeCatalog.shared.firstPage(items: items, sort: SortOption(field: .views, ascending: false))
        case .failure(let message):
            error = message
            results = []
        }
        isLoading = false
    }
}
