import SwiftUI
import Shared

/// Список подборок для Apple TV: focus-сетка с постраничной догрузкой.
struct TvCollectionsView: View {
    @StateObject private var viewModel = CollectionsViewModel()

    private let columns = [GridItem(.adaptive(minimum: 260), spacing: DS.Spacing.xl)]

    var body: some View {
        Group {
            if viewModel.collections.isEmpty {
                if viewModel.isLoading {
                    TvLoadingView()
                } else if let error = viewModel.error {
                    TvErrorView(message: error) { Task { await viewModel.loadNext() } }
                } else {
                    TvEmptyStateView(message: "Подборок пока нет")
                }
            } else {
                grid
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .task { await viewModel.loadInitial() }
    }

    private var grid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: DS.Spacing.xxl) {
                ForEach(viewModel.collections, id: \.id) { collection in
                    TvPosterCard(
                        route: .collection(id: collection.id, title: collection.title),
                        posterURL: collection.posterURL,
                        title: collection.title
                    )
                    .onAppear {
                        if collection.id == viewModel.collections.last?.id {
                            Task { await viewModel.loadNext() }
                        }
                    }
                }
            }
            .padding(DS.tvSafeHorizontal)
        }
    }
}

/// Содержимое подборки для Apple TV: focus-сетка элементов с догрузкой → Детали.
struct TvCollectionDetailView: View {
    @StateObject private var viewModel: CollectionDetailViewModel
    let title: String

    private let columns = [GridItem(.adaptive(minimum: 240), spacing: DS.Spacing.xl)]

    init(collectionId: Int32, title: String) {
        _viewModel = StateObject(wrappedValue: CollectionDetailViewModel(collectionId: collectionId))
        self.title = title
    }

    var body: some View {
        Group {
            if viewModel.items.isEmpty {
                if viewModel.isLoading {
                    TvLoadingView()
                } else if let error = viewModel.error {
                    TvErrorView(message: error) { Task { await viewModel.loadNext() } }
                } else {
                    TvEmptyStateView(message: "В подборке нет элементов")
                }
            } else {
                grid
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .task { await viewModel.loadInitial() }
    }

    private var grid: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.lg) {
                TvSectionHeader(title: title)
                LazyVGrid(columns: columns, spacing: DS.Spacing.xxl) {
                    ForEach(viewModel.items, id: \.id) { item in
                        TvPosterCard(
                            route: .details(itemId: item.id),
                            posterURL: item.posterURL,
                            title: item.title,
                            subtitle: item.metaLine,
                            ratingText: item.ratingText
                        )
                        .onAppear {
                            if item.id == viewModel.items.last?.id {
                                Task { await viewModel.loadNext() }
                            }
                        }
                    }
                }
            }
            .padding(DS.tvSafeHorizontal)
            .padding(.vertical, DS.tvSafeVertical)
        }
    }
}
