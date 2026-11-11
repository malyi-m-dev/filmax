import Combine
import Shared

/// Каталог (iOS и tvOS): чипы типа и жанра, сортировка и фильтры по возможностям источника, сетка
/// с подгрузкой страниц. Страницы по нескольким типам и их склейку ведёт общий `CatalogPager`,
/// досортировку — `ArrangeCatalog`, сборку запроса — `CatalogKit`: всё как на Android.
@MainActor
final class CatalogViewModel: ObservableObject {
    /// Чипы типа: nil — «Все». Аниме — жанр поверх фильмов и сериалов.
    let typeChips: [ItemType?] = [nil, .movie, .series, .anime, .documentary, .tvShow, .concert]

    @Published var type: ItemType?
    @Published var genres: [Genre] = []
    @Published var genreId: Int32?
    @Published var countries: [Country] = []
    @Published var sort = SortOption(field: .views, ascending: false)
    @Published var filters: CatalogFilters = CatalogKit.shared.noFilters
    @Published var items: [Item] = []
    @Published var count: CatalogCount?
    @Published var isLoading = false
    @Published var loadingMore = false
    @Published var endReached = false
    @Published var error: String?

    let capabilities: SourceCapabilities

    private let catalog = RepositoryProvider.shared.catalog
    private let pager = CatalogPager(catalog: RepositoryProvider.shared.catalog)

    init() {
        capabilities = CatalogKit.shared.capabilities(source: RepositoryProvider.shared.sources.source.value)
    }

    var activeFilters: Int { Int(filters.activeCount) }

    func start() async {
        guard items.isEmpty else { return }
        async let dictionaries: Void = loadDictionaries()
        await reload()
        await dictionaries
    }

    func select(type: ItemType?) async { self.type = type; await reload() }
    func select(genreId: Int32?) async { self.genreId = genreId; await reload() }
    func select(sort: SortOption) async { self.sort = sort; await reload() }
    func apply(filters: CatalogFilters) async { self.filters = filters; await reload() }

    func reload() async {
        let query = CatalogKit.shared.query(type: type, genreId: genreId.kotlin, filters: filters, sort: sort)
        isLoading = true
        error = nil
        guard let page = try? await pager.first(query: query) else {
            isLoading = false
            return
        }
        switch onEnum(of: page) {
        case .loaded(let loaded):
            items = ArrangeCatalog.shared.firstPage(items: loaded.items, sort: sort)
            endReached = pager.endReached
            count = pager.count(shown: Int32(items.count))
        case .failed(let failed):
            items = []
            count = nil
            error = failed.message ?? "Не удалось загрузить каталог."
        case .unchanged, .outdated:
            break
        }
        isLoading = false
    }

    /// Следующая страница при подходе к хвосту сетки; во время загрузки и после конца — пусто.
    func loadMore() async {
        guard !isLoading, !loadingMore, !endReached else { return }
        loadingMore = true
        if let page = try? await pager.next(), case .loaded(let loaded) = onEnum(of: page) {
            items = ArrangeCatalog.shared.nextPage(shown: items, page: loaded.items, sort: sort)
            endReached = pager.endReached
            count = pager.count(shown: Int32(items.count))
        }
        loadingMore = false
    }

    /// «Около 62 результатов» — пока выдача не догружена, число — оценка.
    var countText: String? {
        guard let count else { return nil }
        return count.exact ? "\(count.value) тайтлов" : "около \(count.value)"
    }

    private func loadDictionaries() async {
        let loadedGenres: Loaded<[Genre]> = await request(fallback: "") { try await self.catalog.getGenres() }
        genres = (loadedGenres.value ?? []).filter { $0.video }
        guard capabilities.countries else { return }
        let loadedCountries: Loaded<[Country]> = await request(fallback: "") { try await self.catalog.getCountries() }
        countries = loadedCountries.value ?? []
    }
}
