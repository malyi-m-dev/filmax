import SwiftUI
import Shared

/// Все подборки — сетка с постраничной догрузкой (с Главной, по «Все» у ряда «Подборки»).
struct TvCollectionsView: View {
    @StateObject private var viewModel = CollectionsViewModel()

    var body: some View {
        TvScreenScroll {
            TvScreenTitle(title: "Подборки")
            content
        }
        .task { await viewModel.loadInitial() }
    }

    @ViewBuilder private var content: some View {
        if !viewModel.collections.isEmpty {
            TvGrid {
                ForEach(viewModel.collections, id: \.id) { collection in
                    TvPosterCard(collection: collection)
                        .onAppear {
                            guard collection.id == viewModel.collections.last?.id else { return }
                            Task { await viewModel.loadNext() }
                        }
                }
            }
        } else if viewModel.isLoading {
            TvLoadingView()
        } else if let error = viewModel.error {
            TvErrorView(message: error) { Task { await viewModel.loadInitial() } }
        } else {
            TvEmptyStateView(message: "Подборок пока нет", systemImage: "rectangle.stack")
        }
    }
}

/// Содержимое подборки: сетка тайтлов с догрузкой → Детали.
struct TvCollectionDetailView: View {
    @StateObject private var viewModel: CollectionDetailViewModel
    let title: String

    init(collectionId: Int32, title: String) {
        _viewModel = StateObject(wrappedValue: CollectionDetailViewModel(collectionId: collectionId))
        self.title = title
    }

    var body: some View {
        TvScreenScroll {
            TvScreenTitle(title: title)
            content
        }
        .task { await viewModel.loadInitial() }
    }

    @ViewBuilder private var content: some View {
        // Модель склеивает страницы как есть, а дубль id в ленивой сетке путает ячейки и фокус.
        let items = viewModel.items.uniqueByID()
        if !items.isEmpty {
            TvGrid {
                ForEach(items, id: \.id) { item in
                    TvPosterCard(item: item)
                        .onAppear {
                            guard item.id == items.last?.id else { return }
                            Task { await viewModel.loadNext() }
                        }
                }
            }
        } else if viewModel.isLoading {
            TvLoadingView()
        } else if let error = viewModel.error {
            TvErrorView(message: error) { Task { await viewModel.loadInitial() } }
        } else {
            TvEmptyStateView(message: "В подборке пока пусто", systemImage: "rectangle.stack")
        }
    }
}
