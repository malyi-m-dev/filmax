import Combine
import Shared

/// Фильмография актёра или режиссёра (iOS и tvOS) — тап по имени в деталях.
@MainActor
final class FilmographyViewModel: ObservableObject {
    let name: String
    let isDirector: Bool
    @Published var items: [Item] = []
    @Published var isLoading = false
    @Published var error: String?

    private let search = RepositoryProvider.shared.search
    private static let perPage: Int32 = 60

    init(name: String, isDirector: Bool) {
        self.name = name
        self.isDirector = isDirector
    }

    func load() async {
        guard items.isEmpty else { return }
        isLoading = true
        let result: Loaded<[Item]> = await request(fallback: "Не удалось загрузить фильмографию.") {
            if self.isDirector {
                return try await self.search.searchByDirector(director: self.name, perPage: Self.perPage)
            }
            return try await self.search.searchByActor(actor: self.name, perPage: Self.perPage)
        }
        switch result {
        case .value(let found): items = found.sorted { $0.year > $1.year }
        case .failure(let message): error = message
        }
        isLoading = false
    }
}
