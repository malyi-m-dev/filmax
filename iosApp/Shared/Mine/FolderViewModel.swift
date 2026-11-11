import Combine
import Shared

/// Открытая папка-закладка (iOS и tvOS): тайтлы с постраничной догрузкой и «убрать из папки».
@MainActor
final class FolderViewModel: ObservableObject {
    let folderId: Int32
    @Published var items: [Item] = []
    @Published var isLoading = false
    @Published var error: String?

    private let user = RepositoryProvider.shared.user
    private var page: Int32 = 1
    private var hasNext = true

    init(folderId: Int32) { self.folderId = folderId }

    func loadNext() async {
        guard hasNext, !isLoading else { return }
        isLoading = true
        let result: Loaded<ItemPage> = await request(fallback: "Не удалось открыть папку.") {
            try await self.user.getBookmarkItems(folderId: self.folderId, page: self.page)
        }
        switch result {
        case .value(let loaded):
            // Страницы закладок пересекаются: повтор id роняет сетку.
            let known = Set(items.map { $0.id })
            items += loaded.items.filter { !known.contains($0.id) }
            hasNext = loaded.pagination.hasNextPage
            if hasNext { page += 1 }
        case .failure(let message):
            // Пустая папка с ошибкой — экран ошибки с «Повторить»; сбой догрузки — конец листания.
            if items.isEmpty { error = message } else { hasNext = false }
        }
        isLoading = false
    }

    func retry() async {
        error = nil
        hasNext = true
        await loadNext()
    }

    func remove(_ item: Item) async {
        if await succeeded({ try await self.user.removeFromBookmark(itemId: item.id, folderId: self.folderId) }) {
            items.removeAll { $0.id == item.id }
        }
    }
}
