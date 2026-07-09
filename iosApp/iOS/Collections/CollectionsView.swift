import SwiftUI
import Shared

/// Список подборок для iPhone/iPad: сетка с постраничной догрузкой.
struct CollectionsView: View {
    @StateObject private var viewModel = CollectionsViewModel()

    private let columns = [GridItem(.adaptive(minimum: 130), spacing: DS.Spacing.md)]

    var body: some View {
        Group {
            if viewModel.collections.isEmpty {
                if viewModel.isLoading {
                    LoadingView()
                } else if let error = viewModel.error {
                    ErrorView(message: error) { Task { await viewModel.loadNext() } }
                } else {
                    EmptyStateView(message: "Подборок пока нет")
                }
            } else {
                grid
            }
        }
        .navigationTitle("Подборки")
        .background(Theme.background.ignoresSafeArea())
        .task { await viewModel.loadInitial() }
    }

    private var grid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: DS.Spacing.lg) {
                ForEach(viewModel.collections, id: \.id) { collection in
                    NavigationLink(value: AppRoute.collection(id: collection.id, title: collection.title)) {
                        PosterCard(posterURL: collection.posterURL, title: collection.title, width: 130)
                    }
                    .buttonStyle(.plain)
                    .onAppear {
                        if collection.id == viewModel.collections.last?.id {
                            Task { await viewModel.loadNext() }
                        }
                    }
                }
            }
            .padding(DS.Spacing.md)
            if viewModel.isLoading { ProgressView().tint(Theme.accent).padding() }
        }
    }
}

/// Содержимое подборки для iPhone/iPad: сетка элементов с догрузкой → Детали.
struct CollectionDetailView: View {
    @StateObject private var viewModel: CollectionDetailViewModel
    let title: String

    private let columns = [GridItem(.adaptive(minimum: 110), spacing: DS.Spacing.md)]

    init(collectionId: Int32, title: String) {
        _viewModel = StateObject(wrappedValue: CollectionDetailViewModel(collectionId: collectionId))
        self.title = title
    }

    var body: some View {
        Group {
            if viewModel.items.isEmpty {
                if viewModel.isLoading {
                    LoadingView()
                } else if let error = viewModel.error {
                    ErrorView(message: error) { Task { await viewModel.loadNext() } }
                } else {
                    EmptyStateView(message: "В подборке нет элементов")
                }
            } else {
                grid
            }
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .background(Theme.background.ignoresSafeArea())
        .task { await viewModel.loadInitial() }
    }

    private var grid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: DS.Spacing.lg) {
                ForEach(viewModel.items, id: \.id) { item in
                    NavigationLink(value: AppRoute.details(itemId: item.id)) {
                        PosterCard(
                            posterURL: item.posterURL,
                            title: item.title,
                            subtitle: item.metaLine,
                            ratingText: item.ratingText,
                            width: 110
                        )
                    }
                    .buttonStyle(.plain)
                    .onAppear {
                        if item.id == viewModel.items.last?.id {
                            Task { await viewModel.loadNext() }
                        }
                    }
                }
            }
            .padding(DS.Spacing.md)
            if viewModel.isLoading { ProgressView().tint(Theme.accent).padding() }
        }
    }
}
