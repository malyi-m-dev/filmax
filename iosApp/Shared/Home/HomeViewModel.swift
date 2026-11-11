import Combine
import Shared

/// Главная (iOS и tvOS): лента общего `GetHomeFeedUseCase` — герой, «Продолжить», новинки,
/// подборки — и тематические ряды, те же, что у Android (`HomeShelves`). Ряды приходят после
/// ленты и листаются дальше постранично.
@MainActor
final class HomeViewModel: ObservableObject {
    struct Shelf: Identifiable {
        let kind: HomeShelf
        var items: [Item]
        var page: Int32
        var hasMore: Bool
        var id: String { kind.title }
    }

    @Published var feed: HomeFeed?
    @Published var shelves: [Shelf] = []
    @Published var watchlistIds: Set<Int32> = []
    @Published var isLoading = false
    /// Жёсткая ошибка (пусто + сбой) — экран ошибки. Мягкая (`HomeFeed.error`) — баннером.
    @Published var error: String?

    private let getHomeFeed = UseCaseProvider.shared.getHomeFeedUseCase()
    private let homeShelves = UseCaseProvider.shared.homeShelves()
    private let toggleWatchlistUseCase = UseCaseProvider.shared.toggleWatchlistUseCase()
    private let watchlist = RepositoryProvider.shared.watchlist
    private var loadingMore: Set<String> = []

    func load() async {
        isLoading = true
        error = nil
        do {
            let loaded = try await getHomeFeed.invoke()
            feed = loaded
            if !loaded.hasContent { error = loaded.error ?? "Не удалось загрузить ленту." }
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
        await loadShelves()
    }

    func observeWatchlist() async {
        for await items in watchlist.items {
            watchlistIds = Set(items.map { $0.id })
        }
    }

    func toggleWatchlist(_ item: Item) async {
        _ = try? await toggleWatchlistUseCase.invoke(item: item)
    }

    /// Догрузка ряда при подходе к его концу; повторный вызов во время загрузки — пустой.
    func loadMore(_ shelf: Shelf) async {
        guard shelf.hasMore, !loadingMore.contains(shelf.id),
              let index = shelves.firstIndex(where: { $0.id == shelf.id }) else { return }
        loadingMore.insert(shelf.id)
        let next = shelf.page + 1
        let result: Loaded<ItemPage> = await request(fallback: "") {
            try await self.homeShelves.load(shelf: shelf.kind, page: next)
        }
        if let page = result.value {
            let known = Set(shelves[index].items.map { $0.id })
            shelves[index].items += page.items.filter { !known.contains($0.id) }
            shelves[index].page = next
            shelves[index].hasMore = page.pagination.hasNextPage
        }
        loadingMore.remove(shelf.id)
    }

    /// Ряды — параллельно, а показываются в порядке ленты; пустые и упавшие выпадают.
    private func loadShelves() async {
        let kinds: [HomeShelf] = [.popularSeries, .cartoons, .comedies, .sciFi, .anime, .doramas, .documentary, .tvShows, .concerts]
        var loaded: [HomeShelf: Shelf] = [:]
        await withTaskGroup(of: (HomeShelf, ItemPage?).self) { group in
            for kind in kinds {
                group.addTask {
                    let result: Loaded<ItemPage> = await request(fallback: "") {
                        try await self.homeShelves.load(shelf: kind, page: 1)
                    }
                    return (kind, result.value)
                }
            }
            for await (kind, page) in group {
                guard let page, !page.items.isEmpty else { continue }
                loaded[kind] = Shelf(kind: kind, items: page.items, page: 1, hasMore: page.pagination.hasNextPage)
            }
        }
        shelves = kinds.compactMap { loaded[$0] }
    }
}
