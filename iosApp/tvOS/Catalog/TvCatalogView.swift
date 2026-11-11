import SwiftUI
import Shared

/// Каталог Apple TV — как на Android TV: чипы типа и жанра, сортировка и фильтры по возможностям
/// источника, сетка с подгрузкой. Поиск живёт здесь же (`.searchable`): набран запрос — вместо
/// витрины выдача поиска; стёрт — витрина с прежними фильтрами.
struct TvCatalogView: View {
    @StateObject private var catalog = CatalogViewModel()
    @StateObject private var search = SearchViewModel()

    var body: some View {
        Group {
            if search.query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                TvCatalogBrowser(viewModel: catalog)
            } else {
                TvSearchResultsView(viewModel: search)
            }
        }
        .searchable(text: $search.query, prompt: "Фильмы и сериалы")
        .task { await catalog.start() }
    }
}

/// Витрина: ряды выбора сверху, сетка под ними. Ряды не прячутся на время загрузки — иначе фокус
/// слетал бы с только что нажатого чипа.
private struct TvCatalogBrowser: View {
    @ObservedObject var viewModel: CatalogViewModel
    @State private var sortShown = false
    @State private var filtersShown = false

    /// За сколько карточек до конца просить следующую страницу: две строки сетки.
    private static let prefetchDistance = 12

    init(viewModel: CatalogViewModel) {
        self.viewModel = viewModel
    }

    var body: some View {
        TvScreenScroll {
            typeChips
            // У «Аниме» жанр задан самим разделом (`CatalogKit.query`), выбор жанра он бы перекрыл.
            if !viewModel.genres.isEmpty && viewModel.type != .anime {
                genreChips
            }
            toolbar
            results
        }
        .confirmationDialog("Сортировка", isPresented: $sortShown, titleVisibility: .visible) {
            sortButtons
        }
        .sheet(isPresented: $filtersShown) {
            TvCatalogFiltersView(
                filters: viewModel.filters,
                capabilities: viewModel.capabilities,
                countries: viewModel.countries
            ) { filters in
                Task { await viewModel.apply(filters: filters) }
            }
        }
    }

    private var typeChips: some View {
        TvChipRow(
            options: viewModel.typeChips.map { TvChipOption<ItemType?>(value: $0, title: $0?.label ?? "Все") },
            selected: viewModel.type
        ) { type in
            Task { await viewModel.select(type: type) }
        }
    }

    private var genreChips: some View {
        TvChipRow(options: genreOptions, selected: viewModel.genreId) { genreId in
            Task { await viewModel.select(genreId: genreId) }
        }
    }

    private var genreOptions: [TvChipOption<Int32?>] {
        [TvChipOption<Int32?>(value: nil, title: "Все жанры")]
            + viewModel.genres.map { TvChipOption<Int32?>(value: $0.id, title: $0.title) }
    }

    private var toolbar: some View {
        HStack(spacing: DS.Spacing.lg) {
            Button { sortShown = true } label: {
                Label(viewModel.sort.field.label, systemImage: "arrow.up.arrow.down")
            }
            .tvActionStyle(.secondary)
            Button { filtersShown = true } label: {
                Label(filtersTitle, systemImage: "line.3.horizontal.decrease.circle")
            }
            .tvActionStyle(.secondary)
            if viewModel.isLoading || viewModel.loadingMore {
                ProgressView().tint(Theme.accent)
            }
            if let countText = viewModel.countText {
                Text(countText)
                    .font(TvType.caption)
                    .foregroundColor(Theme.onSurfaceVariant)
            }
        }
        .padding(.horizontal, DS.tvSafeHorizontal)
        .focusSection()
    }

    private var filtersTitle: String {
        viewModel.activeFilters > 0 ? "Фильтры · \(viewModel.activeFilters)" : "Фильтры"
    }

    /// Меню на tvOS 16 нет (`Menu` — с tvOS 17), поэтому сортировка — системный диалог выбора.
    @ViewBuilder private var sortButtons: some View {
        ForEach(viewModel.capabilities.sorts, id: \.self) { field in
            Button(field == viewModel.sort.field ? "✓ \(field.label)" : field.label) {
                Task { await viewModel.select(sort: SortOption(field: field, ascending: false)) }
            }
        }
        Button("Отмена", role: .cancel) {}
    }

    @ViewBuilder private var results: some View {
        if !viewModel.items.isEmpty {
            TvGrid {
                ForEach(viewModel.items, id: \.id) { item in
                    TvPosterCard(item: item)
                        .onAppear { loadMoreIfNeeded(after: item) }
                }
            }
        } else if let error = viewModel.error {
            TvErrorView(message: error) { Task { await viewModel.reload() } }
        } else if viewModel.isLoading || viewModel.count == nil {
            // До первого ответа счётчика нет: пустая выдача ещё не значит «ничего не нашлось».
            TvLoadingView()
        } else {
            TvEmptyStateView(
                message: "Ничего не нашлось — попробуйте другие фильтры",
                systemImage: "line.3.horizontal.decrease.circle"
            )
        }
    }

    private func loadMoreIfNeeded(after item: Item) {
        guard viewModel.items.suffix(Self.prefetchDistance).contains(where: { $0.id == item.id }) else { return }
        Task { await viewModel.loadMore() }
    }
}
