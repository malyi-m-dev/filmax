import Combine
import Shared

/// Презентейшен экрана Деталей (iOS + tvOS). Данные и действия — общий `CatalogRepository` и
/// `WatchlistRepository` текущего источника; здесь только состояние экрана.
@MainActor
final class DetailsViewModel: ObservableObject {
    @Published var item: Item?
    @Published var similar: [Item] = []
    @Published var isLoading = false
    @Published var error: String?
    @Published var inWatchlist = false

    let itemId: Int32

    private let catalog = RepositoryProvider.shared.catalog
    private let watchlist = RepositoryProvider.shared.watchlist
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
                // Отметку поставили в вебе или на другом устройстве — список её подхватывает.
                if loaded.inWatchlist { try? await watchlist.adopt(item: loaded.toWatchlistItem()) }
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

    /// Кнопка «Буду смотреть» следит за списком источника, а не за флагом тайтла: так она не
    /// расходится с «Моим».
    func observeWatchlist() async {
        for await marked in watchlist.contains(itemId: itemId) {
            inWatchlist = marked.boolValue
        }
    }

    func toggleWatchlist() async {
        guard let item else { return }
        // Итоговое состояние подхватит `observeWatchlist` из Flow — не дублируем логику.
        _ = try? await toggleWatchlistUseCase.invoke(item: item)
    }
}
