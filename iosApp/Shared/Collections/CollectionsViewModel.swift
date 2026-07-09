import Combine
import Shared

/// Список подборок с постраничной догрузкой (iOS + tvOS) поверх `CatalogRepository`.
@MainActor
final class CollectionsViewModel: ObservableObject {
    @Published var collections: [FilmCollection] = []
    @Published var isLoading = false
    @Published var error: String?

    private let catalog = RepositoryProvider.shared.catalog
    private var page: Int32 = 1
    private var canLoadMore = true

    func loadInitial() async {
        guard collections.isEmpty else { return }
        page = 1
        canLoadMore = true
        await loadNext()
    }

    func loadNext() async {
        guard canLoadMore, !isLoading else { return }
        isLoading = true
        error = nil
        do {
            let result = try await catalog.getCollections(page: page)
            switch onEnum(of: result) {
            case .success(let success):
                let batch = (success.data as? [FilmCollection]) ?? []
                collections.append(contentsOf: batch)
                // Список подборок не отдаёт метаданные пагинации — тянем до пустой страницы.
                if batch.isEmpty { canLoadMore = false } else { page += 1 }
            case .error(let requestError):
                if collections.isEmpty { error = requestError.message ?? "Не удалось загрузить подборки." }
                canLoadMore = false
            }
        } catch {
            if collections.isEmpty { self.error = error.localizedDescription }
        }
        isLoading = false
    }
}

/// Содержимое одной подборки с постраничной догрузкой (использует `Pagination.hasNextPage`).
@MainActor
final class CollectionDetailViewModel: ObservableObject {
    let collectionId: Int32
    @Published var items: [Item] = []
    @Published var isLoading = false
    @Published var error: String?

    private let catalog = RepositoryProvider.shared.catalog
    private var page: Int32 = 1
    private var hasNext = true

    init(collectionId: Int32) { self.collectionId = collectionId }

    func loadInitial() async {
        guard items.isEmpty else { return }
        page = 1
        hasNext = true
        await loadNext()
    }

    func loadNext() async {
        guard hasNext, !isLoading else { return }
        isLoading = true
        error = nil
        do {
            let result = try await catalog.getCollectionItems(collectionId: collectionId, page: page)
            switch onEnum(of: result) {
            case .success(let success):
                if let collectionPage = success.data as? CollectionPage {
                    items.append(contentsOf: collectionPage.items)
                    hasNext = collectionPage.pagination.hasNextPage
                    if hasNext { page += 1 }
                } else {
                    hasNext = false
                }
            case .error(let requestError):
                if items.isEmpty { error = requestError.message ?? "Не удалось загрузить подборку." }
                hasNext = false
            }
        } catch {
            if items.isEmpty { self.error = error.localizedDescription }
        }
        isLoading = false
    }
}
