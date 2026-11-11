import Combine
import Shared

/// Детали (iOS и tvOS): тайтл, сезоны и серии, «Похожие», «Буду смотреть» и папки (только у
/// источника с папками). Как на Android: кнопка «Буду смотреть» следит за списком источника, а не
/// за флагом тайтла, отметка с другого устройства подхватывается, сериал открывается на сезоне
/// серии «продолжить».
@MainActor
final class DetailsViewModel: ObservableObject {
    @Published var item: Item?
    @Published var similar: [Item] = []
    @Published var selectedSeason: Int32?
    @Published var inWatchlist = false
    @Published var folders: [BookmarkFolder] = []
    @Published var itemFolders: Set<Int32> = []
    @Published var isLoading = false
    @Published var error: String?

    let itemId: Int32
    let foldersAvailable: Bool

    private let catalog = RepositoryProvider.shared.catalog
    private let watchlist = RepositoryProvider.shared.watchlist
    private let toggleWatchlistUseCase = UseCaseProvider.shared.toggleWatchlistUseCase()
    private let bookmarkFolders = UseCaseProvider.shared.bookmarkFolders()
    private let user = RepositoryProvider.shared.user

    /// Папка «Буду смотреть» — системная (WATCHLIST_FOLDER_TITLE в домене): среди папок её нет.
    private static let watchlistFolderTitle = "Буду смотреть"

    init(itemId: Int32) {
        self.itemId = itemId
        foldersAvailable = CatalogKit.shared.capabilities(source: RepositoryProvider.shared.sources.source.value).folders
    }

    /// Серии выбранного сезона.
    var seasonEpisodes: [MediaTrack] {
        guard let item, item.isEpisodic, let season = selectedSeason else { return [] }
        return item.tracklist.filter { $0.seasonNumber == season }
    }

    /// Главная кнопка: фильм — целиком, сериал — серия «продолжить».
    var playTarget: PlayTarget? {
        guard let item else { return nil }
        guard item.isEpisodic else { return nil }
        return item.resumeTrack?.target
    }

    var playTitle: String {
        guard let item, item.isEpisodic, let resume = item.resumeTrack else { return "Смотреть" }
        return resume.watchStatus == 0 ? "Продолжить \(resume.episodeCode)" : "Смотреть \(resume.episodeCode)"
    }

    func load() async {
        isLoading = true
        error = nil
        let result: Loaded<Item> = await request(fallback: "Не удалось загрузить детали.") {
            try await self.catalog.getItemDetails(id: self.itemId)
        }
        switch result {
        case .value(let loaded):
            item = loaded
            // Возврат из плеера перезагружает детали — выбранный сезон остаётся, если он ещё есть.
            if let chosen = selectedSeason, loaded.seasonNumbers.contains(chosen) {
                selectedSeason = chosen
            } else {
                selectedSeason = loaded.resumeTrack?.seasonNumber ?? loaded.seasonNumbers.first
            }
            // Отметку поставили в вебе или на другом устройстве — список её подхватывает.
            if loaded.inWatchlist { try? await watchlist.adopt(item: loaded.toWatchlistItem()) }
            isLoading = false
            await loadSimilar()
            await loadFolders()
        case .failure(let message):
            error = message
            isLoading = false
        }
    }

    func observeWatchlist() async {
        for await marked in watchlist.contains(itemId: itemId) {
            inWatchlist = marked.boolValue
        }
    }

    func toggleWatchlist() async {
        guard let item else { return }
        // Итоговое состояние подхватит `observeWatchlist`.
        _ = try? await toggleWatchlistUseCase.invoke(item: item)
    }

    /// Положить в папку или убрать из неё.
    func toggle(folder: BookmarkFolder) async {
        let inFolder = itemFolders.contains(folder.id)
        // `try await` справа от тернарного оператора Swift не принимает — ветки явные.
        let ok = await succeeded {
            if inFolder {
                return try await self.user.removeFromBookmark(itemId: self.itemId, folderId: folder.id)
            }
            return try await self.user.addToBookmark(itemId: self.itemId, folderId: folder.id)
        }
        guard ok else { return }
        if inFolder { itemFolders.remove(folder.id) } else { itemFolders.insert(folder.id) }
    }

    func createFolder(title: String) async {
        let trimmed = title.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        let created: Loaded<BookmarkFolder> = await request(fallback: "") {
            try await self.bookmarkFolders.create(title: trimmed, tint: 0)
        }
        guard let folder = created.value else { return }
        folders.append(folder)
        await toggle(folder: folder)
    }

    private func loadSimilar() async {
        let result: Loaded<[Item]> = await request(fallback: "") { try await self.catalog.getSimilarItems(id: self.itemId) }
        similar = result.value ?? []
    }

    private func loadFolders() async {
        guard foldersAvailable else { return }
        let listed: Loaded<[BookmarkFolder]> = await request(fallback: "") { try await self.bookmarkFolders.list() }
        folders = (listed.value ?? []).filter { $0.title != Self.watchlistFolderTitle }
        let ids = (try? await bookmarkFolders.itemFolders(itemId: itemId, folders: folders)) ?? []
        itemFolders = Set(ids.map { $0.int32Value })
    }
}
