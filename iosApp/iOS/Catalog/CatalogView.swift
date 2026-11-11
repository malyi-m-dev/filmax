import SwiftUI
import Shared

/// Каталог iPhone/iPad: чипы типа и жанра, меню сортировки и шторка фильтров — всё по
/// возможностям источника, сетка с подгрузкой.
struct CatalogView: View {
    @StateObject private var viewModel = CatalogViewModel()
    @State private var filtersOpen = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.md) {
                typeChips
                if !viewModel.genres.isEmpty, viewModel.type != .anime { genreChips }
                if let caption = viewModel.countText {
                    Text(caption).font(.footnote).foregroundColor(Theme.textSecondary).padding(.horizontal, DS.Spacing.md)
                }
                grid
            }
            .padding(.vertical, DS.Spacing.sm)
        }
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle("Каталог")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                sortMenu
                Button {
                    filtersOpen = true
                } label: {
                    Image(systemName: viewModel.activeFilters > 0
                          ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                }
            }
        }
        .sheet(isPresented: $filtersOpen) {
            CatalogFiltersSheet(viewModel: viewModel)
        }
        .task { await viewModel.start() }
        .refreshable { await viewModel.reload() }
    }

    @ViewBuilder private var grid: some View {
        if viewModel.isLoading && viewModel.items.isEmpty {
            LoadingView().frame(minHeight: 300)
        } else if let error = viewModel.error, viewModel.items.isEmpty {
            ErrorView(message: error) { Task { await viewModel.reload() } }
        } else if viewModel.items.isEmpty {
            EmptyStateView(message: "Ничего не нашлось — смягчите фильтры")
        } else {
            ItemGrid(items: viewModel.items) { Task { await viewModel.loadMore() } }
            if viewModel.loadingMore { ProgressView().frame(maxWidth: .infinity).padding() }
        }
    }

    private var typeChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DS.Spacing.sm) {
                ForEach(Array(viewModel.typeChips.enumerated()), id: \.offset) { _, type in
                    FilterChip(title: type?.label ?? "Все", selected: viewModel.type == type) {
                        Task { await viewModel.select(type: type) }
                    }
                }
            }
            .padding(.horizontal, DS.Spacing.md)
        }
    }

    private var genreChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DS.Spacing.sm) {
                FilterChip(title: "Все жанры", selected: viewModel.genreId == nil) {
                    Task { await viewModel.select(genreId: nil) }
                }
                ForEach(viewModel.genres, id: \.id) { genre in
                    FilterChip(title: genre.title, selected: viewModel.genreId == genre.id) {
                        Task { await viewModel.select(genreId: genre.id) }
                    }
                }
            }
            .padding(.horizontal, DS.Spacing.md)
        }
    }

    private var sortMenu: some View {
        Menu {
            ForEach(viewModel.capabilities.sorts, id: \.self) { field in
                Button {
                    Task { await viewModel.select(sort: SortOption(field: field, ascending: false)) }
                } label: {
                    if viewModel.sort.field == field { Label(field.label, systemImage: "checkmark") } else { Text(field.label) }
                }
            }
            Divider()
            Button {
                Task { await viewModel.select(sort: SortOption(field: viewModel.sort.field, ascending: !viewModel.sort.ascending)) }
            } label: {
                Label(viewModel.sort.ascending ? "По возрастанию" : "По убыванию", systemImage: "arrow.up.arrow.down")
            }
        } label: {
            Image(systemName: "arrow.up.arrow.down.circle")
        }
    }
}

/// Шторка фильтров: правится черновик, в выдачу он уходит по «Применить» — иначе каталог
/// перезапрашивался бы на каждое касание.
struct CatalogFiltersSheet: View {
    @ObservedObject var viewModel: CatalogViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var draft: CatalogFilters = CatalogKit.shared.noFilters

    private let years: [Int32] = Array(stride(from: Int32(2025), through: 1950, by: -5))
    private let thresholds: [Int32] = [5, 6, 7, 8, 9]

    var body: some View {
        NavigationStack {
            Form {
                Section("Год") {
                    Picker("С", selection: yearBinding(from: true)) { yearOptions }
                    Picker("По", selection: yearBinding(from: false)) { yearOptions }
                }
                Section("Оценка") {
                    ForEach(viewModel.capabilities.ratings, id: \.self) { source in
                        Picker(source.title, selection: ratingBinding(source)) {
                            Text("Любая").tag(Int32?.none)
                            ForEach(thresholds, id: \.self) { value in Text("от \(value)").tag(Int32?.some(value)) }
                        }
                    }
                }
                if viewModel.capabilities.countries, !viewModel.countries.isEmpty {
                    Section("Страна") {
                        Picker("Страна", selection: countryBinding) {
                            Text("Любая").tag(Int32?.none)
                            ForEach(viewModel.countries, id: \.id) { country in Text(country.title).tag(Int32?.some(country.id)) }
                        }
                    }
                }
                Section {
                    Toggle("Только 4K", isOn: Binding(
                        get: { draft.only4k },
                        set: { draft = CatalogKit.shared.withOnly4k(filters: draft, only4k: $0) }
                    ))
                    Toggle("Только завершённые сериалы", isOn: Binding(
                        get: { draft.onlyFinished?.boolValue == true },
                        set: { draft = CatalogKit.shared.withFinished(filters: draft, finished: $0 ? KotlinBoolean(bool: true) : nil) }
                    ))
                }
            }
            .navigationTitle("Фильтры")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Сбросить") { draft = CatalogKit.shared.noFilters }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Применить") {
                        Task { await viewModel.apply(filters: draft) }
                        dismiss()
                    }
                }
            }
        }
        .onAppear { draft = viewModel.filters }
    }

    @ViewBuilder private var yearOptions: some View {
        Text("Любой").tag(Int32?.none)
        ForEach(years, id: \.self) { year in Text(String(year)).tag(Int32?.some(year)) }
    }

    private func yearBinding(from: Bool) -> Binding<Int32?> {
        Binding(
            get: { (from ? draft.yearFrom : draft.yearTo)?.int32Value },
            set: { value in
                draft = from
                    ? CatalogKit.shared.withYears(filters: draft, from: value.kotlin, to: draft.yearTo)
                    : CatalogKit.shared.withYears(filters: draft, from: draft.yearFrom, to: value.kotlin)
            }
        )
    }

    private func ratingBinding(_ source: RatingSource) -> Binding<Int32?> {
        Binding(
            get: { CatalogKit.shared.minRating(filters: draft, source: source)?.int32Value },
            set: { value in draft = CatalogKit.shared.withMinRating(filters: draft, source: source, value: value.kotlin) }
        )
    }

    private var countryBinding: Binding<Int32?> {
        Binding(
            get: { draft.countryId?.int32Value },
            set: { value in draft = CatalogKit.shared.withCountry(filters: draft, countryId: value.kotlin) }
        )
    }
}
