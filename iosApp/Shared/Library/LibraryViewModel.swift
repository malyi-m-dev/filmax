import Combine
import Shared

/// Презентейшен Библиотеки (iOS + tvOS): история/продолжить, «Буду смотреть» (Flow), закладки
/// (папки). Данные — общие `WatchingRepository`/`UserRepository`/`WatchlistRepository`; вся логика
/// уже в общем слое.
@MainActor
final class LibraryViewModel: ObservableObject {
    @Published var history: [WatchHistory] = []
    @Published var watchlist: [WatchlistItem] = []
    @Published var folders: [BookmarkFolder] = []

    /// Выбранная папка закладок и её содержимое (in-place просмотр).
    @Published var selectedFolder: BookmarkFolder?
    @Published var folderItems: [Item] = []

    @Published var isLoading = false
    @Published var error: String?

    private let watching = RepositoryProvider.shared.watching
    private let user = RepositoryProvider.shared.user
    private let watchlistRepo = RepositoryProvider.shared.watchlist

    func load() async {
        isLoading = true
        error = nil
        await loadHistory()
        await loadFolders()
        isLoading = false
    }

    private func loadHistory() async {
        guard let result = try? await watching.getHistory(type: "all") else { return }
        if case .success(let success) = onEnum(of: result) {
            history = (success.data as? [WatchHistory]) ?? []
        } else if case .error(let requestError) = onEnum(of: result) {
            error = requestError.message
        }
    }

    private func loadFolders() async {
        guard let result = try? await user.getBookmarkFolders() else { return }
        if case .success(let success) = onEnum(of: result) {
            folders = (success.data as? [BookmarkFolder]) ?? []
        }
    }

    // MARK: - Реактивные локальные источники (Flow)

    func observeWatchlist() async {
        for await list in watchlistRepo.items { watchlist = list }
    }

    // MARK: - Закладки

    func openFolder(_ folder: BookmarkFolder) async {
        selectedFolder = folder
        folderItems = []
        guard let result = try? await user.getBookmarkItems(folderId: folder.id, page: 1) else { return }
        if case .success(let success) = onEnum(of: result), let page = success.data as? ItemPage {
            folderItems = page.items
        }
    }

    func closeFolder() {
        selectedFolder = nil
        folderItems = []
    }

    func createFolder(title: String) async {
        _ = try? await user.createBookmarkFolder(title: title)
        await loadFolders()
    }

    func deleteFolder(_ folder: BookmarkFolder) async {
        _ = try? await user.deleteBookmarkFolder(folderId: folder.id)
        await loadFolders()
    }

    // MARK: - Буду смотреть

    func removeFromWatchlist(id: Int32) async { try? await watchlistRepo.remove(itemId: id) }
}
