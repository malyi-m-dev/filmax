import Combine
import Shared

/// Презентейшен экрана Деталей (iOS + tvOS). Данные и действия — общий `CatalogRepository`/
/// `WatchingRepository`/`FavoritesRepository`; здесь только состояние экрана.
@MainActor
final class DetailsViewModel: ObservableObject {
    @Published var item: Item?
    @Published var similar: [Item] = []
    @Published var isLoading = false
    @Published var error: String?
    @Published var inWatchlist = false
    @Published var isFavorite = false

    let itemId: Int32

    private let catalog = RepositoryProvider.shared.catalog
    private let favorites = RepositoryProvider.shared.favorites
    private let toggleWatchlistUseCase = UseCaseProvider.shared.toggleWatchlistUseCase()

    init(itemId: Int32) { self.itemId = itemId }

    func load() async {
        isLoading = true
        error = nil
        do {
            let result = try await catalog.getItemDetails(id: itemId)
            switch onEnum(of: result) {
            case .success(let success):
                guard let loaded = success.data as? Item else {
                    error = "Некорректный ответ сервера."
                    isLoading = false
                    return
                }
                item = loaded
                inWatchlist = loaded.inWatchlist
                await loadSimilar()
            case .error(let requestError):
                error = requestError.message ?? "Не удалось загрузить детали."
            }
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    private func loadSimilar() async {
        guard let result = try? await catalog.getSimilarItems(id: itemId) else { return }
        if case .success(let success) = onEnum(of: result) {
            similar = (success.data as? [Item]) ?? []
        }
    }

    /// Реактивно держит флаг «в избранном» из локального `FavoritesRepository` (Flow).
    func observeFavorite() async {
        for await list in favorites.favorites {
            isFavorite = list.contains { $0.id == itemId }
        }
    }

    func toggleWatchlist() async {
        guard let result = try? await toggleWatchlistUseCase.invoke(itemId: itemId) else { return }
        if case .success(let success) = onEnum(of: result) {
            inWatchlist = (success.data as? KotlinBoolean)?.boolValue ?? inWatchlist
        }
    }

    func toggleFavorite() async {
        guard let item else { return }
        _ = try? await favorites.toggle(item: item.toFavoriteItem())
        // Итоговое состояние подхватит `observeFavorite` из Flow — не дублируем логику.
    }
}
