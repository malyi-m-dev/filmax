import Combine
import Shared

/// «Моё» (iOS и tvOS): «Продолжить · Буду смотреть · Папки · История», как на Android. Папок у
/// домашнего сервера нет — сегмента тоже нет. «Буду смотреть» — живой поток списка источника.
@MainActor
final class MineViewModel: ObservableObject {
    enum Segment: String, CaseIterable, Identifiable {
        case continueWatching = "Продолжить"
        case watchlist = "Буду смотреть"
        case folders = "Папки"
        case history = "История"
        var id: String { rawValue }
    }

    @Published var segment: Segment = .continueWatching
    @Published var history: [WatchHistory] = []
    @Published var watchlist: [WatchlistItem] = []
    @Published var folders: [BookmarkFolder] = []
    @Published var folderPosters: [Int32: [String]] = [:]
    @Published var isLoading = false
    @Published var error: String?

    let segments: [Segment]

    private let watching = RepositoryProvider.shared.watching
    private let watchlistRepository = RepositoryProvider.shared.watchlist
    private let bookmarkFolders = UseCaseProvider.shared.bookmarkFolders()
    private static let watchlistFolderTitle = "Буду смотреть"

    init() {
        let foldersAvailable = CatalogKit.shared.capabilities(source: RepositoryProvider.shared.sources.source.value).folders
        segments = Segment.allCases.filter { $0 != .folders || foldersAvailable }
    }

    /// Начатые и недосмотренные: досмотренный фильм и сериал с досмотренной последней серией — нет.
    var continueWatching: [WatchHistory] {
        history.filter { entry in
            guard let progress = entry.progress else { return false }
            return progress.status == 0 && progress.fraction < 0.95
        }
    }

    func load() async {
        isLoading = true
        error = nil
        let loaded: Loaded<[WatchHistory]> = await request(fallback: "Не удалось загрузить историю.") {
            try await self.watching.getHistory(type: "all")
        }
        switch loaded {
        case .value(let entries): history = entries
        case .failure(let message): error = message
        }
        isLoading = false
        await loadFolders()
    }

    func observeWatchlist() async {
        for await items in watchlistRepository.items { watchlist = items }
    }

    func removeFromWatchlist(_ itemId: Int32) async {
        try? await watchlistRepository.remove(itemId: itemId)
    }

    func forget(_ entry: WatchHistory) async {
        if await succeeded({ try await self.watching.clearHistory(itemId: entry.itemId) }) {
            history.removeAll { $0.itemId == entry.itemId }
        }
    }

    func createFolder(title: String) async {
        let trimmed = title.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        let created: Loaded<BookmarkFolder> = await request(fallback: "") {
            try await self.bookmarkFolders.create(title: trimmed, tint: 0)
        }
        if created.value != nil { await loadFolders() }
    }

    func delete(folder: BookmarkFolder) async {
        if await succeeded({ try await self.bookmarkFolders.delete(folderId: folder.id) }) {
            folders.removeAll { $0.id == folder.id }
        }
    }

    private func loadFolders() async {
        guard segments.contains(.folders) else { return }
        let shelf: Loaded<FolderShelf> = await request(fallback: "") { try await self.bookmarkFolders.shelf() }
        guard let loaded = shelf.value else { return }
        folders = loaded.folders.filter { $0.title != Self.watchlistFolderTitle }
        var posters: [Int32: [String]] = [:]
        for (id, urls) in loaded.posters { posters[id.int32Value] = urls }
        folderPosters = posters
    }
}
